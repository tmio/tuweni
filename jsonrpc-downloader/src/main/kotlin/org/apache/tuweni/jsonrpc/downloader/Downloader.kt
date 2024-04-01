// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.jsonrpc.downloader

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.Security
import java.text.DecimalFormat
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.math.round
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger(Downloader::class.java)

/**
 * Application downloading chain data from a JSON-RPC endpoint.
 */
object DownloaderApp {

  @JvmStatic
  fun main(args: Array<String>) {
    runBlocking {
      if (args.contains("--version")) {
        println("Apache Tuweni JSON-RPC downloader ${ApplicationUtils.version}")
        exitProcess(0)
      }
      if (args.contains("--help") || args.contains("-h")) {
        println("USAGE: jsonrpc-downloader <config file>")
        exitProcess(0)
      }
      ApplicationUtils.renderBanner("Loading JSON-RPC downloader")
      Security.addProvider(BouncyCastleProvider())
      val configFile = Paths.get(if (args.isNotEmpty()) args[0] else "config.toml")
      Security.addProvider(BouncyCastleProvider())

      val config = DownloaderConfig(configFile)
      if (config.config.hasErrors()) {
        for (error in config.config.errors()) {
          println(error.message)
        }
        System.exit(1)
      }
      val vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(config.numberOfThreads()))
      val pool = Executors.newFixedThreadPool(
        config.numberOfThreads(),
      ) {
        val thread = Thread("downloader")
        thread.isDaemon = true
        thread
      }
      val downloader = Downloader(vertx, config, pool.asCoroutineDispatcher())
      Runtime.getRuntime().addShutdownHook(
        Thread {
          downloader.shutdown()
          vertx.close()
          pool.shutdown()
        },
      )
      logger.info("Starting download")
      try {
        downloader.loopDownload()
      } catch (e: Exception) {
        logger.error("Fatal error downloading blocks", e)
        exitProcess(1)
      }
      logger.info("Completed download")
      downloader.shutdown()
      vertx.close()
      pool.shutdown()
    }
  }
}

class Downloader(val vertx: Vertx, val config: DownloaderConfig, override val coroutineContext: CoroutineContext) :
  CoroutineScope {

  val jsonRpcClients: List<JSONRPCClient>
  val objectMapper = ObjectMapper()

  var compressedFile = AtomicReference<FileSystem>()

  init {
    jsonRpcClients = MutableList(config.numberOfThreads()) {
      JSONRPCClient(vertx, config.url(), coroutineContext = this.coroutineContext)
    }.toList()
  }

  suspend fun loopDownload() = coroutineScope {
    val state = readInitialState()
    val intervals = createMissingIntervals(state)
    logger.info("Working with intervals $intervals")
    val jobs = mutableListOf<Job>()
    var length = 0
    var completed = 0
    var bytes = 0
    if (config.compressed()) {
      compressedFile.set(
        FileSystems.newFileSystem(
          URI.create("jar:${Paths.get(config.outputPath()).toUri()}"),
          mapOf(Pair("create", "true")),
        ),
      )
    }
    for (interval in intervals) {
      length += interval.last - interval.first
      for (i in interval) {
        val job = launch {
          if (compressedFile.get() == null) {
            return@launch
          }
          try {
            val block = downloadBlock(i)
            bytes += block.length
            if (config.compressed()) {
              writeBlockCompressed(i, block)
            } else {
              writeBlockUncompressed(i, block)
            }
          } catch (e: Exception) {
            logger.error("Error downloading block $i, aborting", e)
          }
          completed++
        }
        jobs.add(job)
      }
    }
    launch(Dispatchers.Unconfined) {
      var total = 0
      while (completed < length) {
        delay(5000)
        val delta = completed - total
        total = completed
        val dec = DecimalFormat("###,###,###,###,###")
        logger.info(
          "Progress ${round(completed * 100.0 * 100 / length) / 100}%, " +
            "$delta blocks downloaded / $total total / ${dec.format(bytes)} bytes transferred",
        )
      }
    }
    jobs.joinAll()
  }

  private suspend fun downloadBlock(blockNumber: Int): String {
    val jsonRpcClient = jsonRpcClients[blockNumber % config.numberOfThreads()]
    val blockJson = jsonRpcClient.getBlockByBlockNumber(blockNumber, true)
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(blockJson)
  }

  private suspend fun writeBlockUncompressed(blockNumber: Int, block: String) {
    val filePath = Paths.get(
      config.outputPath(),
      "block-${blockNumber.toString().padStart(16, '0')}.json",
    )
    coroutineScope {
      vertx.fileSystem().writeFile(filePath.toString(), Buffer.buffer(block)).coAwait()
    }
  }

  private suspend fun writeBlockCompressed(blockNumber: Int, block: String) {
    coroutineScope {
      val newElt = compressedFile.get()?.getPath(
        "block-${blockNumber.toString().padStart(16, '0')}.json",
      ) ?: return@coroutineScope
      Files.newBufferedWriter(newElt, StandardCharsets.UTF_8, StandardOpenOption.CREATE).use { writer ->
        writer.write(block)
      }
    }
  }

  fun createMissingIntervals(state: DownloadState): List<IntRange> {
    val intervals = mutableListOf<IntRange>()
    if (config.start() < state.start) {
      intervals.add(config.start()..min(state.start, config.end()))
    }
    if (state.end < config.end()) {
      intervals.add(max(config.start(), state.end)..config.end())
    }

    return intervals
  }

  private fun readInitialState(): DownloadState {
    // read the initial state
    var initialState = DownloadState(0, 0)
    try {
      val str = if (config.compressed()) {
        FileSystems.newFileSystem(
          URI.create("jar:${Paths.get(config.outputPath()).toUri()}"),
          mapOf(Pair("create", "true")),
        ).use {
          val offsetFile = it.getPath(".offset")
          Files.readString(offsetFile)
        }
      } else {
        Files.readString(Path.of(config.outputPath(), ".offset"))
      }
      initialState = objectMapper.readValue(str, object : TypeReference<DownloadState>() {})
    } catch (e: IOException) {
      // ignored
    }
    return initialState
  }

  private fun writeFinalState() {
    val state = DownloadState(config.start(), config.end())
    val json = objectMapper.writeValueAsString(state)
    if (config.compressed()) {
      FileSystems.newFileSystem(
        URI.create("jar:${Paths.get(config.outputPath()).toUri()}"),
        mapOf(Pair("create", "true")),
      ).use {
        val offsetFile = it.getPath(".offset")
        Files.writeString(offsetFile, json)
      }
    } else {
      Files.writeString(Path.of(config.outputPath(), ".offset"), json)
    }
  }

  fun shutdown() {
    val f = compressedFile.get() ?: return
    compressedFile.set(null)
    f.close()
    writeFinalState()
  }
}

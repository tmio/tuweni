package com.sovereign

import com.sovereign.InitialState.createInitialStateTree
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.concurrent.CountDownLatch

fun main() = runBlocking {
  Security.addProvider(BouncyCastleProvider())
  val gate = CountDownLatch(1)
  val vertx = Vertx.vertx()
  val initialBlock = InitialState.createInitialBlock()
  val transactionProducer = TransactionProducer(vertx, "block producer", 10000, createInitialStateTree(initialBlock))
  val fullNodes = listOf(FullNode(vertx, "full1", 11001, createInitialStateTree(initialBlock)), FullNode(vertx, "full2", 11002, createInitialStateTree(initialBlock)))
  val lightClients = listOf(LightClient(vertx, "light1", 12001, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light2", 12002, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light3", 12003, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light4", 12004, createInitialStateTree(initialBlock)))

  val network = Network(transactionProducer, fullNodes, lightClients)
  network.start()
  Runtime.getRuntime().addShutdownHook(Thread {
    network.stop()
    gate.countDown()
  })
  gate.await()
}


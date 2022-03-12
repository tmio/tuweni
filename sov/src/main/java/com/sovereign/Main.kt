package com.sovereign

import io.vertx.core.Vertx
import org.bouncycastle.jce.provider.BouncyCastleProvider
import sun.misc.Signal
import sun.misc.SignalHandler
import java.security.Security
import java.util.concurrent.CountDownLatch

fun main() {
  Security.addProvider(BouncyCastleProvider())
  val gate = CountDownLatch(1)
  val vertx = Vertx.vertx()
  val blockProducer = BlockProducer(vertx, "block producer", 10000)
  val fullNodes = listOf(FullNode(vertx, "full1", 11001), FullNode(vertx, "full2", 11002))
  val lightClients = listOf(LightClient(vertx, "light1", 12001),
    LightClient(vertx, "light2", 12002),
    LightClient(vertx, "light3", 12003),
    LightClient(vertx, "light4", 12004))

  val network = Network(blockProducer, fullNodes, lightClients)
  network.start()
  Runtime.getRuntime().addShutdownHook(Thread {
    network.stop()
    gate.countDown()
  })
  gate.await()
}


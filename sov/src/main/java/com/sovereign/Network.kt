package com.sovereign

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.plumtree.EphemeralPeerRepository
import org.apache.tuweni.plumtree.vertx.VertxGossipServer
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import java.util.Timer
import java.util.TimerTask

open class Peer(open val vertx: Vertx, open val name: String, open val port: Int) {
  protected var server: VertxGossipServer? = null

  private val mapper = ObjectMapper()

  init {
    mapper.registerModule(EthJsonModule())
  }

  open fun newMessage(message: Message) {
  }

  private fun deserializeAndHandle(messageBody: Bytes, @Suppress("UNUSED_PARAMETER") attributes: String) {
    val message: Message = mapper.readerFor(Message::class.java).readValue(messageBody.toArrayUnsafe())
    newMessage(message)
  }

  open fun start() {
    server = VertxGossipServer(vertx,
      "localhost",
      port,
      Hash::keccak256,
      EphemeralPeerRepository(),
      this::deserializeAndHandle,
      null,
      null,
      1000,
      1000)
    server?.start()
  }

  open fun stop() {
    server?.stop()
  }

  open fun connect(port: Int) {
    server?.connectTo("localhost", port)
  }
}

data class FullNode(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository
) :
  Peer(vertx, name, port) {

  override fun newMessage(message: Message) {
    if (message is TransactionData) {
      println("$name-${message.tx!!.hash}")
    }
  }
}

data class LightClient(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository
) :
  Peer(vertx, name, port) {
  override fun newMessage(message: Message) {
    if (message is TransactionData) {
      println("$name-${message.tx!!.hash}")
    }
  }
}

data class TransactionProducer(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository
) :
  Peer(vertx, name, port) {

  var newBlockSender: Timer? = null
  val mapper = ObjectMapper()

  init {
    mapper.registerModule(EthJsonModule())
  }

  override fun start() {
    super.start()
    newBlockSender = Timer(true)
    newBlockSender?.scheduleAtFixedRate(object : TimerTask() {
      override fun run() {
        val txData = TransactionData(Bytes32.random())
        txData.tx = Transaction(UInt256.ONE,
          Wei.valueOf(2),
          Gas.valueOf(2),
          Address.fromBytes(Bytes.random(20)),
          Wei.valueOf(0L),
          Bytes.random(64),
          SECP256K1.KeyPair.random())
        val message = mapper.writeValueAsBytes(txData)
        server?.gossip("", Bytes.wrap(message))
      }
    }, 2000, 5000)
  }

  override fun stop() {
    newBlockSender?.cancel()
    super.stop()
  }

}

class Network(val transactionProducer: TransactionProducer, val fullNodes: List<FullNode>, val lightClients: List<LightClient>) {

  fun start() {
    // start the block producer:
    transactionProducer.start()
    // start the full nodes
    for (fullNode in fullNodes) {
      fullNode.start()
    }
    // start the light clients
    for (lightClient in lightClients) {
      lightClient.start()
    }
    // connect them all!
    for (fullNode in fullNodes) {
      fullNode.connect(transactionProducer.port)
      for (otherFullNode in fullNodes) {
        if (fullNode == otherFullNode) {
          continue
        }
        fullNode.connect(otherFullNode.port)
      }
    }
    for (lightClient in lightClients) {
      lightClient.connect(transactionProducer.port)
      for (fullNode in fullNodes) {
        lightClient.connect(fullNode.port)
      }
      for (otherLightClient in lightClients) {
        if (lightClient == otherLightClient) {
          continue
        }
        lightClient.connect(otherLightClient.port)
      }
    }
  }

  fun stop() {
    transactionProducer.stop()
    for (fullNode in fullNodes) {
      fullNode.stop()
    }
    for (lightClient in lightClients) {
      lightClient.stop()
    }
  }

}


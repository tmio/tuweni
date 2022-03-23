/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sovereign

import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.blockprocessor.BlockProcessor
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.plumtree.EphemeralPeerRepository
import org.apache.tuweni.plumtree.Peer
import org.apache.tuweni.plumtree.vertx.VertxGossipServer
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

open class Node(open val vertx: Vertx, open val name: String, open val port: Int) {
  var server: VertxGossipServer? = null

  protected val mapper = ObjectMapper()

  init {
    mapper.registerModule(EthJsonModule())
  }

  open fun newMessage(message: Message, peer: Peer) {
  }

  private fun deserializeAndHandle(messageBody: Bytes, @Suppress("UNUSED_PARAMETER") attributes: String, peer: Peer) {
    val message: Message = mapper.readerFor(Message::class.java).readValue(messageBody.toArrayUnsafe())
    newMessage(message, peer)
  }

  open fun start() {
    server = VertxGossipServer(
      vertx,
      "localhost",
      port,
      Hash::keccak256,
      EphemeralPeerRepository(),
      this::deserializeAndHandle,
      null,
      null,
      1000,
      1000
    )
    server?.start()
  }

  open fun stop() {
    server?.stop()
  }

  open fun connect(port: Int) {
    server?.connectTo("localhost", port)
  }
}

data class BlockProducer(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository,
  val initialTransactions: List<Transaction>,
  val ongoingTransactions: () -> Transaction,
  val fullNodes: List<FullNode>,
) :
  Node(vertx, name, port) {

  var newBlockSender: Timer? = null
  fun sendToFullNodes(block: Block) {
    for (fullNode in fullNodes) {
      fullNode.receiveBlock(block)
    }
  }

  override fun start() {
    super.start()

    newBlockSender = Timer(true)
    newBlockSender?.schedule(
      object : TimerTask() {
        override fun run() = runBlocking {
          val genesisBlock = repository.retrieveGenesisBlock()

          val processor = BlockProcessor()
          val protoBlock = processor.execute(genesisBlock, initialTransactions, repository)
          val block = protoBlock.toBlock(listOf(), Address.ZERO, UInt256.ONE, Instant.now(), Bytes.EMPTY, Genesis.emptyHash, UInt64.random())

          protoBlock.stateChanges.applyChanges()
          for (i in 0 until protoBlock.transactionReceipts.size) {
            val tx = protoBlock.body.transactions[i]
            repository.storeTransaction(tx)
            val txReceipt = protoBlock.transactionReceipts[i]
            repository.storeTransactionReceipt(txReceipt, i, tx.hash, block.header.hash)
          }
          repository.storeBlock(block)

          // send to all the nodes
          sendToFullNodes(block)
          // wait a second before propagating the state root
          delay(1000)
          val data = StateRootData()
          data.root = block.header.stateRoot
          val message = mapper.writeValueAsBytes(data)
          server?.gossip("", Bytes.wrap(message))
          Unit
        }
      },
      2000
    )
    newBlockSender?.scheduleAtFixedRate(
      object : TimerTask() {
        override fun run() {
          // TODO send tx later
        }
      },
      2000, 5000
    )
  }

  override fun stop() {
    newBlockSender?.cancel()
    super.stop()
  }
}

class Network(
  val blockProducer: BlockProducer,
  val fullNodes: List<FullNode>,
  val lightClients: List<LightClient>,
) {

  fun start() {
    // start the block producer:
    blockProducer.start()
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
      fullNode.connect(blockProducer.port)
      for (otherFullNode in fullNodes) {
        if (fullNode == otherFullNode) {
          continue
        }
        fullNode.connect(otherFullNode.port)
      }
    }
    for (lightClient in lightClients) {
      lightClient.connect(blockProducer.port)
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
    blockProducer.stop()
    for (fullNode in fullNodes) {
      fullNode.stop()
    }
    for (lightClient in lightClients) {
      lightClient.stop()
    }
  }
}

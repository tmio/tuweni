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
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.evm.EVMExecutionStatusCode
import org.apache.tuweni.evm.EthereumVirtualMachine
import org.apache.tuweni.evm.impl.EvmVmImpl
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.plumtree.EphemeralPeerRepository
import org.apache.tuweni.plumtree.vertx.VertxGossipServer
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.trie.MerkleTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import java.time.Instant
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

data class FullNode(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository,
) :
  Peer(vertx, name, port) {

  var blocks = mutableListOf<Block>()

  override fun newMessage(message: Message) {
    if (message is StateRootData) {
      val last = blocks.last()
      println("$name-${message.root}-${last.header.stateRoot.equals(message.root)}")
    }
  }

  fun receiveBlock(block: Block) {
    blocks.add(block)
  }
}

data class LightClient(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository,
) :
  Peer(vertx, name, port) {
  override fun newMessage(message: Message) {
    if (message is StateRootData) {
      println("$name-${message.root}")
    }
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
  Peer(vertx, name, port) {

  var newBlockSender: Timer? = null
  val mapper = ObjectMapper()
  val vm = EthereumVirtualMachine(repository, EvmVmImpl::create)

  init {
    mapper.registerModule(EthJsonModule())
    runBlocking {
      vm.start()
    }
  }

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
          var index = 0L

          val bloomFilter = LogsBloomFilter()

          val transactionsTrie = MerklePatriciaTrie.storingBytes()
          val receiptsTrie = MerklePatriciaTrie.storingBytes()
          val allReceipts = mutableListOf<TransactionReceipt>()

          var counter = 0L
          var allGasUsed = Gas.ZERO
          for (tx in initialTransactions) {
            val indexKey = RLP.encodeValue(UInt256.valueOf(counter).trimLeadingZeros())
            transactionsTrie.put(indexKey, tx.toBytes())
            if (null == tx.to) {
              val contractAddress = Address.fromBytes(
                Hash.keccak256(
                  RLP.encodeList {
                    it.writeValue(tx.sender!!)
                    it.writeValue(tx.nonce)
                  }
                ).slice(12)
              )
              val state = AccountState(
                UInt256.ONE,
                Wei.valueOf(0),
                org.apache.tuweni.eth.Hash.fromBytes(MerkleTrie.EMPTY_TRIE_ROOT_HASH),
                org.apache.tuweni.eth.Hash.hash(tx.payload)
              )
              repository.storeAccount(contractAddress, state)
              repository.storeCode(tx.payload)
              val receipt = TransactionReceipt(
                1,
                0, // TODO
                LogsBloomFilter(),
                emptyList()
              )
              allReceipts.add(receipt)
              receiptsTrie.put(indexKey, receipt.toBytes())
              counter++
            } else {
              val code = repository.getAccountCode(tx.to!!)
              val result = vm.execute(
                tx.sender!!,
                tx.to!!,
                tx.value,
                code!!,
                tx.payload,
                genesisBlock.header.gasLimit,
                tx.gasPrice,
                Address.ZERO,
                index,
                Instant.now().toEpochMilli(),
                tx.gasLimit.toLong(),
                genesisBlock.header.difficulty
              )
              if (result.statusCode != EVMExecutionStatusCode.SUCCESS) {
                throw Exception("invalid transaction result")
              }
              for (balanceChange in result.changes.getBalanceChanges()) {
                val state = repository.getAccount(balanceChange.key)?.let {
                  AccountState(it.nonce, balanceChange.value, it.storageRoot, it.codeHash)
                } ?: repository.newAccountState()
                repository.storeAccount(balanceChange.key, state)
              }

              for (storageChange in result.changes.getAccountChanges()) {
                for (oneStorageChange in storageChange.value) {
                  repository.storeAccountValue(storageChange.key, oneStorageChange.key, oneStorageChange.value)
                }
              }

              for (accountToDestroy in result.changes.accountsToDestroy()) {
                repository.destroyAccount(accountToDestroy)
              }
              for (log in result.changes.getLogs()) {
                bloomFilter.insertLog(log)
              }

              val txLogsBloomFilter = LogsBloomFilter()
              for (log in result.changes.getLogs()) {
                bloomFilter.insertLog(log)
              }
              val receipt = TransactionReceipt(
                1,
                result.gasManager.gasCost.toLong(),
                txLogsBloomFilter,
                result.changes.getLogs()
              )
              allReceipts.add(receipt)
              receiptsTrie.put(indexKey, receipt.toBytes())
              counter++

              allGasUsed = allGasUsed.add(result.gasManager.gasCost)
            }
            repository.storeTransaction(tx)
          }

          // create a block from initial transactions, and execute them:
          val block = Block(
            BlockHeader(
              genesisBlock.header.hash,
              Genesis.emptyListHash,
              Address.ZERO,
              org.apache.tuweni.eth.Hash.fromBytes(repository.worldState!!.rootHash()),
              org.apache.tuweni.eth.Hash.fromBytes(transactionsTrie.rootHash()),
              org.apache.tuweni.eth.Hash.fromBytes(receiptsTrie.rootHash()),
              bloomFilter.toBytes(),
              genesisBlock.header.difficulty,
              genesisBlock.header.number.add(1),
              genesisBlock.header.gasLimit,
              allGasUsed,
              Instant.now(),
              Bytes.EMPTY,
              org.apache.tuweni.eth.Hash.fromBytes(Bytes32.random()),
              UInt64.random()
            ),
            BlockBody(initialTransactions, listOf())
          )

          for (i in 0..(initialTransactions.size - 1)) {
            val receipt = allReceipts[i]
            val tx = initialTransactions[i]
            repository.storeTransactionReceipt(receipt, 0, tx.hash, block.header.hash)
            repository.storeTransaction(tx)
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

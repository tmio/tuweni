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
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.ByteBuffersDirectory
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.genesis.AllocationGenerator
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.genesis.GenesisConfig
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.trie.MerklePatriciaTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Wei

/**
 * Describes the initial block of the network, with several allocations.
 */
object InitialState {

  val allocations = AllocationGenerator().createAllocations(100, UInt256.valueOf(1_000_000_000))

  fun createGenesis(): Genesis {
    val allocs = allocations.map { it.address to it.amount }.toMap()

    return Genesis(
      nonce = Bytes.ofUnsignedLong(0),
      difficulty = UInt256.ONE.shiftLeft(252),
      coinbase = Address.ZERO,
      timestamp = 0,
      gasLimit = 0,
      parentHash = Bytes32.ZERO,
      alloc = allocs,
      config = GenesisConfig(1234, 0, 0, 0, 0, 0, 0),
      extraData = Bytes.EMPTY,
      mixHash = Bytes32.ZERO,
    )
  }

  fun createInitialBlock(genesis: Genesis = createGenesis()): Block {
    val mapper = ObjectMapper()
    mapper.registerModule(EthJsonModule())
    val bytes = mapper.writeValueAsBytes(genesis)
    println(String(bytes))
    val file = GenesisFile.read(bytes)
    return file.toBlock()
  }

  suspend fun createInitialStateTree(genesisBlock: Block): BlockchainRepository {
    val index = ByteBuffersDirectory()
    val analyzer = StandardAnalyzer()
    val config = IndexWriterConfig(analyzer)
    val writer = IndexWriter(index, config)
    val stateStore = MapKeyValueStore<Bytes, Bytes>()
    val repository = BlockchainRepository.init(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      stateStore,
      BlockchainIndex(writer),
      genesisBlock
    )

    for (alloc in allocations) {
      val tree = MerklePatriciaTrie.storingBytes()
      val accountState =
        AccountState(UInt256.ZERO, Wei.valueOf(alloc.amount), Hash.fromBytes(tree.rootHash()), Hash.hash(Bytes.EMPTY))
      repository.storeAccount(alloc.address, accountState)
    }

    repository.storeBlock(genesisBlock)

    return repository
  }
}

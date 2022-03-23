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

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.blockprocessor.BlockProcessor
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.evm.EVMState
import org.apache.tuweni.evm.impl.StepListener
import org.apache.tuweni.plumtree.Peer
import kotlin.math.floor
import kotlin.math.pow

data class FullNode(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository,
) :
  Node(vertx, name, port) {

  val blocks = mutableListOf<Block>()
  val blockStates = mutableMapOf<Bytes32, List<Bytes32>>()

  override fun newMessage(message: Message, peer: Peer) {
    if (message is StateRootData) {
      val last = blocks.last()
      println("$name-${message.root}-${last.header.stateRoot.equals(message.root)}")
    } else if (message is AskForChallenger) {
      server!!.send(peer, "", Bytes.wrap(mapper.writeValueAsBytes(ChallengeAccepted())))
    } else if (message is ChallengeAccepted) {
      val stateRoot = message.root
      val vmHash = executeBisection(stateRoot, 8)
      val bisectResponse = BisectResponse()
      bisectResponse.complete = true

      bisectResponse.vmHash = vmHash
      bisectResponse.numInstructions = 0
      server!!.send(peer, "", Bytes.wrap(mapper.writeValueAsBytes(bisectResponse)))
    } else if (message is BisectRequest) {
      val vmHash = executeBisection(message.root, message.numInstructions)
      val bisectResponse = BisectResponse()
      bisectResponse.complete = true

      bisectResponse.vmHash = vmHash
      bisectResponse.numInstructions = 0
      server!!.send(peer, "", Bytes.wrap(mapper.writeValueAsBytes(bisectResponse)))
    } else {
      println("Unexpected message $message")
    }
  }

  private fun executeBisection(stateRoot: Bytes32?, numBisections: Int): Bytes32? {
    println("Being asked to execute a bisection with stateRoot $stateRoot for $numBisections")
    val states = blockStates[stateRoot] ?: return null
    val index = floor(states.size / 2.0.pow(numBisections.toDouble())).toInt()
    return states[index]
  }

  fun receiveBlock(block: Block) = runBlocking {
    val parentBlock = blocks.lastOrNull() ?: repository.retrieveGenesisBlock()
    blocks.add(block)
    val processor = BlockProcessor()
    val states = mutableListOf<Bytes32>()
    processor.execute(parentBlock,
      block.body.transactions,
      repository,
      object : StepListener {
        override fun handleStep(executionPath: List<Byte>, state: EVMState): Boolean {
          val opCode = executionPath.last()
          if (opCode == 0xa0.toByte() || opCode == 0xa1.toByte() || opCode == 0xa2.toByte()
            || opCode == 0xa3.toByte() || opCode == 0xa4.toByte() || opCode == 0x55.toByte()
          ) {
            states.add(Hash.keccak256(state.toBytes()))
          }
          return false
        }
      })
    blockStates.put(block.header.stateRoot, states)
  }
}

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
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.plumtree.Peer
import java.util.concurrent.atomic.AtomicReference

data class LightClient(
  override val vertx: Vertx,
  override val name: String,
  override val port: Int,
  val repository: BlockchainRepository,
) :
  Node(vertx, name, port) {

  private var challenge: Challenge? = null

  var latestRoot: Bytes32? = null

  override fun newMessage(message: Message, peer: Peer) {
    if (message is StateRootData) {
      println("$name-${message.root}")
      challenge = Challenge(message.root!!, peer, this)
      challenge!!.startChallenge()
    } else if (message is ChallengeAccepted) {
      challenge?.acceptChallenger(peer)
    } else if (message is BisectResponse) {
      challenge?.let {
        it.executeChallenge(peer, message)
      }
    } else {
      println("Unexpected message $message")
    }
  }
}

class Challenge(val stateRoot: Bytes32, val responder: Peer, val lightClient: LightClient) {

  val challengerRef = AtomicReference<Peer>()
  val mapper = ObjectMapper()

  var currentVmHash: Bytes32? = null

  init {
    mapper.registerModule(EthJsonModule())
  }

  fun startChallenge() {
    val msg = AskForChallenger()
    msg.root = stateRoot

    lightClient.server!!.gossip("", Bytes.wrap(mapper.writeValueAsBytes(msg)))
  }

  fun acceptChallenger(peer: Peer) {
    if (challengerRef.compareAndSet(null, peer)) {
      val msg = ChallengeAccepted()
      msg.root = stateRoot

      lightClient.server!!.send(peer, "", Bytes.wrap(mapper.writeValueAsBytes(msg)))
    } else {
      println("Challenger already picked")
    }
  }

  fun executeChallenge(peer: Peer, message: BisectResponse) {
    if (responder == peer) {
      // the peer is responding to the challenge.
      // check what they sent against what the challenger sent.
      if (currentVmHash != message.vmHash) {
        challengeFailed()
        return
      }

      // pass to challenger
      lightClient.server!!.send(challengerRef.get(), "", Bytes.wrap(mapper.writeValueAsBytes(message)))
    } else if (challengerRef.get() == peer) {

      // the challenger wants to pass the challenge to the responder.
      // Store the hash of the partial execution.
      currentVmHash = message.vmHash

      if (message.complete) {
        challengeCompletedSuccessfully()
        return
      }
      // Make a request:
      val request = BisectRequest()
      request.numInstructions = message.numInstructions
      lightClient.server!!.send(responder, "", Bytes.wrap(mapper.writeValueAsBytes(request)))
    }
  }

  private fun challengeFailed() {
    println("The challenge failed!")
  }

  private fun challengeCompletedSuccessfully() {
    lightClient.latestRoot = stateRoot
    println("The challenge succeeded!")
  }
}

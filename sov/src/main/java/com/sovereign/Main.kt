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

import com.sovereign.InitialState.createInitialStateTree
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import java.util.concurrent.CountDownLatch

fun main() = runBlocking {
  Security.addProvider(BouncyCastleProvider())
  val gate = CountDownLatch(1)
  val vertx = Vertx.vertx()
  val initialBlock = InitialState.createInitialBlock()
  val fullNodes = listOf(
    FullNode(vertx, "full1", 11001, createInitialStateTree(initialBlock)),
    FullNode(vertx, "full2", 11002, createInitialStateTree(initialBlock))
  )
  val blockProducer =
    BlockProducer(vertx, "block producer", 10000, createInitialStateTree(initialBlock), listOf(), {
      Transaction(
        UInt256.ONE,
        Wei.valueOf(2),
        Gas.valueOf(2),
        Address.fromBytes(Bytes.random(20)),
        Wei.valueOf(0L),
        Bytes.random(64),
        SECP256K1.KeyPair.random()
      )
    }, fullNodes)
  val lightClients = listOf(
    LightClient(vertx, "light1", 12001, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light2", 12002, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light3", 12003, createInitialStateTree(initialBlock)),
    LightClient(vertx, "light4", 12004, createInitialStateTree(initialBlock))
  )

  val network = Network(blockProducer, fullNodes, lightClients)
  network.start()
  Runtime.getRuntime().addShutdownHook(
    Thread {
      network.stop()
      gate.countDown()
    }
  )
  gate.await()
}

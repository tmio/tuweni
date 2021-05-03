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
package org.apache.tuweni.ethclient

import kotlinx.coroutines.GlobalScope
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.peer.repository.Connection
import org.apache.tuweni.peer.repository.DiscoveryPeer
import org.apache.tuweni.peer.repository.DiscoveryPeerRepository
import org.apache.tuweni.peer.repository.Endpoint
import org.apache.tuweni.peer.repository.EthereumNodeRecord
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.peer.repository.Peer
import org.apache.tuweni.peer.repository.PeerRepository
import org.apache.tuweni.peer.repository.parseEnodeUri
import org.apache.tuweni.rlpx.wire.HelloMessage
import java.net.URI
import java.time.Instant
import java.util.Objects

class DiscoveryPeerRepositoryImpl(private val repository: PeerRepository) :
  DiscoveryPeerRepository {

  override fun addListener(listener: (DiscoveryPeer) -> Unit) {
    TODO("Unsupported")
  }

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): DiscoveryPeer {
    val identity = repository.storeIdentity(host, port, nodeId)
    val peer = repository.storePeer(identity, null, Instant.now(), wireConnection.peerHello)
    return DelegatePeer(repository, peer)
  }

  override suspend fun get(uri: URI): DiscoveryPeer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    return get(endpoint.address, endpoint.udpPort, nodeId)
  }

  override fun getAsync(uri: URI): AsyncResult<DiscoveryPeer> = GlobalScope.asyncResult { get(uri) }

  override fun getAsync(uri: String): AsyncResult<DiscoveryPeer> = GlobalScope.asyncResult { get(uri) }
}

internal class DelegatePeer(
  val repository: PeerRepository,
  val peer: Peer
) : DiscoveryPeer {
  override val nodeId: SECP256K1.PublicKey
    get() = peer.id().publicKey()
  override val endpoint: Endpoint
    get() = Endpoint(peer.id().networkInterface(), peer.id().port())
  override val enr: EthereumNodeRecord?
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.
  override val lastVerified: Long?
    get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.
  override val lastSeen: Long?
    get() = peer.lastContacted()?.toEpochMilli()

  override fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint? = peer.getEndpoint(ifVerifiedOnOrAfter)

  override fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long?) = peer.updateEndpoint(endpoint, time, ifVerifiedBefore)

  override fun verifyEndpoint(endpoint: Endpoint, time: Long) =

  override fun seenAt(time: Long) {
    repository.peerDiscoveredAt(peer, time)
  }

  override fun updateENR(record: EthereumNodeRecord, time: Long) {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun hashCode(): Int = Objects.hashCode(peer)

  override fun equals(other: Any?): Boolean {
    return other is DiscoveryPeer && Objects.equals(other.nodeId, nodeId) && Objects.equals(other.endpoint, endpoint)
  }
}

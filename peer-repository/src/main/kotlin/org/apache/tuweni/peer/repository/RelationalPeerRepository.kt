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
package org.apache.tuweni.peer.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.rlpx.wire.HelloMessage
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.lang.RuntimeException
import java.net.URI
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID
import java.util.stream.Stream
import javax.sql.DataSource
import kotlin.coroutines.CoroutineContext

open class RelationalPeerRepository(
  private val dataSource: DataSource,
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
) : CoroutineScope, EthereumPeerRepository, DiscoveryPeerRepository {

  companion object {
    internal val logger = LoggerFactory.getLogger(RelationalPeerRepository::class.java)
  }

  private val listeners = mutableListOf<(DiscoveryPeer) -> Unit>()
  private val statusListeners = mutableMapOf<String, (EthereumConnection) -> Unit>()
  private val identityListeners = mutableMapOf<String, (Identity) -> Unit>()

  override fun addListener(listener: (DiscoveryPeer) -> Unit) {
    listeners.add(listener)
  }

  override suspend fun get(host: String, port: Int, nodeId: SECP256K1.PublicKey): DiscoveryPeer {
    return get(nodeId, Endpoint(host, port))
  }

  fun get(nodeId: SECP256K1.PublicKey, endpoint: Endpoint): DiscoveryPeer {
    dataSource.connection.use { conn ->
      logger.info("Get peer with $nodeId")
      val stmt = conn.prepareStatement("select id,publickey from identity where publickey=?")
      stmt.setBytes(1, nodeId.bytes().toArrayUnsafe())
      try {
        val rs = stmt.executeQuery()
        logger.info("Results")
        rs.use {
          if (!rs.next()) {
            logger.info("Creating new peer with public key ${nodeId.toHexString()}")
            val id = UUID.randomUUID().toString()
            val insert = conn.prepareStatement("insert into identity(id, publickey) values(?, ?)")
            insert.setString(1, id)
            insert.setBytes(2, nodeId.bytes().toArrayUnsafe())
            insert.execute()
            val newPeer = RepositoryPeer(nodeId, id, endpoint, dataSource)
            listeners.let {
              for (listener in listeners) {
                launch {
                  listener(newPeer)
                }
              }
            }
            return newPeer
          } else {
            logger.info("Found existing peer with public key ${nodeId.toHexString()}")
            val id = rs.getString(1)
            val pubKey = rs.getBytes(2)
            return RepositoryPeer(SECP256K1.PublicKey.fromBytes(Bytes.wrap(pubKey)), id, endpoint, dataSource)
          }
        }
      } catch (e: Exception) {
        logger.error(e.message, e)
        throw RuntimeException(e)
      }
    }
  }

  override suspend fun get(uri: URI): DiscoveryPeer {
    val (nodeId, endpoint) = parseEnodeUri(uri)
    return get(nodeId, endpoint)
  }

  override fun getAsync(uri: URI): AsyncResult<DiscoveryPeer> {
    return asyncResult { get(uri) }
  }

  override fun getAsync(uri: String): AsyncResult<DiscoveryPeer> {
    return asyncResult { get(uri) }
  }

  fun getPeers(infoCollected: Long, from: Int? = null, limit: Int? = null): List<PeerConnectionInfo> {
    dataSource.connection.use { conn ->
      var query = "select distinct nodeinfo.host, nodeinfo.port, nodeinfo.publickey from nodeinfo \n" +
        "  inner join (select id, max(createdAt) as maxCreatedAt from nodeinfo group by id) maxSeen \n" +
        "  on nodeinfo.id = maxSeen.id and nodeinfo.createdAt = maxSeen.maxCreatedAt where createdAt < ?"
      if (from != null && limit != null) {
        query += " limit $limit offset $from"
      }
      val stmt =
        conn.prepareStatement(query)
      stmt.use {
        it.setTimestamp(1, Timestamp(infoCollected))
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<PeerConnectionInfo>()
        while (rs.next()) {
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(3)))
          val port = rs.getInt(2)
          val host = rs.getString(1)
          result.add(PeerConnectionInfo(pubkey, host, port))
        }
        return result
      }
    }
  }

  fun getPeersWithInfo(infoCollected: Long, from: Int? = null, limit: Int? = null): List<PeerConnectionInfoDetails> {
    dataSource.connection.use { conn ->
      var query = "select distinct nodeinfo.host, nodeinfo.port, nodeinfo.publickey, nodeinfo.p2pversion, nodeinfo.clientId, nodeinfo.capabilities, nodeinfo.genesisHash, nodeinfo.besthash, nodeinfo.totalDifficulty from nodeinfo \n" +
        "  inner join (select id, max(createdAt) as maxCreatedAt from nodeinfo group by id) maxSeen \n" +
        "  on nodeinfo.id = maxSeen.id and nodeinfo.createdAt = maxSeen.maxCreatedAt where createdAt < ?"
      if (from != null && limit != null) {
        query += " limit $limit offset $from"
      }
      val stmt =
        conn.prepareStatement(query)
      stmt.use {
        it.setTimestamp(1, Timestamp(infoCollected))
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableListOf<PeerConnectionInfoDetails>()
        while (rs.next()) {
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(3)))
          val port = rs.getInt(2)
          val host = rs.getString(1)
          val p2pVersion = rs.getInt(4)
          val clientId = rs.getString(5)
          val capabilities = rs.getString(6)
          val genesisHash = rs.getString(7)
          val bestHash = rs.getString(8)
          val totalDifficulty = rs.getString(9)
          result.add(PeerConnectionInfoDetails(pubkey, host, port, p2pVersion, clientId, capabilities, genesisHash, bestHash, totalDifficulty))
        }
        return result
      }
    }
  }

  fun getPendingPeers(): Set<PeerConnectionInfo> {
    dataSource.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "select distinct endpoint.host, endpoint.port, identity.publickey from endpoint inner " +
            "join identity on (endpoint.identity = identity.id) where endpoint.identity NOT IN (select identity from nodeinfo)"
        )
      stmt.use {
        // map results.
        val rs = stmt.executeQuery()
        val result = mutableSetOf<PeerConnectionInfo>()
        while (rs.next()) {
          val pubkey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(rs.getBytes(3)))
          val port = rs.getInt(2)
          val host = rs.getString(1)
          result.add(PeerConnectionInfo(pubkey, host, port))
        }
        return result
      }
    }
  }

  override fun storeStatus(wireConnection: WireConnection, status: Status) {
    dataSource.connection.use { conn ->
      val peer = get(wireConnection.peerPublicKey(), Endpoint(wireConnection.peerHost(), wireConnection.peerPort())) as RepositoryPeer
      val stmt =
        conn.prepareStatement(
          "insert into nodeInfo(id, createdAt, host, port, publickey, p2pVersion, clientId, capabilities, genesisHash, bestHash, totalDifficulty, identity) values(?,?,?,?,?,?,?,?,?,?,?,?)"
        )
      stmt.use {
        val peerHello = wireConnection.peerHello
        it.setString(1, UUID.randomUUID().toString())
        it.setTimestamp(2, Timestamp(System.currentTimeMillis()))
        it.setString(3, wireConnection.peerHost())
        it.setInt(4, wireConnection.peerPort())
        it.setBytes(5, wireConnection.peerPublicKey().bytesArray())
        it.setInt(6, peerHello.p2pVersion())
        it.setString(7, peerHello.clientId())
        it.setString(8, peerHello.capabilities().map { it.name() + "/" + it.version() }.joinToString(","))
        it.setString(9, status.genesisHash.toHexString())
        it.setString(10, status.bestHash.toHexString())
        it.setString(11, status.totalDifficulty.toHexString())
        it.setString(12, peer.id)

        it.execute()
      }
    }
  }

  override fun activeConnections(): Stream<EthereumConnection> {
    TODO("Not yet implemented")
  }

  override fun addStatusListener(statusListener: (EthereumConnection) -> Unit): String {
    val key = UUID.randomUUID().toString()
    statusListeners[key] = statusListener
    return key
  }

  override fun removeStatusListener(id: String) {
    statusListeners.remove(id)
  }

  override fun addIdentityListener(identityListener: (Identity) -> Unit): String {
    val key = UUID.randomUUID().toString()
    identityListeners[key] = identityListener
    return key
  }

  override fun removeIdentityListener(id: String) {
    identityListeners.remove(id)
  }

  override suspend fun storePeer(
    id: Identity,
    lastContacted: Instant?,
    lastDiscovered: Instant?,
    peerHello: HelloMessage,
  ): Peer {
    val peer = get(id.networkInterface(), id.port(), id.publicKey())
    lastDiscovered?.let {
      peer.seenAt(it.toEpochMilli())
    }
  }

  override suspend fun randomPeer(): Peer? {
    TODO("Not yet implemented")
  }

  override suspend fun storeIdentity(networkInterface: String, port: Int, publicKey: SECP256K1.PublicKey): Identity {
    return get(networkInterface, port, publicKey) as Identity
  }

  override suspend fun addConnection(peer: Peer, identity: Identity) {
    TODO("Not yet implemented")
  }

  override suspend fun markConnectionInactive(peer: Peer, identity: Identity) {
    TODO("Not yet implemented")
  }

  override suspend fun peerDiscoveredAt(peer: Peer, time: Long) {
    TODO("Not yet implemented")
  }
}

data class PeerConnectionInfo(val nodeId: SECP256K1.PublicKey, val host: String, val port: Int)
data class PeerConnectionInfoDetails(val nodeId: SECP256K1.PublicKey, val host: String, val port: Int, val p2pVersion: Int, val clientId: String, val capabilities: String, val genesisHash: String, val bestHash: String, val totalDifficulty: String)

internal class RepositoryPeer(
  override val nodeId: SECP256K1.PublicKey,
  val id: String,
  knownEndpoint: Endpoint,
  private val dataSource: DataSource,
) : DiscoveryPeer, Identity {

  init {
    dataSource.connection.use {
      val stmt = it.prepareStatement("select lastSeen,lastVerified,host,port from endpoint where identity=?")
      stmt.use {
        it.setString(1, id)
        val rs = it.executeQuery()
        if (rs.next()) {
          val lastSeenStored = rs.getTimestamp(1)
          val lastVerifiedStored = rs.getTimestamp(2)
          val host = rs.getString(3)
          val port = rs.getInt(4)
          if (knownEndpoint.address == host && knownEndpoint.udpPort == port) {
            lastSeen = lastSeenStored.time
            lastVerified = lastVerifiedStored.time
          }
        }
      }
    }
  }

  @Volatile
  override var endpoint: Endpoint = knownEndpoint

  override var enr: EthereumNodeRecord? = null

  @Synchronized
  override fun getEndpoint(ifVerifiedOnOrAfter: Long): Endpoint? {
    if ((lastVerified ?: 0) >= ifVerifiedOnOrAfter) {
      return this.endpoint
    }
    return null
  }

  @Volatile
  override var lastVerified: Long? = null

  @Volatile
  override var lastSeen: Long? = null

  @Synchronized
  override fun updateEndpoint(endpoint: Endpoint, time: Long, ifVerifiedBefore: Long?): Endpoint {
    val currentEndpoint = this.endpoint
    if (currentEndpoint == endpoint) {
      this.seenAt(time)
      return currentEndpoint
    }

    if (ifVerifiedBefore == null || (lastVerified ?: 0) < ifVerifiedBefore) {
      if (currentEndpoint.address != endpoint.address || currentEndpoint.udpPort != endpoint.udpPort) {
        lastVerified = null
      }
      this.endpoint = endpoint
      this.seenAt(time)
      return endpoint
    }

    return currentEndpoint
  }

  @Synchronized
  override fun verifyEndpoint(endpoint: Endpoint, time: Long): Boolean {
    if (endpoint != this.endpoint) {
      return false
    }
    if ((lastVerified ?: 0) < time) {
      lastVerified = time
    }
    seenAt(time)
    return true
  }

  @Synchronized
  override fun seenAt(time: Long) {
    if ((lastSeen ?: 0) < time) {
      lastSeen = time
      persist()
    }
  }

  @Synchronized
  override fun updateENR(record: EthereumNodeRecord, time: Long) {
    if (enr == null || enr!!.seq() < record.seq()) {
      enr = record
      updateEndpoint(Endpoint(record.ip().hostAddress, record.udp()!!, record.tcp()), time)
    }
  }

  fun persist() {
    dataSource.connection.use { conn ->
      val stmt =
        conn.prepareStatement(
          "insert into endpoint(id, lastSeen, lastVerified, host, port, identity) values(?,?,?,?,?,?)"
        )
      stmt.use {
        it.setString(1, UUID.randomUUID().toString())
        it.setTimestamp(2, Timestamp(lastSeen ?: 0))
        it.setTimestamp(3, Timestamp(lastVerified ?: 0))
        it.setString(4, endpoint.address)
        it.setInt(5, endpoint.udpPort)
        it.setString(6, id)
        it.execute()
      }
    }
  }

  override fun networkInterface() = endpoint.address

  override fun port() = endpoint.udpPort

  override fun publicKey() = nodeId

  override fun id() = id

  override fun connections(): List<Connection> {
    TODO("Not yet implemented")
  }

  override fun activePeers(): List<Peer> {
    TODO("Not yet implemented")
  }
}

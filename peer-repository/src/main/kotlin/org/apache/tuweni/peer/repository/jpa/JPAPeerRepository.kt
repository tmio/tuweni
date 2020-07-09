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
package org.apache.tuweni.peer.repository.jpa

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.peer.repository.Connection
import org.apache.tuweni.peer.repository.Identity
import org.apache.tuweni.peer.repository.Peer
import org.apache.tuweni.peer.repository.PeerRepository
import java.time.Instant
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityManagerFactory
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.Table

class JPAPeerRepository(val entityManagerFactory: EntityManagerFactory) : PeerRepository {
  override fun storePeer(id: Identity, lastContacted: Instant?, lastDiscovered: Instant?): Peer {
    val em = entityManagerFactory.createEntityManager()
    em.transaction.begin()
    try {
      val peer =
        JPAPeer(lastContacted = lastContacted, lastDiscovered = lastDiscovered, identity = id, connections = listOf())
      em.persist(peer)
      return peer
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override fun randomPeer(): Peer? {
    val em = entityManagerFactory.createEntityManager()
    em.transaction.begin()
    try {
      return em.find(JPAPeer::class.java, 1)
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override fun storeIdentity(networkInterface: String, port: Int, publicKey: SECP256K1.PublicKey): Identity {
    val em = entityManagerFactory.createEntityManager()
    em.transaction.begin()
    try {
      val identity = JPAIdentity(
        networkInterface = networkInterface,
        port = port,
        publicKey = publicKey.bytesArray(),
        connections = listOf()
      )
      em.persist(identity)
      return identity
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override fun addConnection(peer: Peer, identity: Identity): Connection {
    val em = entityManagerFactory.createEntityManager()
    em.transaction.begin()
    try {
      val conn = JPAConnection(peer = peer, identity = identity, active = true)
      em.persist(conn)
      (peer as JPAPeer).lastContacted = Instant.now()
      em.persist(peer)
      return conn
    } finally {
      em.transaction.commit()
      em.close()
    }
  }

  override fun markConnectionInactive(peer: Peer, identity: Identity) {
    TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
  }

  override fun peerDiscoveredAt(peer: Peer, time: Long) {
    val em = entityManagerFactory.createEntityManager()
    em.transaction.begin()
    try {
      (peer as JPAPeer).lastDiscovered = Instant.ofEpochMilli(time)
      em.persist(peer)
    } finally {
      em.transaction.commit()
      em.close()
    }
  }
}

@Entity
@Table(name = "connection")
open class JPAConnection(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  val id: Int? = null,

  @Column(nullable = false)
  val active: Boolean? = null,

  @ManyToOne(targetEntity = JPAPeer::class)
  val peer: Peer? = null,

  @ManyToOne(targetEntity = JPAIdentity::class)
  val identity: Identity?= null
) : Connection {
  override fun active(): Boolean = active ?: false

  override fun peer(): Peer = peer!!

  override fun identity(): Identity = identity!!
}

@Entity(name = "Identity")
@Table(name = "identity")
open class JPAIdentity(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  val id: Int? = null,
  @Column
  val networkInterface: String? = null,
  @Column
  val port: Int? = null,
  @Column
  val publicKey: ByteArray? = null,
  @OneToMany(targetEntity = JPAConnection::class, mappedBy = "connection")
  val connections: List<Connection>? = null
) : Identity {
  override fun networkInterface() = networkInterface!!

  override fun connections() = connections!!

  override fun port() = port!!

  override fun publicKey(): SECP256K1.PublicKey = SECP256K1.PublicKey.fromBytes(Bytes.wrap(publicKey!!))

  override fun id() = Bytes.wrap(publicKey!!).toHexString() + "@" + networkInterface + ":" + port

  override fun activePeers(): List<Peer> = connections!!.filter { it.active() }.map { it.peer() }
}

@Entity
@Table(name = "peer")
open class JPAPeer(
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  val id: Int? = null,
  @OneToMany(targetEntity = JPAConnection::class)
  val connections: List<Connection>? = null,
  @Column
  var lastContacted: Instant? = null,
  @Column
  var lastDiscovered: Instant? = null,
  @ManyToOne(targetEntity = JPAIdentity::class)
  val identity: Identity? = null
) : Peer {
  override fun connections(): List<Connection> = connections!!

  override fun id(): Identity = identity!!

  override fun lastContacted(): Instant? = lastContacted

  override fun lastDiscovered(): Instant? = lastDiscovered
}

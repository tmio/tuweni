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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.rlp.RLP
import java.net.InetAddress

internal class FindNodeMessage(
  val requestId: Bytes = Message.requestId(),
  val distance: Int = 0
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x03")

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeInt(distance)
    }
  }

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.FINDNODE

  companion object {
    fun create(content: Bytes): FindNodeMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val distance = reader.readInt()
        return@decodeList FindNodeMessage(requestId, distance)
      }
    }
  }
}

internal class NodesMessage(
  val requestId: Bytes = Message.requestId(),
  val total: Int,
  val nodeRecords: List<Bytes>
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x04")

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.NODES

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeInt(total)
      writer.writeList(nodeRecords) { listWriter, it ->
        listWriter.writeValue(it)
      }
    }
  }

  companion object {
    fun create(content: Bytes): NodesMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val total = reader.readInt()
        val nodeRecords = reader.readListContents { listReader ->
          listReader.readValue()
        }
        return@decodeList NodesMessage(requestId, total, nodeRecords)
      }
    }
  }
}

internal class PingMessage(
  val requestId: Bytes = Message.requestId(),
  val enrSeq: Long = 0
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x01")

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.PING

  override fun encode(): Bytes {
    return RLP.encodeList { reader ->
      reader.writeValue(requestId)
      reader.writeLong(enrSeq)
    }
  }

  companion object {
    fun create(content: Bytes): PingMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val enrSeq = reader.readLong()
        return@decodeList PingMessage(requestId, enrSeq)
      }
    }
  }
}

internal class RandomMessage(
  val authTag: Bytes = Message.authTag(),
  val data: Bytes = randomData()
) : Message {

  companion object {
    fun randomData(): Bytes = Bytes.random(Message.RANDOM_DATA_LENGTH)

    fun create(authTag: Bytes, content: Bytes = randomData()): RandomMessage {
      return RandomMessage(authTag, content)
    }
  }

  override fun messageIdentifier(): Bytes {
    throw UnsupportedOperationException("Message type unsupported for random messages")
  }

  override fun type(): MessageType = MessageType.RANDOM

  override fun encode(): Bytes {
    return data
  }
}

internal class TicketMessage(
  val requestId: Bytes = Message.requestId(),
  val ticket: Bytes,
  val waitTime: Long
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x06")

  override fun type(): MessageType = MessageType.TICKET

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(ticket)
      writer.writeLong(waitTime)
    }
  }

  companion object {
    fun create(content: Bytes): TicketMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val ticket = reader.readValue()
        val waitTime = reader.readLong()
        return@decodeList TicketMessage(requestId, ticket, waitTime)
      }
    }
  }
}

internal class WhoAreYouMessage(
  val authTag: Bytes = Message.authTag(),
  val idNonce: Bytes = Message.idNonce(),
  val enrSeq: Long = 0
) : Message {

  companion object {
    fun create(content: Bytes): WhoAreYouMessage {
      return RLP.decodeList(content) { r ->
        val authTag = r.readValue()
        val idNonce = r.readValue()
        val enrSeq = r.readLong()
        return@decodeList WhoAreYouMessage(authTag, idNonce, enrSeq)
      }
    }
  }

  override fun messageIdentifier(): Bytes {
    throw UnsupportedOperationException("Message type unsupported for whoareyou messages")
  }

  override fun type(): MessageType = MessageType.WHOAREYOU

  override fun encode(): Bytes {
    return RLP.encodeList { w ->
      w.writeValue(authTag)
      w.writeValue(idNonce)
      w.writeLong(enrSeq)
    }
  }
}

internal class TopicQueryMessage(
  val requestId: Bytes = Message.requestId(),
  val topic: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x08")

  override fun type(): MessageType = MessageType.TOPICQUERY

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(topic)
    }
  }

  companion object {
    fun create(content: Bytes): TopicQueryMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val topic = reader.readValue()
        return@decodeList TopicQueryMessage(requestId, topic)
      }
    }
  }
}

/**
 * Message to register a topic.
 */
internal class RegTopicMessage(
  val requestId: Bytes = Message.requestId(),
  val nodeRecord: Bytes,
  val topic: Bytes,
  val ticket: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x05")

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.REGTOPIC

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(nodeRecord)
      writer.writeValue(topic)
      writer.writeValue(ticket)
    }
  }

  companion object {
    fun create(content: Bytes): RegTopicMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val nodeRecord = reader.readValue()
        val topic = reader.readValue()
        val ticket = reader.readValue()
        return@decodeList RegTopicMessage(requestId, nodeRecord, topic, ticket)
      }
    }
  }
}

internal class PongMessage(
  val requestId: Bytes = Message.requestId(),
  val enrSeq: Long = 0,
  val recipientIp: InetAddress,
  val recipientPort: Int
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x02")

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.PONG

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeLong(enrSeq)

      val bytesIp = Bytes.wrap(recipientIp.address)
      writer.writeValue(bytesIp)
      writer.writeInt(recipientPort)
    }
  }

  companion object {
    fun create(content: Bytes): PongMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val enrSeq = reader.readLong()
        val address = InetAddress.getByAddress(reader.readValue().toArray())
        val recipientPort = reader.readInt()
        return@decodeList PongMessage(requestId, enrSeq, address, recipientPort)
      }
    }
  }
}

internal class RegConfirmationMessage(
  val requestId: Bytes = Message.requestId(),
  val topic: Bytes
) : Message {

  private val encodedMessageType: Bytes = Bytes.fromHexString("0x07")

  override fun messageIdentifier(): Bytes = encodedMessageType

  override fun type(): MessageType = MessageType.REGCONFIRM

  override fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(requestId)
      writer.writeValue(topic)
    }
  }

  companion object {
    fun create(content: Bytes): RegConfirmationMessage {
      return RLP.decodeList(content) { reader ->
        val requestId = reader.readValue()
        val topic = reader.readValue()
        return@decodeList RegConfirmationMessage(requestId, topic)
      }
    }
  }
}

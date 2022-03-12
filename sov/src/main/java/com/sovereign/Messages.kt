package com.sovereign

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Transaction

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(JsonSubTypes.Type(value = TransactionData::class, name = "TX"))
interface Message

data class TransactionData @JsonCreator constructor(
  @JsonProperty("stateRoot") val stateRoot: Bytes32,
) : Message {

  var tx : Transaction? = null

  @JsonProperty("tx")
  fun tx(data : Bytes) {
    tx = Transaction.fromBytes(data)
  }

  @JsonProperty("tx")
  fun tx() : Bytes {
    return tx!!.toBytes()
  }
}

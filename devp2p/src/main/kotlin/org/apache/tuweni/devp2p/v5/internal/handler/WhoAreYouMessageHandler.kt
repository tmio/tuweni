package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.apache.tuweni.rlp.RLP
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.net.InetSocketAddress

class WhoAreYouMessageHandler(
  private val nodeId: Bytes
): MessageHandler<WhoAreYouMessage> {

  override fun handle(message: WhoAreYouMessage, address: InetSocketAddress, connector: UdpConnector) {
    // Generate ephemeral key pair
    val ephemeralKeyPair = SECP256K1.KeyPair.random()
    val ephemeralKey = ephemeralKeyPair.secretKey()

    // Retrieve enr
    val destRlp = connector.getPendingNodeIdByAddress(address)
    val enr = EthereumNodeRecord.fromRLP(destRlp)
    val destNodeId = Hash.sha2_256(destRlp)

    // Perform agreement
    val secret = SECP256K1.calculateKeyAgreement(ephemeralKey, enr.publicKey())

    // Derive keys
    val hkdf = HKDFBytesGenerator(SHA256Digest())
    val info = Bytes.wrap(INFO_PREFIX, nodeId, destNodeId)
    hkdf.init(HKDFParameters(secret.toArray(), message.idNonce.toArray(), info.toArray()))
    derive(hkdf)
    derive(hkdf)
    val authRespKey = derive(hkdf)

    val signature = sign(connector.getNodeKeyPair(), message)

    val authHeader = generateAuthHeader(signature, message, authRespKey, ephemeralKeyPair.publicKey())
    val findNodeMessage = FindNodeMessage(nodeId, destNodeId, authHeader)
    connector.send(address, findNodeMessage)
  }

  private fun derive(hkdf: HKDFBytesGenerator): ByteArray {
    val result = ByteArray(DERIVED_KEY_SIZE)
    hkdf.generateBytes(result, 0, result.size)
    return result
  }

  private fun sign(keyPair: SECP256K1.KeyPair, message: WhoAreYouMessage): SECP256K1.Signature {
    val signValue = Bytes.wrap(DISCOVERY_ID_NONCE, message.idNonce)
    val hashedSignValue = Hash.sha2_256(signValue)
    return SECP256K1.sign(hashedSignValue, keyPair)
  }

  private fun generateAuthHeader(signature: SECP256K1.Signature, message: WhoAreYouMessage, authRespKey: ByteArray, ephemeralPubKey: SECP256K1.PublicKey): Bytes {
    val plain = RLP.encode { writer ->
      writer.writeInt(VERSION)
      writer.writeValue(signature.bytes())
      // TODO: ENR
    }
    val zeroNonce = ByteArray(UdpMessage.ID_NONCE_LENGTH)
    val authResponse = AES128GCM.encrypt(zeroNonce, authRespKey, plain.toArray(), ByteArray(0))
    return RLP.encodeList { writer ->
      writer.writeValue(message.authTag)
      writer.writeValue(message.idNonce)
      writer.writeValue(AUTH_SCHEME_NAME)
      writer.writeValue(ephemeralPubKey.bytes())
      writer.writeByteArray(authResponse)
    }
  }

  companion object {
    private const val DERIVED_KEY_SIZE: Int = 16
    private const val VERSION: Int = 5

    private val INFO_PREFIX: Bytes = Bytes.wrap("discovery v5 key agreement".toByteArray())
    private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())
    private val AUTH_SCHEME_NAME: Bytes = Bytes.wrap("gcm".toByteArray())
  }

}

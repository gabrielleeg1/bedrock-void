@file:Suppress("UNCHECKED_CAST")

package com.gabrielleeg1.bedrockvoid

import com.gabrielleeg1.bedrockvoid.protocol.PacketDeserializer
import com.gabrielleeg1.bedrockvoid.protocol.PacketDeserializerMap
import com.gabrielleeg1.bedrockvoid.protocol.PacketSerializer
import com.gabrielleeg1.bedrockvoid.protocol.PacketSerializerMap
import com.gabrielleeg1.bedrockvoid.protocol.VarInt
import com.gabrielleeg1.bedrockvoid.protocol.packets.inbound.InboundHandshakePacket
import com.gabrielleeg1.bedrockvoid.protocol.packets.inbound.LoginPacket
import com.gabrielleeg1.bedrockvoid.protocol.packets.inbound.LoginPacket.JwtData
import com.gabrielleeg1.bedrockvoid.protocol.packets.inbound.ViolationWarningPacket
import com.gabrielleeg1.bedrockvoid.protocol.packets.outbound.DisconnectPacket
import com.gabrielleeg1.bedrockvoid.protocol.packets.outbound.OutboundHandshakePacket
import com.gabrielleeg1.bedrockvoid.protocol.packets.outbound.PlayStatusPacket
import com.gabrielleeg1.bedrockvoid.protocol.readVarInt
import com.gabrielleeg1.bedrockvoid.protocol.writeVarInt
import io.netty.buffer.ByteBuf
import kotlinx.serialization.decodeFromString
import java.util.Base64

private fun ByteBuf.readLEString(): String {
  val length = readIntLE()
  val data = ByteArray(length)

  readBytes(data)

  return data.decodeToString()
}

private object DisconnectPacketSerializer : PacketSerializer<DisconnectPacket> {
  override fun ByteBuf.serialize(value: DisconnectPacket) {
    writeBoolean(value.hideDisconnectPacket)
    writeVarInt(VarInt(value.kickMessage.length))
    writeBytes(value.kickMessage.toByteArray())
  }
}

private object LoginPacketDeserializer : PacketDeserializer<LoginPacket> {
  private fun decodeBase64(base64: String): String {
    return Base64.getDecoder().decode(base64.split("\\.".toRegex())[1]).decodeToString()
  }

  override fun ByteBuf.deserialize(): LoginPacket {
    val protocolVersion = readInt()

    val jwt = readSlice(readVarInt().value)

    val chainData = json.decodeFromString<Map<String, List<String>>>(jwt.readLEString()).run {
      val (fst, snd, third) = get("chain") ?: error("'chain' does not exist in 'chainData'")

      JwtData(
        chain = Triple(
          json.decodeFromString(decodeBase64(fst)),
          json.decodeFromString(decodeBase64(snd)),
          json.decodeFromString(decodeBase64(third)),
        ),
      )
    }
    val skinData = jwt.readLEString()

    return LoginPacket(protocolVersion, chainData, skinData)
  }
}

private object ViolationWarningPacketDeserializer : PacketDeserializer<ViolationWarningPacket> {
  override fun ByteBuf.deserialize(): ViolationWarningPacket {
    return ViolationWarningPacket()
  }
}

private object InboundHandshakePacketDeserializer : PacketDeserializer<InboundHandshakePacket> {
  override fun ByteBuf.deserialize(): InboundHandshakePacket {
    return InboundHandshakePacket()
  }
}

private object OutboundHandshakePacketSerializer : PacketSerializer<OutboundHandshakePacket> {
  override fun ByteBuf.serialize(value: OutboundHandshakePacket) {
    writeVarInt(VarInt(value.jwtData.length))
    writeBytes(value.jwtData.toByteArray())
  }
}

private object PlayStatusPacketSerializer : PacketSerializer<PlayStatusPacket> {
  override fun ByteBuf.serialize(value: PlayStatusPacket) {
    writeInt(value.status.ordinal)
  }
}

val serializers = mapOf(
  0x03 to OutboundHandshakePacketSerializer,
  0x02 to PlayStatusPacketSerializer,
  0x05 to DisconnectPacketSerializer,
) as PacketSerializerMap

val deserializers = mapOf(
  0x01 to LoginPacketDeserializer,
  0x04 to InboundHandshakePacketDeserializer,
  0x9C to ViolationWarningPacketDeserializer,
) as PacketDeserializerMap

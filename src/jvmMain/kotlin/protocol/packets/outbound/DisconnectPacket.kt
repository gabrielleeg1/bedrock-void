package com.gabrielleeg1.bedrockvoid.protocol.packets.outbound

import com.gabrielleeg1.bedrockvoid.protocol.Packet
import com.gabrielleeg1.bedrockvoid.protocol.packets.OutboundPacket

@Packet(0x05)
data class DisconnectPacket(
  val hideDisconnectPacket: Boolean = false,
  val kickMessage: String,
) : OutboundPacket

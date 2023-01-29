package codechicken.microblock.handler

import codechicken.lib.packet.PacketCustom.{
  IHandshakeHandler,
  IClientPacketHandler,
  IServerPacketHandler
}
import codechicken.lib.packet.PacketCustom
import net.minecraft.client.Minecraft
import codechicken.microblock.MicroMaterialRegistry
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.{INetHandlerPlayServer, INetHandlerPlayClient}
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.network.NetHandlerPlayServer

class MicroblockPH {
  val registryChannel =
    "ForgeMicroblock" // Must use the 250 system for ID registry as the NetworkMod idMap hasn't been properly initialized from the server yet.
}

object MicroblockCPH extends MicroblockPH with IClientPacketHandler {
  def handlePacket(
      packet: PacketCustom,
      mc: Minecraft,
      netHandler: INetHandlerPlayClient
  ) {
    packet.getType match {
      case 1 => handleMaterialRegistration(packet, netHandler)
    }
  }

  def handleMaterialRegistration(
      packet: PacketCustom,
      netHandler: INetHandlerPlayClient
  ) {
    val missing = MicroMaterialRegistry.readIDMap(packet)
    if (!missing.isEmpty)
      netHandler.handleDisconnect(
        new S40PacketDisconnect(
          new ChatComponentTranslation(
            "microblock.missing",
            missing.mkString(", ")
          )
        )
      )
  }
}

object MicroblockSPH
    extends MicroblockPH
    with IServerPacketHandler
    with IHandshakeHandler {
  def handlePacket(
      packet: PacketCustom,
      sender: EntityPlayerMP,
      netHandler: INetHandlerPlayServer
  ) {}

  def handshakeRecieved(netHandler: NetHandlerPlayServer) {
    val packet = new PacketCustom(registryChannel, 1)
    MicroMaterialRegistry.writeIDMap(packet)
    netHandler.sendPacket(packet.toPacket)
  }
}

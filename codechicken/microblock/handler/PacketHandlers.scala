package codechicken.microblock.handler

import codechicken.core.packet.PacketCustom.IClientPacketHandler
import codechicken.core.packet.PacketCustom.IServerPacketHandler
import codechicken.core.packet.PacketCustom
import net.minecraft.client.multiplayer.NetClientHandler
import net.minecraft.client.Minecraft
import codechicken.microblock.MicroMaterialRegistry
import net.minecraft.network.packet.Packet255KickDisconnect
import net.minecraft.network.NetServerHandler
import net.minecraft.entity.player.EntityPlayerMP

class MicroblockPH
{
    val registryChannel = "ForgeMicroblock"//Must use the 250 system for ID registry as the NetworkMod idMap hasn't been properly initialized from the server yet.
}

object MicroblockCPH extends MicroblockPH with IClientPacketHandler
{
    def handlePacket(packet:PacketCustom, netHandler:NetClientHandler, mc:Minecraft)
    {
        packet.getType match
        {
            case 1 => handleMaterialRegistration(packet, netHandler)
        }
    }
    
    def handleMaterialRegistration(packet:PacketCustom, netHandler:NetClientHandler)
    {
        val missing = MicroMaterialRegistry.readIDMap(packet)
        if(!missing.isEmpty)
            netHandler.handleKickDisconnect(new Packet255KickDisconnect("The following microblocks are not installed on this client: "+missing.mkString(", ")))
    }
}

object MicroblockSPH extends MicroblockPH with IServerPacketHandler
{
    def handlePacket(packet:PacketCustom, netHandler:NetServerHandler, sender:EntityPlayerMP){}
}
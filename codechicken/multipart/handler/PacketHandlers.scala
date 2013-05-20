package codechicken.multipart.handler

import codechicken.core.packet.PacketCustom.ICustomPacketHandler.IClientPacketHandler
import codechicken.core.packet.PacketCustom.ICustomPacketHandler.IServerPacketHandler
import codechicken.core.packet.PacketCustom
import net.minecraft.client.multiplayer.NetClientHandler
import net.minecraft.client.Minecraft
import codechicken.multipart.MultiPartRegistry
import net.minecraft.network.NetServerHandler
import net.minecraft.entity.player.EntityPlayerMP
import codechicken.multipart.TileMultipart
import codechicken.multipart.ControlKeyModifer
import codechicken.multipart.TileMultipartObj
import net.minecraft.network.packet.Packet255KickDisconnect
import net.minecraft.server.MinecraftServer

object MultipartCPH extends IClientPacketHandler
{
    val channel = MultipartMod;
    
    def handlePacket(packet:PacketCustom, netHandler:NetClientHandler, mc:Minecraft)
    {
        packet.getType match
        {
            case 1 => handlePartRegistration(packet, netHandler)
            case 2 => TileMultipartObj.handleDescPacket(mc.theWorld, packet, netHandler)
            case 3 | 4 | 5 => TileMultipartObj.handlePacket(mc.theWorld, packet)
        }
    }
    
    def handlePartRegistration(packet:PacketCustom, netHandler:NetClientHandler)
    {
        val missing = MultiPartRegistry.readIDMap(packet)
        if(!missing.isEmpty)
            netHandler.handleKickDisconnect(new Packet255KickDisconnect("The following multiparts are not installed on this client: "+missing.mkString(", ")))
    }
}

object MultipartSPH extends IServerPacketHandler
{
    val channel = MultipartMod;
    
    def handlePacket(packet:PacketCustom, netHandler:NetServerHandler, sender:EntityPlayerMP)
    {
        packet.getType() match
        {
            case 1 => ControlKeyModifer.map.put(sender, packet.readBoolean)
        }
    }
}

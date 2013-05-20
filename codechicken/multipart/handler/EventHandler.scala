package codechicken.multipart.handler

import net.minecraftforge.event.tileentity.TileEntityLoadEvent
import net.minecraft.nbt.NBTTagCompound
import codechicken.multipart.TileMultipart
import net.minecraftforge.event.ForgeSubscribe
import codechicken.multipart.TileMultipartObj
import cpw.mods.fml.common.network.IConnectionHandler
import net.minecraft.network.packet.NetHandler
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet1Login
import cpw.mods.fml.common.network.Player
import net.minecraft.network.NetLoginHandler
import net.minecraft.server.MinecraftServer
import codechicken.core.packet.PacketCustom
import codechicken.multipart.MultiPartRegistry

object MultipartEventHandler extends IConnectionHandler
{
    @ForgeSubscribe
    def tileEntityLoad(event:TileEntityLoadEvent)
    {
        val id = event.tag.getString("id");
        if(!id.equals("savedMultipart"))
            return
        
        val t = TileMultipartObj.createFromNBT(event.tag)
        if(t != null)
            event.setResult(t)
    }
    
    def connectionReceived(loginHandler:NetLoginHandler, netManager:INetworkManager):String = 
    {
        val packet = new PacketCustom(MultipartSPH.channel, 1)
        MultiPartRegistry.writeIDMap(packet)
        netManager.addToSendQueue(packet.toPacket)
        return null
    }
    
    def clientLoggedIn(netHandler:NetHandler, netManager:INetworkManager, packet:Packet1Login){}
    def playerLoggedIn(player:Player, netHandler:NetHandler, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:String, port:Int, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:MinecraftServer, netManager:INetworkManager){}
    def connectionClosed(netManager:INetworkManager){}
}
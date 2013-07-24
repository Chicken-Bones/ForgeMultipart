package codechicken.multipart.handler

import net.minecraft.nbt.NBTTagCompound
import codechicken.multipart.TileMultipart
import net.minecraftforge.event.ForgeSubscribe
import cpw.mods.fml.common.network.IConnectionHandler
import net.minecraft.network.packet.NetHandler
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet1Login
import cpw.mods.fml.common.network.Player
import net.minecraft.network.NetLoginHandler
import net.minecraft.server.MinecraftServer
import codechicken.lib.packet.PacketCustom
import codechicken.multipart.MultiPartRegistry
import net.minecraftforge.event.world._
import cpw.mods.fml.common.ITickHandler
import java.util.EnumSet
import cpw.mods.fml.common.TickType
import scala.collection.JavaConverters._
import java.util.List
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraftforge.event.EventPriority

object MultipartEventHandler extends IConnectionHandler with ITickHandler
{
    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    def tileEntityLoad(event:ChunkDataEvent.Load)
    {
        MultipartSaveLoad.loadTiles(event.getChunk())
    }
    
    @ForgeSubscribe
    def worldLoad(event:WorldEvent.Load)
    {
        MultipartSPH.onWorldLoad(event.world)
    }
    
    @ForgeSubscribe
    def worldUnLoad(event:WorldEvent.Unload)
    {
        MultipartSPH.onWorldUnload(event.world)
    }
    
    @ForgeSubscribe
    def chunkWatch(event:ChunkWatchEvent.Watch)
    {
        val cc = event.chunk
        MultipartSPH.onChunkWatch(event.player, event.player.worldObj.getChunkFromChunkCoords(cc.chunkXPos, cc.chunkZPos))
    }
    
    def connectionReceived(loginHandler:NetLoginHandler, netManager:INetworkManager):String = 
    {
        val packet = new PacketCustom(MultipartSPH.registryChannel, 1)
        MultiPartRegistry.writeIDMap(packet)
        netManager.addToSendQueue(packet.toPacket)
        return null
    }
    
    def clientLoggedIn(netHandler:NetHandler, netManager:INetworkManager, packet:Packet1Login){}
    def playerLoggedIn(player:Player, netHandler:NetHandler, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:String, port:Int, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:MinecraftServer, netManager:INetworkManager){}
    def connectionClosed(netManager:INetworkManager){}
    
    def ticks = EnumSet.of(TickType.SERVER)
    def getLabel = "Multipart"
    def tickStart(tickType:EnumSet[TickType], data:Object*){}
    def tickEnd(tickType:EnumSet[TickType], data:Object*)
    {
        if(tickType.contains(TickType.SERVER))
        {
            MultipartSPH.onTickEnd(
                MinecraftServer.getServer.getConfigurationManager.playerEntityList
                    .asInstanceOf[List[EntityPlayerMP]].asScala)
        }
    }
}
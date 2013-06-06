package codechicken.multipart.handler

import codechicken.core.packet.PacketCustom.IClientPacketHandler
import codechicken.core.packet.PacketCustom.IServerPacketHandler
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
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import java.util.Map
import net.minecraft.tileentity.TileEntity
import codechicken.multipart.TileMultipart
import net.minecraft.entity.player.EntityPlayer
import scala.collection._
import codechicken.core.vec.BlockCoord
import codechicken.core.data.MCOutputStreamWrapper
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import net.minecraft.world.WorldServer
import net.minecraft.world.ChunkCoordIntPair
import MultipartProxy._

object MultipartCPH extends IClientPacketHandler
{
    val channel = MultipartMod;
    
    def handlePacket(packet:PacketCustom, netHandler:NetClientHandler, mc:Minecraft)
    {
        packet.getType match
        {
            case 1 => handlePartRegistration(packet, netHandler)
            case 2 => handleCompressedTileDesc(packet, mc.theWorld)
            case 3 => handleCompressedTileData(packet, mc.theWorld)
        }
    }
    
    def handlePartRegistration(packet:PacketCustom, netHandler:NetClientHandler)
    {
        val missing = MultiPartRegistry.readIDMap(packet)
        if(!missing.isEmpty)
            netHandler.handleKickDisconnect(new Packet255KickDisconnect("The following multiparts are not installed on this client: "+missing.mkString(", ")))
    }
    
    def handleCompressedTileDesc(packet:PacketCustom, world:World)
    {
        val cc = new ChunkCoordIntPair(packet.readInt, packet.readInt)
        while(packet.more)
            TileMultipartObj.handleDescPacket(world, indexInChunk(cc, packet.readShort), packet)
    }
    
    def handleCompressedTileData(packet:PacketCustom, world:World)
    {
        while(packet.more)
        {
            val pos = packet.readCoord
            var i = packet.readUnsignedByte
            while(i < 255)
            {
                TileMultipartObj.handlePacket(pos, world, i, packet)
                i = packet.readUnsignedByte
            }
        }
    }
}

object MultipartSPH extends IServerPacketHandler
{
    class MCByteStream(bout:ByteArrayOutputStream) extends MCOutputStreamWrapper(new DataOutputStream(bout))
    {
        def getBytes = bout.toByteArray
    }
    
    val channel = MultipartMod
    private val updateMap = mutable.Map[World, mutable.Map[BlockCoord, MCByteStream]]()
    
    def handlePacket(packet:PacketCustom, netHandler:NetServerHandler, sender:EntityPlayerMP)
    {
        packet.getType() match
        {
            case 1 => ControlKeyModifer.map.put(sender, packet.readBoolean)
        }
    }
    
    def onWorldLoad(world:World)
    {
        if(!world.isRemote)
            updateMap.put(world, mutable.Map())
    }
    
    def onWorldUnload(world:World)
    {
        if(!world.isRemote)
            updateMap.remove(world)
    }
    
    def getTileStream(world:World, pos:BlockCoord) =
        updateMap.getOrElse(world, null).getOrElseUpdate(pos, {
            val s = new MCByteStream(new ByteArrayOutputStream)
            s.writeCoord(pos)
            s
        })
    
    def onTickEnd(players:Seq[EntityPlayerMP])
    {
        players.foreach{p =>
            val m = updateMap.getOrElse(p.worldObj, null)
            if(!m.isEmpty)
            {
                val manager = p.worldObj.asInstanceOf[WorldServer].getPlayerManager
                val packet = new PacketCustom(channel, 3).setChunkDataPacket().compressed()
                var send = false
                m.foreach(e => 
                    if(manager.isPlayerWatchingChunk(p, e._1.x>>4, e._1.z>>4))
                    {
                        send = true
                        packet.writeByteArray(e._2.getBytes)
                        packet.writeByte(255)
                    })
                if(send)
                    packet.sendToPlayer(p)
            }
        }
        updateMap.foreach(_._2.clear())
    }
    
    def onChunkWatch(player:EntityPlayer, chunk:Chunk)
    {
        val iterator = chunk.chunkTileEntityMap.asInstanceOf[Map[_, TileEntity]].values.iterator
        val packet = new PacketCustom(channel, 2).setChunkDataPacket().compressed()
            .writeInt(chunk.xPosition).writeInt(chunk.zPosition)
        
        var empty = true
        while(iterator.hasNext)
        {
            val tile = iterator.next
            if(tile.isInstanceOf[TileMultipart])
            {
                packet.writeShort(indexInChunk(new BlockCoord(tile)))
                tile.asInstanceOf[TileMultipart].writeDesc(packet)
                empty = false
            }
        }
        if(!empty)
            packet.sendToPlayer(player)
    }
}

package codechicken.multipart.handler

import codechicken.lib.packet.PacketCustom.{
  IHandshakeHandler,
  IClientPacketHandler,
  IServerPacketHandler
}
import codechicken.lib.packet.PacketCustom
import net.minecraft.client.Minecraft
import codechicken.multipart.MultiPartRegistry
import net.minecraft.entity.player.EntityPlayerMP
import codechicken.multipart.ControlKeyModifer
import net.minecraft.world.World
import net.minecraft.world.chunk.Chunk
import java.util.{Map => JMap}
import java.util.Iterator
import net.minecraft.tileentity.TileEntity
import codechicken.multipart.TileMultipart
import net.minecraft.entity.player.EntityPlayer
import scala.collection.mutable.{Map, Set, HashMap, MultiMap}
import codechicken.lib.vec.BlockCoord
import codechicken.lib.data.MCDataOutputWrapper
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import net.minecraft.world.ChunkCoordIntPair
import MultipartProxy._
import codechicken.multipart.PacketScheduler
import java.util.LinkedList
import scala.collection.JavaConversions._
import net.minecraft.network.play.{INetHandlerPlayServer, INetHandlerPlayClient}
import net.minecraft.network.play.server.S40PacketDisconnect
import net.minecraft.util.{ChatComponentText, ChatComponentTranslation}
import net.minecraft.network.NetHandlerPlayServer

class MultipartPH {
  val channel = MultipartMod
  val registryChannel = "ForgeMultipart"
}

object MultipartCPH extends MultipartPH with IClientPacketHandler {
  def handlePacket(
      packet: PacketCustom,
      mc: Minecraft,
      netHandler: INetHandlerPlayClient
  ) {
    try {
      packet.getType match {
        case 1 => handlePartRegistration(packet, netHandler)
        case 2 => handleCompressedTileDesc(packet, mc.theWorld)
        case 3 => handleCompressedTileData(packet, mc.theWorld)
      }
    } catch {
      case e: RuntimeException
          if e.getMessage != null && e.getMessage.startsWith("DC: ") =>
        netHandler.handleDisconnect(
          new S40PacketDisconnect(
            new ChatComponentText(e.getMessage.substring(4))
          )
        )
    }
  }

  def handlePartRegistration(
      packet: PacketCustom,
      netHandler: INetHandlerPlayClient
  ) {
    val missing = MultiPartRegistry.readIDMap(packet)
    if (!missing.isEmpty)
      netHandler.handleDisconnect(
        new S40PacketDisconnect(
          new ChatComponentTranslation(
            "multipart.missing",
            missing.mkString(", ")
          )
        )
      )
  }

  def handleCompressedTileDesc(packet: PacketCustom, world: World) {
    val cc = new ChunkCoordIntPair(packet.readInt, packet.readInt)
    val num = packet.readUShort
    for (i <- 0 until num)
      TileMultipart.handleDescPacket(
        world,
        indexInChunk(cc, packet.readShort),
        packet
      )
  }

  def handleCompressedTileData(packet: PacketCustom, world: World) {
    var x = packet.readInt
    while (x != Int.MaxValue) {
      val pos = new BlockCoord(x, packet.readInt, packet.readInt)
      var i = packet.readUByte
      while (i < 255) {
        TileMultipart.handlePacket(pos, world, i, packet)
        i = packet.readUByte
      }
      x = packet.readInt
    }
  }
}

object MultipartSPH
    extends MultipartPH
    with IServerPacketHandler
    with IHandshakeHandler {
  class MCByteStream(bout: ByteArrayOutputStream)
      extends MCDataOutputWrapper(new DataOutputStream(bout)) {
    def getBytes = bout.toByteArray
  }

  private val updateMap = Map[World, Map[BlockCoord, MCByteStream]]()

  /** These maps are keyed by entityID so that new player instances with the
    * same entity id don't conflict world references
    */
  private val chunkWatchers = new HashMap[Int, Set[ChunkCoordIntPair]]
    with MultiMap[Int, ChunkCoordIntPair]
  private val newWatchers = Map[Int, LinkedList[ChunkCoordIntPair]]()

  def handlePacket(
      packet: PacketCustom,
      sender: EntityPlayerMP,
      netHandler: INetHandlerPlayServer
  ) {
    packet.getType match {
      case 1 => ControlKeyModifer.map.put(sender, packet.readBoolean)
    }
  }

  def handshakeRecieved(netHandler: NetHandlerPlayServer) {
    val packet = new PacketCustom(registryChannel, 1)
    MultiPartRegistry.writeIDMap(packet)
    netHandler.sendPacket(packet.toPacket)
  }

  def onWorldUnload(world: World) {
    if (!world.isRemote)
      updateMap.remove(world)
  }

  def getTileStream(world: World, pos: BlockCoord) =
    updateMap
      .getOrElseUpdate(
        world, {
          if (world.isRemote)
            throw new IllegalArgumentException(
              "Cannot use MultipartSPH on a client world"
            )
          Map()
        }
      )
      .getOrElseUpdate(
        pos, {
          val s = new MCByteStream(new ByteArrayOutputStream)
          s.writeCoord(pos)
          s
        }
      )

  def onTickEnd(players: Seq[EntityPlayerMP]) {
    PacketScheduler.sendScheduled()

    for (p <- players if chunkWatchers.containsKey(p.getEntityId)) {
      updateMap.get(p.worldObj) match {
        case Some(m) if !m.isEmpty =>
          val chunks = chunkWatchers(p.getEntityId)
          val packet = new PacketCustom(channel, 3).compress()
          var send = false
          for (
            (pos, stream) <- m
            if chunks(new ChunkCoordIntPair(pos.x >> 4, pos.z >> 4))
          ) {
            send = true
            packet.writeByteArray(stream.getBytes)
            packet.writeByte(255) // terminator
          }
          if (send) {
            packet.writeInt(Int.MaxValue) // terminator
            packet.sendToPlayer(p)
          }
        case _ =>
      }
    }
    updateMap.foreach(_._2.clear())
    for (p <- players if newWatchers.containsKey(p.getEntityId)) {
      for (c <- newWatchers(p.getEntityId)) {
        val chunk = p.worldObj.getChunkFromChunkCoords(c.chunkXPos, c.chunkZPos)
        val pkt = getDescPacket(
          chunk,
          chunk.chunkTileEntityMap
            .asInstanceOf[JMap[_, TileEntity]]
            .values
            .iterator
        )
        if (pkt != null) pkt.sendToPlayer(p)
        chunkWatchers.addBinding(p.getEntityId, c)
      }
    }
    newWatchers.clear()
  }

  def onChunkWatch(p: EntityPlayer, c: ChunkCoordIntPair) {
    newWatchers.getOrElseUpdate(p.getEntityId, new LinkedList).add(c)
  }

  def onChunkUnWatch(p: EntityPlayer, c: ChunkCoordIntPair) {
    newWatchers.get(p.getEntityId) match {
      case Some(chunks) => chunks.remove(c)
      case _            =>
    }
    chunkWatchers.removeBinding(p.getEntityId, c)
  }

  def getDescPacket(chunk: Chunk, it: Iterator[TileEntity]): PacketCustom = {
    val s = new MCByteStream(new ByteArrayOutputStream)

    var num = 0
    while (it.hasNext) {
      val tile = it.next
      if (tile.isInstanceOf[TileMultipart]) {
        s.writeShort(indexInChunk(new BlockCoord(tile)))
        tile.asInstanceOf[TileMultipart].writeDesc(s)
        num += 1
      }
    }
    if (num != 0) {
      return new PacketCustom(channel, 2)
        .compress()
        .writeInt(chunk.xPosition)
        .writeInt(chunk.zPosition)
        .writeShort(num)
        .writeByteArray(s.getBytes)
    }
    return null
  }
}

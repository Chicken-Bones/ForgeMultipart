package codechicken.multipart.handler

import codechicken.multipart.{
  TileCache,
  TileMultipart,
  MultiPartRegistry,
  BlockMultipart
}
import cpw.mods.fml.common.eventhandler.{EventPriority, SubscribeEvent}
import net.minecraft.server.MinecraftServer
import codechicken.lib.packet.PacketCustom
import net.minecraftforge.event.world._
import java.util.EnumSet
import scala.collection.JavaConverters._
import java.util.List
import net.minecraft.entity.player.EntityPlayerMP
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import cpw.mods.fml.common.gameevent.TickEvent

object MultipartEventHandler {
  @SubscribeEvent(priority = EventPriority.HIGHEST)
  def tileEntityLoad(event: ChunkDataEvent.Load) {
    MultipartSaveLoad.loadTiles(event.getChunk)
  }

  @SubscribeEvent
  def worldUnLoad(event: WorldEvent.Unload) {
    MultipartSPH.onWorldUnload(event.world)
    if (event.world.isRemote)
      TileCache.clear()
  }

  @SubscribeEvent
  def chunkWatch(event: ChunkWatchEvent.Watch) {
    MultipartSPH.onChunkWatch(event.player, event.chunk)
  }

  @SubscribeEvent
  def chunkUnWatch(event: ChunkWatchEvent.UnWatch) {
    MultipartSPH.onChunkUnWatch(event.player, event.chunk)
  }

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  def drawBlockHighlight(event: DrawBlockHighlightEvent) {
    if (
      event.target != null && event.target.typeOfHit == MovingObjectType.BLOCK &&
      event.player.worldObj
        .getTileEntity(
          event.target.blockX,
          event.target.blockY,
          event.target.blockZ
        )
        .isInstanceOf[TileMultipart]
    ) {
      if (
        BlockMultipart.drawHighlight(
          event.player.worldObj,
          event.player,
          event.target,
          event.partialTicks
        )
      )
        event.setCanceled(true)
    }
  }

  @SubscribeEvent
  def serverTick(event: TickEvent.ServerTickEvent) {
    if (event.phase == TickEvent.Phase.END)
      MultipartSPH.onTickEnd(
        MinecraftServer.getServer.getConfigurationManager.playerEntityList
          .asInstanceOf[List[EntityPlayerMP]]
          .asScala
      )
  }
}

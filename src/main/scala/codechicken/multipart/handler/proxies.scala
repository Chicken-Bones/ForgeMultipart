package codechicken.multipart.handler

import codechicken.multipart._
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.client.registry.ClientRegistry
import net.minecraft.tileentity.TileEntity
import codechicken.lib.config.ConfigFile
import java.io.File
import net.minecraftforge.common.MinecraftForge
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.lib.packet.PacketCustom
import net.minecraft.world.ChunkCoordIntPair
import codechicken.lib.vec.BlockCoord
import codechicken.lib.world.{TileChunkLoadHook, WorldExtensionManager}
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import org.apache.logging.log4j.Logger

class MultipartProxy_serverImpl {
  var block: BlockMultipart = _
  var config: ConfigFile = _
  var logger: Logger = _

  def preInit(cfgdir: File, logger: Logger) {
    this.logger = logger
    config = new ConfigFile(new File(cfgdir, "multipart.cfg"))
      .setComment("Multipart API config file")

    GameRegistry.registerBlock(
      new BlockMultipart().setBlockName("multipart"),
      null,
      "block"
    )
    block = Block.blockRegistry
      .getObject("ForgeMultipart:block")
      .asInstanceOf[BlockMultipart]

    MultipartGenerator.registerTrait(
      "codechicken.multipart.TSlottedPart",
      "codechicken.multipart.scalatraits.TSlottedTile"
    )
    MultipartGenerator.registerTrait(
      "net.minecraftforge.fluids.IFluidHandler",
      "codechicken.multipart.scalatraits.TFluidHandlerTile"
    )
    MultipartGenerator.registerTrait(
      "net.minecraft.inventory.IInventory",
      "codechicken.multipart.scalatraits.JInventoryTile"
    )
    MultipartGenerator.registerTrait(
      "net.minecraft.inventory.ISidedInventory",
      "codechicken.multipart.scalatraits.JInventoryTile"
    )
    MultipartGenerator.registerTrait(
      "codechicken.multipart.JPartialOcclusion",
      "codechicken.multipart.scalatraits.TPartialOcclusionTile"
    )
    MultipartGenerator.registerTrait(
      "codechicken.multipart.IRedstonePart",
      "codechicken.multipart.scalatraits.TRedstoneTile"
    )
    MultipartGenerator.registerTrait(
      "codechicken.multipart.IRandomDisplayTick",
      "codechicken.multipart.scalatraits.TRandomDisplayTickTile",
      null
    )
    MultipartGenerator.registerTrait(
      "codechicken.multipart.INeighborTileChange",
      null,
      "codechicken.multipart.scalatraits.TTileChangeTile"
    )

    MultipartSaveLoad.hookLoader()
  }

  def init() {}

  def postInit() {
    FMLCommonHandler.instance().bus().register(MultipartEventHandler)
    MinecraftForge.EVENT_BUS.register(MultipartEventHandler)
    PacketCustom.assignHandler(MultipartSPH.channel, MultipartSPH)
    PacketCustom.assignHandshakeHandler(
      MultipartSPH.registryChannel,
      MultipartSPH
    )

    WorldExtensionManager.registerWorldExtension(TickScheduler)
    TileChunkLoadHook.init()

    MultipartCompatiblity.load()
  }

  def onTileClassBuilt(t: Class[_ <: TileEntity]) {
    MultipartSaveLoad.registerTileClass(t)
  }
}

class MultipartProxy_clientImpl extends MultipartProxy_serverImpl {
  @SideOnly(Side.CLIENT)
  override def postInit() {
    super.postInit()
    RenderingRegistry.registerBlockHandler(MultipartRenderer)
    PacketCustom.assignHandler(MultipartCPH.channel, MultipartCPH)
    PacketCustom.assignHandler(MultipartCPH.registryChannel, MultipartCPH)

    FMLCommonHandler.instance().bus().register(ControlKeyHandler)
    ClientRegistry.registerKeyBinding(ControlKeyHandler)
  }

  @SideOnly(Side.CLIENT)
  override def onTileClassBuilt(t: Class[_ <: TileEntity]) {
    super.onTileClassBuilt(t)
    ClientRegistry.bindTileEntitySpecialRenderer(t, MultipartRenderer)
  }
}

object MultipartProxy extends MultipartProxy_clientImpl {
  def indexInChunk(cc: ChunkCoordIntPair, i: Int) = new BlockCoord(
    cc.chunkXPos << 4 | i & 0xf,
    (i >> 8) & 0xff,
    cc.chunkZPos << 4 | (i & 0xf0) >> 4
  )
  def indexInChunk(pos: BlockCoord) =
    pos.x & 0xf | pos.y << 8 | (pos.z & 0xf) << 4
}

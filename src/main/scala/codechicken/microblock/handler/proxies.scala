package codechicken.microblock.handler

import net.minecraftforge.common.MinecraftForge
import codechicken.multipart.handler.MultipartProxy._
import codechicken.microblock._
import net.minecraftforge.client.MinecraftForgeClient
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.Item
import net.minecraft.item.crafting.IRecipe

import java.util.{List => JList}
import codechicken.lib.config.ConfigFile
import net.minecraftforge.oredict.OreDictionary
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe
import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.init.{Blocks, Items}
import org.apache.logging.log4j.Logger

import scala.collection.mutable

class MicroblockProxy_serverImpl {
  var logger: Logger = _

  var itemMicro: ItemMicroPart = _
  var sawStone: Item = _
  var sawIron: Item = _
  var sawDiamond: Item = _
  var stoneRod: Item = _

  var useSawIcons: Boolean = _

  def preInit(logger: Logger) {
    this.logger = logger
    itemMicro = new ItemMicroPart
    GameRegistry.registerItem(itemMicro, "microblock")
    sawStone = createSaw(config, "sawStone", 1)
    sawIron = createSaw(config, "sawIron", 2)
    sawDiamond = createSaw(config, "sawDiamond", 3)
    stoneRod = new Item()
      .setUnlocalizedName("microblock:stoneRod")
      .setTextureName("microblock:stoneRod")
    GameRegistry.registerItem(stoneRod, "stoneRod")

    OreDictionary.registerOre("rodStone", stoneRod)
    MinecraftForge.EVENT_BUS.register(MicroblockEventHandler)

    useSawIcons = config
      .getTag("useSawIcons")
      .setComment(
        "Set to true to use mc style icons for the saw instead of the 3D model"
      )
      .getBooleanValue(false)
  }

  protected var saws = mutable.MutableList[Item]()
  def createSaw(config: ConfigFile, name: String, strength: Int) = {
    val saw = new ItemSaw(config.getTag(name).useBraces(), strength)
      .setUnlocalizedName("microblock:" + name)
      .setTextureName("microblock:" + name)
    GameRegistry.registerItem(saw, name)
    saws += saw
    saw
  }

  def addSawRecipe(saw: Item, blade: Item) {
    CraftingManager.getInstance.getRecipeList
      .asInstanceOf[JList[IRecipe]]
      .add(
        new ShapedOreRecipe(
          new ItemStack(saw),
          "srr",
          "sbr",
          's': Character,
          "stickWood",
          'r': Character,
          "rodStone",
          'b': Character,
          blade
        )
      )
  }

  def init() {
    CraftingManager.getInstance.getRecipeList
      .asInstanceOf[JList[IRecipe]]
      .add(MicroRecipe)
    if (!Loader.isModLoaded("dreamcraft")) {
      CraftingManager.getInstance.addRecipe(
        new ItemStack(stoneRod, 4),
        "s",
        "s",
        's': Character,
        Blocks.stone
      )
      addSawRecipe(sawStone, Items.flint)
      addSawRecipe(sawIron, Items.iron_ingot)
      addSawRecipe(sawDiamond, Items.diamond)
    }
  }

  def postInit() {
    MicroMaterialRegistry.calcMaxCuttingStrength()
    PacketCustom.assignHandshakeHandler(
      MicroblockSPH.registryChannel,
      MicroblockSPH
    )
  }
}

class MicroblockProxy_clientImpl extends MicroblockProxy_serverImpl {
  @SideOnly(Side.CLIENT)
  lazy val renderBlocks = new RenderBlocks

  @SideOnly(Side.CLIENT)
  override def postInit() {
    super.postInit()
    MicroMaterialRegistry.loadIcons()
    MinecraftForgeClient.registerItemRenderer(itemMicro, ItemMicroPartRenderer)
    PacketCustom.assignHandler(MicroblockCPH.registryChannel, MicroblockCPH)
  }

  @SideOnly(Side.CLIENT)
  override def init() {
    super.init()
    saws.foreach(MinecraftForgeClient.registerItemRenderer(_, ItemSawRenderer))
  }
}

object MicroblockProxy extends MicroblockProxy_clientImpl {}

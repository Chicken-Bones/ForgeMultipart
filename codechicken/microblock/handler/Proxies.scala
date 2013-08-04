package codechicken.microblock.handler

import net.minecraftforge.common.MinecraftForge
import codechicken.multipart.handler.MultipartProxy._
import codechicken.multipart.TMultiPart
import codechicken.multipart.MultiPartRegistry
import codechicken.microblock.FaceMicroblock
import codechicken.microblock.ItemMicroPart
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import codechicken.microblock.MicroblockClass
import net.minecraftforge.client.MinecraftForgeClient
import codechicken.microblock.ItemMicroPartRenderer
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.item.crafting.CraftingManager
import codechicken.microblock.MicroRecipe
import net.minecraft.item.Item
import codechicken.microblock.ItemSaw
import net.minecraft.item.crafting.IRecipe
import java.util.{List => JList}
import cpw.mods.fml.common.registry.LanguageRegistry
import codechicken.lib.config.ConfigTag
import codechicken.lib.config.ConfigFile
import net.minecraftforge.oredict.OreDictionary
import codechicken.microblock.ItemSawRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraft.block.Block
import codechicken.lib.lang.LangUtil
import net.minecraft.util.ResourceLocation
import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.common.network.NetworkRegistry
import net.minecraft.client.renderer.RenderBlocks

class MicroblockProxy_serverImpl
{
    private var baseID = 24875-1
    
    def nextItemID = {baseID+=1; baseID}
    
    var itemMicro:ItemMicroPart = _
    var sawStone:Item = _
    var sawIron:Item = _
    var sawDiamond:Item = _
    var stoneRod:Item = _
    
    def preInit()
    {
        itemMicro = new ItemMicroPart(config.getTag("itemMicro.id").getIntValue(nextItemID))
        itemMicro.setUnlocalizedName("microblock")
        
        sawStone = createSaw(config, "sawStone", 1)
        sawIron = createSaw(config, "sawIron", 2)
        sawDiamond = createSaw(config, "sawDiamond", 3)
        stoneRod = new Item(config.getTag("stoneRod.id").getIntValue(nextItemID))
            .setUnlocalizedName("microblock:stoneRod").func_111206_d("microblock:stoneRod")
        
        OreDictionary.registerOre("stoneRod", stoneRod)
        MinecraftForge.EVENT_BUS.register(MicroblockEventHandler)
        NetworkRegistry.instance.registerConnectionHandler(MicroblockEventHandler)
    }
    
    def createSaw(config:ConfigFile, name:String, strength:Int) = 
        new ItemSaw(config.getTag(name).useBraces(), strength).setUnlocalizedName("microblock:"+name)
    
    def addSawRecipe(saw:Item, blade:Item)
    {
        CraftingManager.getInstance.getRecipeList.asInstanceOf[JList[IRecipe]].add(
                new ShapedOreRecipe(new ItemStack(saw), 
                "srr",
                "sbr",
                's':Character, Item.stick,
                'r':Character, "stoneRod",
                'b':Character, blade))
    }
    
    def init()
    {
        CraftingManager.getInstance.getRecipeList.asInstanceOf[JList[IRecipe]].add(MicroRecipe)
        CraftingManager.getInstance.addRecipe(new ItemStack(stoneRod, 4), "s", "s", 's':Character, Block.stone)
        addSawRecipe(sawStone, Item.flint)
        addSawRecipe(sawIron, Item.ingotIron)
        addSawRecipe(sawDiamond, Item.diamond)
    }
    
    def postInit()
    {
    }
}

class MicroblockProxy_clientImpl extends MicroblockProxy_serverImpl
{
    val renderBlocks = new RenderBlocks
    
    @SideOnly(Side.CLIENT)
    override def preInit()
    {
        super.preInit()
        LangUtil.loadBaseLangDir(new ResourceLocation("microblock:lang"))
    }
    
    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        super.postInit()
        MinecraftForgeClient.registerItemRenderer(itemMicro.itemID, ItemMicroPartRenderer)
        PacketCustom.assignHandler(MicroblockCPH.registryChannel, 1, 127, MicroblockCPH)
    }
    
    @SideOnly(Side.CLIENT)
    override def createSaw(config:ConfigFile, name:String, strength:Int) = 
    {
        val saw = super.createSaw(config, name, strength)
        MinecraftForgeClient.registerItemRenderer(saw.itemID, ItemSawRenderer)
        saw
    }
}

object MicroblockProxy extends MicroblockProxy_clientImpl
{
}
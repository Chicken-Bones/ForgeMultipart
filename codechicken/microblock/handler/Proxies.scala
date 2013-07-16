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
import codechicken.core.config.ConfigTag
import codechicken.core.config.ConfigFile
import net.minecraftforge.oredict.OreDictionary
import codechicken.microblock.ItemSawRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraft.block.Block

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
        sawStone = createSaw(config, "sawStone", 1, "Stone Handsaw")
        sawIron = createSaw(config, "sawIron", 2, "Iron Handsaw")
        sawDiamond = createSaw(config, "sawDiamond", 3, "Diamond Handsaw")
        stoneRod = new Item(config.getTag("stoneRod.id").getIntValue(nextItemID))
            .setUnlocalizedName("microblock:stoneRod").func_111206_d("microblock:stoneRod")
        LanguageRegistry.addName(stoneRod, "Stone Rod")
        OreDictionary.registerOre("stoneRod", stoneRod)
        
        MinecraftForge.EVENT_BUS.register(MicroblockEventHandler)
    }
    
    def createSaw(config:ConfigFile, name:String, strength:Int, localized:String) = 
    {
        val saw = new ItemSaw(config.getTag(name).useBraces(), strength).setUnlocalizedName("microblock:"+name)
        LanguageRegistry.addName(saw, localized)
        saw
    }
    
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
    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        MinecraftForgeClient.registerItemRenderer(itemMicro.itemID, ItemMicroPartRenderer)
    }
    
    @SideOnly(Side.CLIENT)
    override def createSaw(config:ConfigFile, name:String, strength:Int, localized:String) = 
    {
        val saw = super.createSaw(config, name, strength, localized)
        MinecraftForgeClient.registerItemRenderer(saw.itemID, ItemSawRenderer)
        saw
    }
}

object MicroblockProxy extends MicroblockProxy_clientImpl
{
}
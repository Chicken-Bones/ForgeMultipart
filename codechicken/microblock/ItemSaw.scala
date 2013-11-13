package codechicken.microblock

import net.minecraft.item.Item
import codechicken.lib.config.ConfigTag
import codechicken.microblock.handler.MicroblockProxy._
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import codechicken.lib.render.CCModel
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import codechicken.lib.vec.SwapYZ
import codechicken.lib.render.CCRenderState
import codechicken.lib.vec.Transformation
import codechicken.lib.vec.TransformationList
import codechicken.lib.vec.Translation
import codechicken.lib.vec.Scale
import codechicken.lib.vec.Rotation
import org.lwjgl.opengl.GL11
import codechicken.lib.render.UVTranslation
import ItemRenderType._
import codechicken.lib.math.MathHelper._
import net.minecraft.util.ResourceLocation
import net.minecraft.client.renderer.texture.IconRegister

/**
 * Interface for items that are 'saws'
 */
trait Saw
{
    /**
     * The harvest level this saw is capable of cutting
     */
    def getCuttingStrength:Int
}

class ItemSaw(sawTag:ConfigTag, val harvestLevel:Int) extends Item(sawTag.getTag("id").getIntValue(nextItemID)) with Saw
{
    {
        val maxDamage = sawTag.getTag("durability").getIntValue(1<<harvestLevel+8)
        if(maxDamage > 0)
            setMaxDamage(maxDamage)
        setNoRepair()
    }
    
    override def hasContainerItem = true
        
    override def getContainerItemStack(stack:ItemStack) = 
        if(isDamageable)
            new ItemStack(stack.itemID, 1, stack.getItemDamage+1)
        else
            stack
    
    override def doesContainerItemLeaveCraftingGrid(stack:ItemStack) = false
    
    def getCuttingStrength = harvestLevel
    
    override def registerIcons(register:IconRegister){}
}

object ItemSawRenderer extends IItemRenderer
{
    val models = CCModel.parseObjModels(new ResourceLocation("microblock", "models/saw.obj"), 7, new SwapYZ())
    val handle = models.get("Handle");
    val holder = models.get("BladeSupport")
    val blade = models.get("Blade")
    
    def handleRenderType(item:ItemStack, renderType:ItemRenderType) = true
    
    def shouldUseRenderHelper(renderType:ItemRenderType, item:ItemStack, helper:ItemRendererHelper) = true
    
    def renderItem(renderType:ItemRenderType, item:ItemStack, data:Object*)
    {
        var t = renderType match {
            case INVENTORY => new TransformationList(new Scale(1.8), new Translation(0, 0, -0.6), new Rotation(-pi/4, 1, 0, 0), new Rotation(pi*3/4, 0, 1, 0))
            case ENTITY => new TransformationList(new Scale(1), new Translation(0, 0, -0.25), new Rotation(-pi/4, 1, 0, 0))
            case EQUIPPED_FIRST_PERSON => new TransformationList(new Scale(1.5), new Rotation(-pi/3, 1, 0, 0), new Rotation(pi*3/4, 0, 1, 0), new Translation(0.5, 0.5, 0.5))
            case EQUIPPED => new TransformationList(new Scale(1.5), new Rotation(-pi/5, 1, 0, 0), new Rotation(-pi*3/4, 0, 1, 0), new Translation(0.75, 0.5, 0.75))
            case _ => return
        }
        
        CCRenderState.reset()
        CCRenderState.useNormals(true)
        CCRenderState.pullLightmap()
        CCRenderState.changeTexture("microblock:textures/items/saw.png")
        CCRenderState.setColour(0xFFFFFFFF)
        CCRenderState.startDrawing(7)
        handle.render(t, null)
        holder.render(t, null)
        CCRenderState.draw()
        GL11.glDisable(GL11.GL_CULL_FACE)
        CCRenderState.startDrawing(7)
        blade.render(t, new UVTranslation(0, (item.getItem.asInstanceOf[Saw].getCuttingStrength-1)*4/64D))
        CCRenderState.draw()
        GL11.glEnable(GL11.GL_CULL_FACE)
    }
}
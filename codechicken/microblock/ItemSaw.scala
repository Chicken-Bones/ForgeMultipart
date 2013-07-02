package codechicken.microblock

import net.minecraft.item.Item
import codechicken.core.config.ConfigTag
import codechicken.microblock.handler.MicroblockProxy._
import net.minecraft.item.ItemStack
import net.minecraftforge.client.IItemRenderer
import codechicken.core.render.CCModel
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import codechicken.core.vec.SwapYZ
import codechicken.core.render.CCRenderState
import codechicken.core.vec.Transformation
import codechicken.core.vec.TransformationList
import codechicken.core.vec.Translation
import codechicken.core.vec.Scale
import codechicken.core.vec.Rotation
import org.lwjgl.opengl.GL11
import codechicken.core.render.UVTranslation
import ItemRenderType._
import codechicken.core.alg.MathHelper._

trait Saw
{
    def getCuttingStrength:Int
}

class ItemSaw(sawTag:ConfigTag, val harvestLevel:Int) extends Item(sawTag.getTag("id").getIntValue(nextItemID)) with Saw
{
    {
        val maxDamage = sawTag.getTag("durability").getIntValue(1<<harvestLevel+8)
        if(maxDamage > 0)
            setMaxDamage(maxDamage)
    }
    
    override def hasContainerItem = true
        
    override def getContainerItemStack(stack:ItemStack) = 
        if(isDamageable)
            new ItemStack(stack.itemID, 1, stack.getItemDamage+1)
        else
            stack
    
    override def doesContainerItemLeaveCraftingGrid(stack:ItemStack) = false
    
    def getCuttingStrength = harvestLevel
}

object ItemSawRenderer extends IItemRenderer
{
    val models = CCModel.parseObjModels("/mods/microblock/models/saw.obj", 7, new SwapYZ())
    val handle = models.get("Handle");
    val holder = models.get("BaldeSupport")
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
        CCRenderState.changeTexture("/mods/microblock/textures/items/saw.png")
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
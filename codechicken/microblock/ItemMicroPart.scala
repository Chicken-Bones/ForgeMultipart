package codechicken.microblock

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import ItemMicroPart._
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import net.minecraft.util.StatCollector
import java.util.List
import net.minecraft.creativetab.CreativeTabs
import codechicken.microblock.MicroblockClassRegistry._
import codechicken.core.render.CCRenderState
import net.minecraft.client.renderer.texture.IconRegister
import codechicken.microblock.handler.MicroblockProxy
import net.minecraft.util.MovingObjectPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import codechicken.core.raytracer.RayTracer
import net.minecraft.util.EnumMovingObjectType
import codechicken.core.vec.Vector3
import codechicken.core.render.TextureUtils

class ItemMicroPart(id:Int) extends Item(id)
{
    setHasSubtypes(true)
    
    override def getItemDisplayName(stack:ItemStack):String =
    {
        val material = getMaterial(stack)
        val mcrClass = MicroblockClassRegistry.getMicroClass(stack.getItemDamage)
        val size = stack.getItemDamage&0xFF
        if(material == null || mcrClass == null)
            return "Unnamed"
        
        return material.getLocalizedName+" "+mcrClass.getDisplayName(size)
    }
    
    override def getSubItems(id:Int, tab:CreativeTabs, list$:List[_])
    {
        val list = list$.asInstanceOf[List[ItemStack]]
        for(classId <- 0 until classes.length)
        {
            val mcrClass = classes(classId)
            if(mcrClass != null)
                for(size <- Seq(1, 2, 4))
                    MicroMaterialRegistry.getIdMap.foreach(e => list.add(create(classId<<8|size, e._1)))
        }
    }
    
    override def registerIcons(register:IconRegister){}
    
    override def onItemUse(item:ItemStack, player:EntityPlayer, world:World, x:Int, y:Int, z:Int, s:Int, hitX:Float, hitY:Float, hitZ:Float):Boolean =
    {
        val material = getMaterialID(item)
        val mcrClass = MicroblockClassRegistry.getMicroClass(item.getItemDamage)
        val size = item.getItemDamage&0xFF
        if(material < 0 || mcrClass == null)
            return false
            
        val hit = RayTracer.retraceBlock(world, player, x, y, z)
        if(hit != null && hit.typeOfHit == EnumMovingObjectType.TILE)
        {
            val placement = MicroblockPlacement(world, player, hit, size, material, !player.capabilities.isCreativeMode, mcrClass.placementProperties)
            if(placement == null)
                return false
            
            if(!world.isRemote)
            {
                placement.place(world, player, item)
                if(!player.capabilities.isCreativeMode)
                    placement.consume(world, player, item)
            }
            
            return true
        }
        
        return false
    }
}

object ItemMicroPart
{
    def checkTagCompound(stack:ItemStack)
    {
        if(!stack.hasTagCompound())
            stack.setTagCompound(new NBTTagCompound("tag"))
    }
    
    def create(damage:Int, material:Int):ItemStack = create(damage, MicroMaterialRegistry.materialName(material))
    
    def create(damage:Int, material:String):ItemStack = create(1, damage, material)
    
    def create(amount:Int, damage:Int, material:String):ItemStack = 
    {
        val stack = new ItemStack(MicroblockProxy.itemMicro, amount, damage)
        checkTagCompound(stack)
        stack.getTagCompound().setString("mat", material)
        return stack
    }
    
    def getMaterial(stack:ItemStack):IMicroMaterial =
    {
        checkTagCompound(stack)
        if(!stack.getTagCompound().hasKey("mat"))
            return null;
        
        return MicroMaterialRegistry.getMaterial(stack.getTagCompound().getString("mat"))
    }
    
    def getMaterialID(stack:ItemStack):Int =
    {
        checkTagCompound(stack)
        if(!stack.getTagCompound().hasKey("mat"))
            return -1;
        
        return MicroMaterialRegistry.materialID(stack.getTagCompound().getString("mat"))
    }
}

object ItemMicroPartRenderer extends IItemRenderer
{
    def handleRenderType(item:ItemStack, t:ItemRenderType) = true
    
    def shouldUseRenderHelper(t:ItemRenderType, item:ItemStack, helper:ItemRendererHelper) = true
    
    def renderItem(t:ItemRenderType, item:ItemStack, data:Object*)
    {
        GL11.glPushMatrix()
        if(t == ItemRenderType.ENTITY)
            GL11.glScaled(0.5, 0.5, 0.5)
        if(t == ItemRenderType.INVENTORY || t == ItemRenderType.ENTITY)
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F)
        
        val material = getMaterial(item)
        val mcrClass = MicroblockClassRegistry.getMicroClass(item.getItemDamage)
        val size = item.getItemDamage&0xFF
        if(material == null || mcrClass == null)
            return
        
        CCRenderState.reset()
        TextureUtils.bindAtlas(0);
        CCRenderState.useNormals(true)
        CCRenderState.useModelColours(true)
        CCRenderState.pullLightmap()
        CCRenderState.startDrawing(7)
            val part = mcrClass.create(size, mcrClass.itemSlot, 0, true).asInstanceOf[MicroblockClient]
            part.render(new Vector3(0.5, 0.5, 0.5).subtract(part.getBounds.center), null, material, part.getBounds, 0)
        CCRenderState.draw()
        GL11.glPopMatrix()
    }
    
    def renderHighlight(world:World, player:EntityPlayer, stack:ItemStack, hit:MovingObjectPosition):Boolean =
    {
        val material = getMaterialID(stack)
        val mcrClass = MicroblockClassRegistry.getMicroClass(stack.getItemDamage)
        val size = stack.getItemDamage&0xFF
        if(material < 0 || mcrClass == null)
            return false
            
        return mcrClass.renderHighlight(world, player, hit, size, material)
    }
}
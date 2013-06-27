package codechicken.multipart

import net.minecraft.item.Item
import codechicken.core.vec.Vector3
import codechicken.core.vec.Rotation
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import codechicken.core.vec.BlockCoord

abstract class ItemMultiPart(id:Int) extends Item(id)
{
    def getHitDepth(vhit:Vector3, side:Int):Double = 
        vhit.copy.scalarProject(Rotation.axes(side)) + (side%2^1)
        
    override def onItemUse(item:ItemStack, player:EntityPlayer, world:World, x:Int, y:Int, z:Int, side:Int, hitX:Float, hitY:Float, hitZ:Float):Boolean =
    {
        val pos = new BlockCoord(x, y, z)
        val vhit = new Vector3(hitX, hitY, hitZ)
        val d = getHitDepth(vhit, side)
    
        def place():Boolean =
        {
            val part = newPart(item, player, world, pos, side, vhit)
            if(part == null || !TileMultipart.canPlacePart(world, pos, part))
                return false
            
            if(!world.isRemote)
                TileMultipart.addPart(world, pos, part)
            if(!player.capabilities.isCreativeMode)
                item.stackSize-=1
            return true
        }
        
        if(d < 1 && place())
            return true
        
        pos.offset(side)
        return place()
    }
    
    def newPart(item:ItemStack, player:EntityPlayer, world:World, pos:BlockCoord, side:Int, vhit:Vector3):TMultiPart
}
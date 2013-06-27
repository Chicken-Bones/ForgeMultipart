package codechicken.microblock

import net.minecraft.world.World
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MovingObjectPosition
import codechicken.core.vec.Vector3
import codechicken.core.vec.BlockCoord
import codechicken.multipart.TileMultipart
import codechicken.core.vec.Rotation
import codechicken.multipart.ControlKeyModifer._
import net.minecraft.item.ItemStack
import codechicken.microblock.handler.MicroblockProxy
import codechicken.multipart.TMultiPart
import codechicken.multipart.TileMultipart
import codechicken.core.raytracer.ExtendedMOP

abstract class ExecutablePlacement(val pos:BlockCoord, val part:Microblock)
{
    def place(world:World, player:EntityPlayer, item:ItemStack)
    def consume(world:World, player:EntityPlayer, item:ItemStack)
}

class AdditionPlacement($pos:BlockCoord, $part:Microblock) extends ExecutablePlacement($pos, $part)
{
    def place(world:World, player:EntityPlayer, item:ItemStack)
    {
        TileMultipart.addPart(world, pos, part)
    }
    
    def consume(world:World, player:EntityPlayer, item:ItemStack)
    {
        item.stackSize-=1
    }
}

class ExpandingPlacement($pos:BlockCoord, $part:Microblock, opart:Microblock) extends ExecutablePlacement($pos, $part)
{
    def place(world:World, player:EntityPlayer, item:ItemStack)
    {
        opart.shape = part.shape
        opart.tile.notifyPartChange()
        opart.sendDescUpdate()
    }
    
    def consume(world:World, player:EntityPlayer, item:ItemStack)
    {
        item.stackSize-=1
    }
}

abstract class PlacementProperties
{
    def opposite(slot:Int, side:Int):Int
    
    def sneakOpposite(slot:Int, side:Int) = true
    
    def expand(slot:Int, side:Int) = true
    
    def microClass():MicroblockClass
    
    def placementGrid():PlacementGrid
    
    def customPlacement(pmt:MicroblockPlacement):ExecutablePlacement = null
}

object MicroblockPlacement
{
    def apply(world:World, player:EntityPlayer, hit:MovingObjectPosition, size:Int, material:Int, checkMaterial:Boolean, pp:PlacementProperties):ExecutablePlacement =
        new MicroblockPlacement(world, player, hit, size, material, checkMaterial, pp)()
}

class MicroblockPlacement(val world:World, val player:EntityPlayer, val hit:MovingObjectPosition, val size:Int, val material:Int, val checkMaterial:Boolean, val pp:PlacementProperties)
{
    val mcrClass = pp.microClass
    val pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ)
    val vhit = new Vector3(hit.hitVec).add(-pos.x, -pos.y, -pos.z)
    val gtile = TileMultipart.getOrConvertTile2(world, pos)
    val htile = gtile._1
    val slot = pp.placementGrid.getHitSlot(vhit, hit.sideHit)
    val oslot = pp.opposite(slot, hit.sideHit)
    
    val d = getHitDepth(vhit, hit.sideHit)
    val useOppMod = pp.sneakOpposite(slot, hit.sideHit)
    val oppMod = player.isControlDown
    val internal = d < 1 && htile != null
    val doExpand = internal && !gtile._2 && !player.isSneaking && !(oppMod && useOppMod) && pp.expand(slot, hit.sideHit)
    val side = hit.sideHit
    
    def apply():ExecutablePlacement = 
    {
        val customPlacement = pp.customPlacement(this)
        if(customPlacement != null)
            return customPlacement
        
        if(slot < 0)
            return null
        
        if(doExpand)
        {
            val hpart = htile.partList(ExtendedMOP.getData[(Int, _)](hit)._1)
            if(hpart.getType == mcrClass.getName)
            {
                val mpart = hpart.asInstanceOf[CommonMicroblock]
                if(mpart.material == material && mpart.getSize + size < 8)
                    return expand(mpart)
            }
        }
        
        if(internal)
        {
            if(!useOppMod)
                return internalPlacement(htile, slot)
            if((1-d)*8 < size)
            {
                if(htile.canAddPart(create(1, oslot, material)))
                    return externalPlacement(slot)
            }
            if(d < 0.5)
            {
                var ret = internalPlacement(htile, slot)
                if(ret != null && !oppMod)
                    return ret
                if(ret != null && oppMod || ret == null && !oppMod)
                    return internalPlacement(htile, oslot)
                //sneaking
                if(internalPlacement(htile, oslot) != null)
                    return externalPlacement(slot)
                return null
            }
            var ret = internalPlacement(htile, oslot)
            if(!oppMod)
                return ret
            if(ret != null)
                return externalPlacement(slot)
            return null
        }
        var ret = externalPlacement(slot)
        if(ret != null && oppMod && useOppMod)
            return externalPlacement(oslot)
        return ret
    }
    
    def materialCheck(sizea:Int):Boolean =
    {
        if(!checkMaterial)
            return true
        
        val mUnits = mcrClass.sizeToVolume(sizea) - mcrClass.sizeToVolume(sizea+size)
        var mat = 0
        player.inventory.mainInventory.foreach(item =>
            if(item != null && item.itemID == MicroblockProxy.item.itemID)
            {
                val mcrClass = MicroblockClassRegistry.getMicroClass(item.getItemDamage)
                if(ItemMicroPart.getMaterialID(item) == material)
                    mat+=mcrClass.sizeToVolume(item.getItemDamage&0xFF)
            })
        return mat >= mUnits
    }
    
    def expand(mpart:CommonMicroblock):ExecutablePlacement = expand(mpart, create(mpart.getSize+size, mpart.getSlot, mpart.material))
    
    def expand(mpart:Microblock, npart:Microblock):ExecutablePlacement = 
    {
        if(!materialCheck(mpart.getSize))
            return null
        
        if(mpart.tile.canReplacePart(mpart, npart))
            return new ExpandingPlacement(new BlockCoord(mpart.tile), npart, mpart)
        return null
    }
    
    def internalPlacement(htile:TileMultipart, slot:Int):ExecutablePlacement = internalPlacement(htile, create(size, slot, material))
    
    def internalPlacement(htile:TileMultipart, npart:Microblock):ExecutablePlacement = 
    {
        if(htile.canAddPart(npart))
            return new AdditionPlacement(new BlockCoord(htile), npart)
        return null
    }
    
    def externalPlacement(slot:Int):ExecutablePlacement = externalPlacement(create(size, slot, material))
    
    def externalPlacement(npart:Microblock):ExecutablePlacement = 
    {
        val pos = this.pos.copy().offset(side)
        if(TileMultipart.canPlacePart(world, pos, npart))
            return new AdditionPlacement(pos, npart)
        return null
    }
    
    def getHitDepth(vhit:Vector3, side:Int):Double = 
        vhit.copy.scalarProject(Rotation.axes(side)) + (side%2^1)
    
    def create(size:Int, slot:Int, material:Int) = mcrClass.create(size, slot, material, world.isRemote)
}
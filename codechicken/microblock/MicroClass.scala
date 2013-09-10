package codechicken.microblock

import codechicken.lib.lighting.CCRBModel
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import codechicken.lib.vec.BlockCoord
import codechicken.lib.vec.Vector3
import codechicken.lib.render.CCModel
import codechicken.multipart.TMultiPart
import net.minecraft.entity.player.EntityPlayer
import codechicken.multipart.TileMultipart
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side

trait MicroblockClass
{
    var classID:Int = _
    
    def itemSlot() = 3
    
    @SideOnly(Side.CLIENT)
    def renderHighlight(world:World, player:EntityPlayer, hit:MovingObjectPosition, size:Int, material:Int):Boolean =
    {
        placementProperties.placementGrid.render(new Vector3(hit.hitVec), hit.sideHit)
        
        val placement = MicroblockPlacement(world, player, hit, size, material, !player.capabilities.isCreativeMode, placementProperties)
        if(placement != null)
            CommonMicroblock.renderHighlight(world, placement.pos, placement.part.asInstanceOf[MicroblockClient])
        
        return true
    }
    
    def getName():String
    
    def create(client:Boolean):CommonMicroblock
    
    def create(size:Int, slot:Int, material:Int, client:Boolean):CommonMicroblock
    
    def placementProperties():PlacementProperties
    
    def register(id:Int) = MicroblockClassRegistry.registerMicroClass(this, id)
}
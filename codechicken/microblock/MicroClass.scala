package codechicken.microblock

import codechicken.core.lighting.CCRBModel
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World
import codechicken.core.vec.BlockCoord
import codechicken.core.vec.Vector3
import codechicken.core.render.CCModel
import codechicken.multipart.TMultiPart
import net.minecraft.entity.player.EntityPlayer
import codechicken.multipart.TileMultipart
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side

trait MicroblockClass
{
    var classID:Int = _
    
    def getDisplayName(size:Int):String
    
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
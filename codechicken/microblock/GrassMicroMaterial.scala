package codechicken.microblock

import net.minecraft.util.Icon
import codechicken.lib.vec.Vector3
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.block.Block
import codechicken.lib.render.Vertex5
import codechicken.lib.render.IconTransformation
import net.minecraft.block.BlockGrass
import codechicken.lib.render.IUVTransformation
import codechicken.lib.render.UV
import codechicken.lib.lighting.LightMatrix

class TopIconTransformation(h:Double, uvt:IUVTransformation) extends IUVTransformation
{
    def transform(uv:UV)
    {
        uv.v -= 1-h
        uvt.transform(uv)
    }
}

class GrassMicroMaterial extends BlockMicroMaterial(Block.grass, 0)
{
    var sideIconT:IconTransformation = _
    
    override def loadIcons()
    {
        super.loadIcons()
        sideIconT = new IconTransformation(BlockGrass.getIconSideOverlay)
    }
    
    override def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, part:IMicroMaterialRender)
    {
        val colour = getColour(part)
        if(side%6 == 1)
            renderMicroFace(verts, side, pos, lightMatrix, colour, icont)
        else
            renderMicroFace(verts, side, pos, lightMatrix, -1, icont)
        
        if(side%6 > 1 && side%6 < 6)
            renderMicroFace(verts, side, pos, lightMatrix, colour, new TopIconTransformation(part.getRenderBounds.max.y, sideIconT))
    }
}

class TopMicroMaterial($block:Block, $meta:Int = 0) extends BlockMicroMaterial($block, $meta)
{
    override def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, part:IMicroMaterialRender)
    {
        if(side%6 > 1 && side%6 < 6)
            renderMicroFace(verts, side, pos, lightMatrix, getColour(part), new TopIconTransformation(part.getRenderBounds.max.y, icont))
        else
            super.renderMicroFace(verts, side, pos, lightMatrix, part)
    }
}
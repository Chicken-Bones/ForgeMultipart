package codechicken.microblock

import codechicken.lib.vec.{Cuboid6, Vector3}
import net.minecraft.block.Block
import codechicken.lib.render.{ColourMultiplier, CCRenderState}
import net.minecraft.block.BlockGrass
import net.minecraft.init.Blocks
import codechicken.lib.render.uv.{UVTranslation, IconTransformation}
import codechicken.lib.render.BlockRenderer.BlockFace

class GrassMicroMaterial extends BlockMicroMaterial(Blocks.grass, 0)
{
    var sideIconT:IconTransformation = _
    
    override def loadIcons()
    {
        super.loadIcons()
        sideIconT = new IconTransformation(BlockGrass.getIconSideOverlay)
    }

    override def renderMicroFace(pos:Vector3, pass:Int, bounds:Cuboid6) {
        val face = CCRenderState.model.asInstanceOf[BlockFace]
        if(pass != -1)
            face.computeLightCoords()

        if(face.side == 1)
            MaterialRenderHelper.start(pos, pass, icont).blockColour(getColour(pass)).lighting().render()
        else
            MaterialRenderHelper.start(pos, pass, icont).lighting().render()

        if(face.side > 1)
            MaterialRenderHelper.start(pos, pass, new UVTranslation(0, bounds.max.y-1) ++ sideIconT)
                .blockColour(getColour(pass)).lighting().render()
    }
}

class TopMicroMaterial($block:Block, $meta:Int = 0) extends BlockMicroMaterial($block, $meta)
{
    override def renderMicroFace(pos:Vector3, pass:Int, bounds:Cuboid6)
    {
        val face = CCRenderState.model.asInstanceOf[BlockFace]
        if(face.side <= 1)
            MaterialRenderHelper.start(pos, pass, icont).blockColour(getColour(pass)).lighting().render()
        else
            MaterialRenderHelper.start(pos, pass, new UVTranslation(0, bounds.max.y-1) ++ icont)
                .blockColour(getColour(pass)).lighting().render()
    }
}
package codechicken.multipart

import codechicken.core.vec.Cuboid6
import codechicken.core.raytracer.IndexedCuboid6
import net.minecraft.client.renderer.RenderBlocks
import codechicken.core.render.CCRenderState
import codechicken.core.render.RenderUtils
import codechicken.scala.ScalaBridge._
import codechicken.core.vec.Vector3
import codechicken.core.render.IconTransformation
import codechicken.scala.JSeq

abstract class JCuboidPart extends TCuboidPart

trait TCuboidPart extends TMultiPart
{
    def getBounds:Cuboid6
    
    override def getSubParts:JSeq[IndexedCuboid6] = Seq(new IndexedCuboid6(0, getBounds))
    
    override def getCollisionBoxes:JSeq[Cuboid6] = Seq(getBounds)
    
    override def drawBreaking(renderBlocks:RenderBlocks)
    {
        CCRenderState.reset()
        RenderUtils.renderBlock(getBounds, 0, Vector3.fromTileEntity(tile), new IconTransformation(renderBlocks.overrideBlockTexture))
    }
}
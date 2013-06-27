package codechicken.multipart

import codechicken.core.vec.Cuboid6
import codechicken.core.raytracer.IndexedCuboid6
import net.minecraft.client.renderer.RenderBlocks
import codechicken.core.render.CCRenderState
import codechicken.core.render.RenderUtils
import codechicken.core.vec.Vector3
import codechicken.core.render.IconTransformation
import scala.collection.JavaConversions._
import java.lang.Iterable

abstract class JCuboidPart extends TCuboidPart

trait TCuboidPart extends TMultiPart
{
    def getBounds:Cuboid6
    
    override def getSubParts:Iterable[IndexedCuboid6] = Seq(new IndexedCuboid6(0, getBounds))
    
    override def getCollisionBoxes:Iterable[Cuboid6] = Seq(getBounds)
    
    override def drawBreaking(renderBlocks:RenderBlocks)
    {
        CCRenderState.reset()
        RenderUtils.renderBlock(getBounds, 0, Vector3.fromTileEntity(tile), null, -1, new IconTransformation(renderBlocks.overrideBlockTexture))
    }
}
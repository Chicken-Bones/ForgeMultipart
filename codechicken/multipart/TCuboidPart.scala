package codechicken.multipart

import codechicken.lib.vec.Cuboid6
import codechicken.lib.raytracer.IndexedCuboid6
import net.minecraft.client.renderer.RenderBlocks
import codechicken.lib.render.CCRenderState
import codechicken.lib.render.RenderUtils
import codechicken.lib.vec.Vector3
import codechicken.lib.render.IconTransformation
import scala.collection.JavaConversions._
import java.lang.Iterable
import codechicken.lib.vec.Translation

abstract class JCuboidPart extends TCuboidPart

trait TCuboidPart extends TMultiPart
{
    def getBounds:Cuboid6
    
    override def getSubParts:Iterable[IndexedCuboid6] = Seq(new IndexedCuboid6(0, getBounds))
    
    override def getCollisionBoxes:Iterable[Cuboid6] = Seq(getBounds)
    
    override def drawBreaking(renderBlocks:RenderBlocks)
    {
        CCRenderState.reset()
        RenderUtils.renderBlock(getBounds, 0, new Translation(x, y, z), new IconTransformation(renderBlocks.overrideBlockTexture), null)
    }
}
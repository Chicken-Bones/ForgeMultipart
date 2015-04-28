package codechicken.multipart

import codechicken.lib.vec.Cuboid6
import codechicken.lib.raytracer.IndexedCuboid6
import net.minecraft.client.renderer.RenderBlocks
import codechicken.lib.render.{BlockRenderer, CCRenderState, RenderUtils}
import scala.collection.JavaConversions._
import java.lang.Iterable
import codechicken.lib.vec.Translation
import codechicken.lib.render.uv.IconTransformation
import cpw.mods.fml.relauncher.{Side, SideOnly}

/**
 * Java class implementation
 */
abstract class JCuboidPart extends TCuboidPart

/**
 * Trait for parts that are simply a cuboid, having one bounding box. Overrides multipart functions to this effect.
 */
trait TCuboidPart extends TMultiPart
{
    /**
     * Return the bounding Cuboid6 for this part.
     */
    def getBounds:Cuboid6
    
    override def getSubParts:Iterable[IndexedCuboid6] = Seq(new IndexedCuboid6(0, getBounds))
    
    override def getCollisionBoxes:Iterable[Cuboid6] = Seq(getBounds)

    @SideOnly(Side.CLIENT)
    override def drawBreaking(renderBlocks:RenderBlocks)
    {
        CCRenderState.reset()
        CCRenderState.setPipeline(new Translation(x, y, z), new IconTransformation(renderBlocks.overrideBlockTexture))
        BlockRenderer.renderCuboid(getBounds, 0)
    }
}
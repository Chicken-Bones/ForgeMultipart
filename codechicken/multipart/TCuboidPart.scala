package codechicken.multipart

import codechicken.lib.vec.{ Cuboid6, BlockCoord }
import codechicken.lib.raytracer.{ IndexedCuboid6, RayTracer }
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.{ Vec3, MovingObjectPosition }
import codechicken.lib.render.CCRenderState
import codechicken.lib.render.RenderUtils
import codechicken.lib.vec.Vector3
import codechicken.lib.render.IconTransformation
import scala.collection.JavaConversions._
import java.lang.Iterable
import java.util.{ List => JList }
import codechicken.lib.vec.Translation

abstract class JCuboidPart extends TCuboidPart

trait TCuboidPart extends TMultiPart
{
    def getBounds:Cuboid6
    
    override def collisionRayTrace(start: Vec3, end: Vec3) = {
      val offset = new Vector3(x, y, z)
      val boxes: JList[IndexedCuboid6] = getCollisionBoxes.map{ c => new IndexedCuboid6(0, c.copy.add(offset)) }.toList
      RayTracer.instance.rayTraceCuboids(
        new Vector3(start),
        new Vector3(end),
        boxes,
        new BlockCoord(x, y, z),
        tile.blockType)
    }

    override def getCollisionBoxes:Iterable[Cuboid6] = Seq(getBounds)
    
    override def drawBreaking(renderBlocks:RenderBlocks)
    {
        CCRenderState.reset()
        RenderUtils.renderBlock(getBounds, 0, new Translation(x, y, z), new IconTransformation(renderBlocks.overrideBlockTexture), null)
    }
}

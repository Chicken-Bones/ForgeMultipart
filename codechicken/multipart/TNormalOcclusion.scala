package codechicken.multipart

import codechicken.core.vec.Cuboid6
import codechicken.scala.JSeq
import codechicken.scala.ScalaBridge._

object NormalOcclusionTest
{
    def apply(boxes1:Traversable[Cuboid6], boxes2:Traversable[Cuboid6]):Boolean =
        boxes1.forall(v1 => boxes2.forall(v2 => !v1.intersects(v2)))
    
    def apply(part1:JNormalOcclusion, part2:TMultiPart):Boolean = 
    {
        var boxes = Seq[Cuboid6]()
        if(part2.isInstanceOf[JNormalOcclusion])
            boxes = boxes++part2.asInstanceOf[JNormalOcclusion].getOcclusionBoxes
        
        if(part2.isInstanceOf[JPartialOcclusion])
            boxes = boxes++part2.asInstanceOf[JPartialOcclusion].getPartialOcclusionBoxes

        return NormalOcclusionTest(boxes, part1.getOcclusionBoxes) 
    }
}

trait JNormalOcclusion
{
    def getOcclusionBoxes():JSeq[Cuboid6]
}

trait TNormalOcclusion extends TMultiPart with JNormalOcclusion
{
    override def occlusionTest(npart:TMultiPart):Boolean =
        NormalOcclusionTest(this, npart) && super.occlusionTest(npart)
}
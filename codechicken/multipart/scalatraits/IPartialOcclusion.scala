package codechicken.multipart.scalatraits

import codechicken.multipart.TileMultipart
import codechicken.multipart.TMultiPart
import codechicken.multipart.JPartialOcclusion
import codechicken.multipart.PartialOcclusionTest

trait TPartialOcclusionTile extends TileMultipart
{
    override def occlusionTest(parts:Seq[TMultiPart], npart:TMultiPart):Boolean =
    {
        if(npart.isInstanceOf[JPartialOcclusion] && !partialOcclusionTest(parts:+npart))
            return false
        
        return super.occlusionTest(parts, npart)
    }
    
    def partialOcclusionTest(parts:Seq[TMultiPart]):Boolean =
    {
        val test = new PartialOcclusionTest(parts.length)
        for(i <- 0 until parts.length)
        {
            val part = parts(i)
            if(part.isInstanceOf[JPartialOcclusion])
                test.fill(i, part.asInstanceOf[JPartialOcclusion])
        }
        return test()
    }
}
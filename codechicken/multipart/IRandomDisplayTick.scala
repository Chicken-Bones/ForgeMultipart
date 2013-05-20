package codechicken.multipart

import scala.collection.mutable.ArrayBuffer
import java.util.Random

trait IRandomDisplayTick
{
    def randomDisplayTick(random:Random)
}

trait TRandomDisplayTickTile extends TileMultipartClient
{
    override def randomDisplayTick(random:Random)
    {
        partList.foreach(p =>
            if(p.isInstanceOf[IRandomDisplayTick])
                p.asInstanceOf[IRandomDisplayTick].randomDisplayTick(random))
    }
}
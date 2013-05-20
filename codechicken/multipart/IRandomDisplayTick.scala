package codechicken.multipart

import scala.collection.mutable.ArrayBuffer
import java.util.Random

trait IRandomDisplayTick
{
    def randomDisplayTick(random:Random)
}

trait TRandomDisplayTickTile extends TileMultipartClient
{
    var displayParts = ArrayBuffer[IRandomDisplayTick]()
    
    override def loadFrom(that:TileMultipart)
    {
        super.loadFrom(that)
        if(that.isInstanceOf[TRandomDisplayTickTile])
            displayParts = that.asInstanceOf[TRandomDisplayTickTile].displayParts
    }
    
    override def randomDisplayTick(random:Random)
    {
        displayParts.foreach(_.randomDisplayTick(random))
    }
    
    override def partAdded(part:TMultiPart)
    {
        super.partAdded(part)
        if(part.isInstanceOf[IRandomDisplayTick])
            displayParts+=part.asInstanceOf[IRandomDisplayTick]
    }
    
    override def partRemoved(part:TMultiPart, p:Int)
    {
        super.partRemoved(part, p)
        if(part.isInstanceOf[IRandomDisplayTick])
            displayParts-=part.asInstanceOf[IRandomDisplayTick]
    }
    
    override def clearParts()
    {
        super.clearParts()
        displayParts.clear
    }
}
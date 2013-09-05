package codechicken.multipart.scalatraits

import codechicken.multipart.TMultiPart
import codechicken.multipart.INeighborTileChange
import codechicken.multipart.TileMultipart
import codechicken.lib.vec.BlockCoord

trait TTileChangeTile extends TileMultipart {
    var weakTileChanges = false
    
    override def copyFrom(that:TileMultipart)
    {
        super.copyFrom(that)
        if(that.isInstanceOf[TTileChangeTile])
            weakTileChanges = that.asInstanceOf[TTileChangeTile].weakTileChanges
    }
    
    override def partAdded(part:TMultiPart)
    {
        super.partAdded(part)
        if(part.isInstanceOf[INeighborTileChange])
            weakTileChanges|=part.asInstanceOf[INeighborTileChange].weakTileChanges
    }
    
    override def partRemoved(part:TMultiPart, p:Int) {
        super.partRemoved(part, p)
        weakTileChanges = partList.find{p =>
                p.isInstanceOf[INeighborTileChange] && p.asInstanceOf[INeighborTileChange].weakTileChanges
            }.isDefined
    }
    
    override def onNeighborTileChange(tileX:Int, tileY:Int, tileZ:Int)
    {
        super.onNeighborTileChange(tileX, tileY, tileZ)
        val offset = new BlockCoord(tileX, tileY, tileZ).sub(xCoord, yCoord, zCoord)
        val diff = offset.absSum
        val side = offset.toSide
        
        if(side < 0 || diff <= 0 || diff > 2)
            return
            
        val weak = diff == 2
        if(weak && !weakTileChanges)
            return
        
        TileMultipart.startOperation(this)
        for(p@(_p: INeighborTileChange) <- partList.iterator)
            p.onNeighborTileChanged(side, weak)
        TileMultipart.finishOperation(this)
    }
}
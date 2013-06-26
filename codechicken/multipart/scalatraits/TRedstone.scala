package codechicken.multipart.scalatraits

import codechicken.multipart.IRedstoneConnector
import codechicken.multipart.TileMultipart
import codechicken.multipart.IRedstonePart
import codechicken.multipart.RedstoneInteractions._
import codechicken.multipart.PartMap._
import codechicken.core.vec.Rotation._
import codechicken.multipart.IRedstoneTile

trait TRedstoneTile extends TileMultipart with IRedstoneTile
{
    override def strongPowerLevel(side:Int):Int =
    {
        var max = 0
        for(p@(_p: IRedstonePart) <- partList.iterator)
        {
            val l = p.strongPowerLevel(side)
            if(l > max) max = l
        }
        return max
    }
    
    def openConnections(side:Int):Int =
    {
        if(blocksRedstone(side))
            return 0
        
        var m = 0x10
        var i = 0
        while(i < 4)
        {
            if(!blocksRedstone(edgeBetween(side, rotateSide(side&6, i))))
                m|=1<<i
            i+=1
        }
        return m
    }
    
    def blocksRedstone(i:Int) = partMap(i) != null && partMap(i).blocksRedstone
    
    override def weakPowerLevel(side:Int):Int = 
        weakPowerLevel(side, otherConnectionMask(worldObj, xCoord, yCoord, zCoord, side, true))
    
    override def canConnectRedstone(side:Int):Boolean =
    {
        val vside = vanillaToSide(side)
        return (getConnectionMask(vside) & otherConnectionMask(worldObj, xCoord, yCoord, zCoord, vside, false)) > 0
    }
    
    def getConnectionMask(side:Int):Int = 
    {
        val mask = openConnections(side)
        var res = 0
        partList.foreach(p => 
            res|=connectionMask(p, side)&mask)
        return res
    }
    
    def weakPowerLevel(side:Int, mask:Int):Int = 
    {
        val tmask = openConnections(side)&mask
        var max = 0
        partList.foreach(p => 
            if((connectionMask(p, side)&tmask) > 0)
            {
                val l = p.asInstanceOf[IRedstonePart].weakPowerLevel(side)
                if(l > max) max = l
            })
        return max
    }
}


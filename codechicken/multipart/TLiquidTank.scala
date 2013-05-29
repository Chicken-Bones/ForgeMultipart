package codechicken.multipart

import net.minecraftforge.liquids.ITankContainer
import scala.collection.mutable.ListBuffer
import net.minecraftforge.liquids.LiquidStack
import net.minecraftforge.liquids.ILiquidTank
import net.minecraftforge.common.ForgeDirection

trait TLiquidTank extends TileMultipart with ITankContainer
{
    var tankList = ListBuffer[ITankContainer]()
    
    override def partAdded(part:TMultiPart)
    {
        super.partAdded(part)
        if(part.isInstanceOf[ITankContainer])
            tankList+=part.asInstanceOf[ITankContainer]
    }
    
    override def partRemoved(part:TMultiPart, p:Int)
    {
        super.partRemoved(part, p)
        if(part.isInstanceOf[ITankContainer])
            tankList-=part.asInstanceOf[ITankContainer]
    }
    
    override def loadFrom(that:TileMultipart)
    {
        super.loadFrom(that)
        if(that.isInstanceOf[TLiquidTank])
            tankList = that.asInstanceOf[TLiquidTank].tankList
    }
    
    override def clearParts()
    {
        super.clearParts()
        tankList.clear
    }
    
    override def getTanks(dir:ForgeDirection):Array[ILiquidTank] =
    {
        var tankCount:Int = 0
        tankList.foreach(t => tankCount += t.getTanks(dir).length)
        val tanks = new Array[ILiquidTank](tankCount)
        var i = 0
        tankList.foreach(p => p.getTanks(dir).foreach(t => {
            tanks(i) = t
            i+=1
        }))
        return tanks
    }
    
    override def getTank(dir:ForgeDirection, liquid:LiquidStack):ILiquidTank =
    {
        tankList.foreach(p => {
            val t = p.getTank(dir, liquid)
            if(t != null)
                return t//TODO: bad, slow
        })
        return null
    }
    
    override def fill(dir:ForgeDirection, liquid:LiquidStack, doFill:Boolean):Int = 
    {
        var filled = 0
        var initial = liquid.amount
        tankList.foreach(p => 
            filled+=p.fill(dir, copy(liquid, initial-filled), doFill)
        )
        return filled
    }
    
    def copy(liquid:LiquidStack, quantity:Int):LiquidStack = 
    {
        val copy = liquid.copy
        copy.amount = quantity
        return copy
    }
    
    override def drain(dir:ForgeDirection, amount:Int, doDrain:Boolean):LiquidStack = 
    {
        var drained:LiquidStack = null
        tankList.foreach(p => {
            val ret = p.drain(dir, amount-drained.amount, false)
            if(ret != null && ret.amount > 0 && (drained == null || drained.isLiquidEqual(ret)))
            {
                if(doDrain)
                    p.drain(dir, amount-drained.amount, true)
                
                if(drained == null)
                    drained = ret
                else
                    drained.amount+=ret.amount
            }
        })
        return drained
    }
    
    override def fill(tankIndex:Int, liquid:LiquidStack, doFill:Boolean):Int =
        throw new UnsupportedOperationException("Index based tank filling")
    
    override def drain(tankIndex:Int, maxDrain:Int, doDrain:Boolean):LiquidStack = 
        throw new UnsupportedOperationException("Index based tank draining")
}
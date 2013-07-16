package codechicken.multipart.scalatraits

import scala.collection.mutable.ListBuffer
import net.minecraftforge.common.ForgeDirection
import codechicken.multipart.TMultiPart
import codechicken.multipart.TileMultipart
import net.minecraftforge.fluids.IFluidHandler
import net.minecraftforge.fluids.FluidStack
import net.minecraftforge.fluids.IFluidTank
import net.minecraftforge.fluids.FluidTankInfo
import net.minecraftforge.fluids.Fluid

trait TTankContainerTile extends TileMultipart with IFluidHandler
{
    var tankList = ListBuffer[IFluidHandler]()
    
    override def copyFrom(that:TileMultipart)
    {
        super.copyFrom(that)
        if(that.isInstanceOf[TTankContainerTile])
            tankList = that.asInstanceOf[TTankContainerTile].tankList
    }
    
    override def partAdded(part:TMultiPart)
    {
        super.partAdded(part)
        if(part.isInstanceOf[IFluidHandler])
            tankList+=part.asInstanceOf[IFluidHandler]
    }
    
    override def partRemoved(part:TMultiPart, p:Int)
    {
        super.partRemoved(part, p)
        if(part.isInstanceOf[IFluidHandler])
            tankList-=part.asInstanceOf[IFluidHandler]
    }
    
    override def clearParts()
    {
        super.clearParts()
        tankList.clear
    }
    
    override def getTankInfo(dir:ForgeDirection):Array[FluidTankInfo] =
    {
        var tankCount:Int = 0
        tankList.foreach(t => tankCount += t.getTankInfo(dir).length)
        val tanks = new Array[FluidTankInfo](tankCount)
        var i = 0
        tankList.foreach(p => p.getTankInfo(dir).foreach{t =>
            tanks(i) = t
            i+=1
        })
        return tanks
    }
    
    override def fill(dir:ForgeDirection, liquid:FluidStack, doFill:Boolean):Int = 
    {
        var filled = 0
        var initial = liquid.amount
        tankList.foreach(p => 
            filled+=p.fill(dir, copy(liquid, initial-filled), doFill)
        )
        return filled
    }
    
    override def canFill(dir:ForgeDirection, liquid:Fluid) = tankList.find(_.canFill(dir, liquid)).isDefined
    
    override def canDrain(dir:ForgeDirection, liquid:Fluid) = tankList.find(_.canDrain(dir, liquid)).isDefined
    
    def copy(liquid:FluidStack, quantity:Int):FluidStack = 
    {
        val copy = liquid.copy
        copy.amount = quantity
        return copy
    }
    
    override def drain(dir:ForgeDirection, amount:Int, doDrain:Boolean):FluidStack = 
    {
        var drained:FluidStack = null
        tankList.foreach{p =>
            val ret = p.drain(dir, amount-drained.amount, false)
            if(ret != null && ret.amount > 0 && (drained == null || drained.isFluidEqual(ret)))
            {
                if(doDrain)
                    p.drain(dir, amount-drained.amount, true)
                
                if(drained == null)
                    drained = ret
                else
                    drained.amount+=ret.amount
            }
        }
        return drained
    }
}
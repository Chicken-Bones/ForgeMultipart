package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Scale
import codechicken.lib.vec.Vector3
import Vector3._

object CornerPlacement extends PlacementProperties
{
    def microClass = CornerMicroClass
    
    def placementGrid = CornerPlacementGrid
    
    def opposite(slot:Int, side:Int) = ((slot-7)^(1<<(side>>1)))+7
}

object CornerMicroClass extends MicroblockClass
{
    var aBounds:Array[Cuboid6] = new Array(256)
    
    for(s <- 0 until 8)
    {
        val rx = if((s&4) != 0) -1 else 1
        val ry = if((s&1) != 0) -1 else 1
        val rz = if((s&2) != 0) -1 else 1
        val transform = new Scale(new Vector3(rx, ry, rz)).at(center)
        
        for(t <- 1 until 8)
        {
            val d = t/8D
            aBounds(t<<4|s) = new Cuboid6(0, 0, 0, d, d, d).apply(transform)
        }
    }
    
    override def itemSlot = 7
    
    def getName = "mcr_cnr"
    
    def create(client:Boolean) = 
        if(client)
            new CornerMicroblockClient
        else
            new CornerMicroblock
    
    def create(size:Int, slot:Int, material:Int, client:Boolean) = 
        if(client)
            new CornerMicroblockClient(size, slot, material)
        else
            new CornerMicroblock(size, slot, material)
    
    def placementProperties = CornerPlacement

    def getResistanceFactor = 1
}

class CornerMicroblockClient(shape$:Byte = 0, material$:Int = 0) extends CornerMicroblock(shape$, material$) with CommonMicroblockClient
{
    def this(size:Int, slot:Int, material:Int) = this((size<<4|(slot-7)).toByte, material)
}

class CornerMicroblock(shape$:Byte = 0, material$:Int = 0) extends CommonMicroblock(shape$, material$)
{
    def this(size:Int, slot:Int, material:Int) = this((size<<4|(slot-7)).toByte, material)
    
    def microClass = CornerMicroClass
    
    def getBounds = CornerMicroClass.aBounds(shape)
    
    override def getSlot = getShape+7
}
package codechicken.multipart

import net.minecraft.world.World
import net.minecraft.block.Block
import net.minecraft.block.BlockRedstoneWire
import net.minecraft.world.IBlockAccess
import net.minecraft.util.Direction

trait IRedstonePart
{
    def strongPowerLevel(side:Int):Int
    def weakPowerLevel(side:Int):Int
    def canConnectRedstone(side:Int):Boolean
}

/**
 * For parts like wires that adhere to a specific face, reduces redstone connections to the specific edge between two faces
 */
trait IFaceRedstonePart extends IRedstonePart
{
    def getFace():Int
}

trait TRedstoneTile extends TileMultipart with IRedstoneConnector
{
    import PartMap._
    import RedstoneInteractions._
    
    override def strongPowerLevel(side:Int):Int =
    {
        var max = 0
        partList.foreach(p => 
            if(p.isInstanceOf[IRedstonePart])
            {
                val l = p.asInstanceOf[IRedstonePart].strongPowerLevel(side)
                if(l > max) max = l
            }
        )
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
            if(!blocksRedstone(edgeBetween(side, rotateSide(side, i))))
                m|=1<<i
            i+=1
        }
        return m
    }
    
    def blocksRedstone(i:Int) = partMap(i) != null && partMap(i).blocksRedstone
    
    override def weakPowerLevel(side:Int):Int = 
    {
        val mask = openConnections(side)&otherConnectionMap(worldObj, xCoord, yCoord, zCoord, side, true)
        var max = 0
        partList.foreach(p => 
            if((connectionMask(p, side)&mask) > 0)
            {
                val l = p.asInstanceOf[IRedstonePart].weakPowerLevel(side)
                if(l > max) max = l
            })
        return max
    }
    
    override def canConnectRedstone(side:Int):Boolean =
    {
        val vside = vanillaToSide(side)
        return (getConnectionMap(vside) & otherConnectionMap(worldObj, xCoord, yCoord, zCoord, vside, false)) > 0
    }
    
    def getConnectionMap(side:Int):Int = 
    {
        val mask = openConnections(side)
        var res = 0
        partList.foreach(p => 
            res|=connectionMask(p, side)&mask)
        return res
    }
    
    def connectionMask(p:TMultiPart, side:Int):Int =
    {
        if(p.isInstanceOf[IRedstonePart] && p.asInstanceOf[IRedstonePart].canConnectRedstone(side))
        {
            if(p.isInstanceOf[IFaceRedstonePart])
                return rotationTo(side, p.asInstanceOf[IFaceRedstonePart].getFace)
            return 0x1F
        }
        return 0
    }
}

trait IRedstoneConnector
{
    def getConnectionMap(side:Int):Int
}

trait IRedstoneConnectorBlock
{
    def getConnectionMap(x:Int, y:Int, z:Int, side:Int):Int
}

object RedstoneInteractions
{
    import net.minecraft.util.Facing._
    
    val vanillaSideMap = Array(-2, -1, 0, 2, 3, 1)
    val sideVanillaMap = Array(1, 2, 5, 3, 4)
    
    def vanillaToSide(vside:Int) = sideVanillaMap(vside+1)
    
    def otherConnectionMap(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, power:Boolean):Int =
        getConnectionMap(world, x+offsetsXForSide(side), y+offsetsYForSide(side), z+offsetsZForSide(side), side^1, power)
    
    /**
     * @param power If true, don't test canConnectRedstone on blocks, just get a power transmission mask rather than a visual connection
     */
    def getConnectionMap(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, power:Boolean):Int =
    {
        val tile = world.getBlockTileEntity(x, y, z)
        if(tile.isInstanceOf[IRedstoneConnector])
            return tile.asInstanceOf[IRedstoneConnector].getConnectionMap(side)
        
        val block = Block.blocksList(world.getBlockId(x, y, z))
        if(block == null)
            return 0
        
        if(block.isInstanceOf[IRedstoneConnectorBlock])
            return block.asInstanceOf[IRedstoneConnectorBlock].getConnectionMap(x, y, z, side)
        
        //start vanilla interactions
        if(side == 0)//vanilla doesn't handle side 0
        {
            if(power)
                return 0x1F
            return 0
        }
        
        if(block == Block.redstoneWire)
        {
            if(side != 1)
                return 4
            return 0x1F
        }
        
        if(block == Block.redstoneComparatorActive || block == Block.redstoneComparatorIdle)
        {
            if(side != 1)
                return 4
            return 0
        }
        
        val vside = vanillaSideMap(side)
        if(block == Block.redstoneRepeaterActive || block == Block.redstoneRepeaterIdle)//stupid minecraft hardcodes
        {
             val meta = world.getBlockMetadata(x, y, z);
             if(vside == (meta & 3) || vside == Direction.rotateOpposite(meta & 3))
                 return 4
             return 0
        }
        
        if(power || block.canConnectRedstone(world, x, y, z, vside))//some blocks accept power without visualising connections
            return 0x1F
        
        return 0
    }
}
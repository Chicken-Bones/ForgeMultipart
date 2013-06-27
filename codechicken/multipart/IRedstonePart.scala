package codechicken.multipart

import net.minecraft.world.World
import net.minecraft.block.Block
import net.minecraft.block.BlockRedstoneWire
import net.minecraft.world.IBlockAccess
import net.minecraft.util.Direction
import PartMap._
import codechicken.core.vec.Rotation._

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

/**
 * Internal interface for TileMultipart instances hosting IRedstonePart
 */
trait IRedstoneTile extends IRedstoneConnector
{
    def openConnections(side:Int):Int
    def blocksRedstone(i:Int):Boolean
}

/**
 * All connection masks are a 5 bit map. 
 * The lowest 4 bits correspond to the connection toward the face specfied rotateSide(side&6, b) where b is the bit index from lowest to highest.
 * Bit 5 corresponds to a connection opposite side.
 */
trait IRedstoneConnector
{
    def getConnectionMask(side:Int):Int
    def weakPowerLevel(side:Int, mask:Int):Int
}

/**
 * Block version of IRedstoneConnector
 */
trait IRedstoneConnectorBlock
{
    def getConnectionMask(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int):Int
    def weakPowerLevel(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, mask:Int):Int
}

object RedstoneInteractions
{
    import net.minecraft.util.Facing._
    
    val vanillaSideMap = Array(-2, -1, 0, 2, 3, 1)
    val sideVanillaMap = Array(1, 2, 5, 3, 4)
    
    def getPowerTo(p:TMultiPart, side:Int):Int =
    {
        val tile = p.tile
        return getPowerTo(tile.worldObj, tile.xCoord, tile.yCoord, tile.zCoord, side,
                tile.asInstanceOf[IRedstoneTile].openConnections(side)&connectionMask(p, side))
    }
    
    def getPowerTo(world:World, x:Int, y:Int, z:Int, side:Int, mask:Int):Int =
        getPower(world, x+offsetsXForSide(side), y+offsetsYForSide(side), z+offsetsZForSide(side), side^1, mask)
        
    def getPower(world:World, x:Int, y:Int, z:Int, side:Int, mask:Int):Int =
    {
        val tile = world.getBlockTileEntity(x, y, z)
        if(tile.isInstanceOf[IRedstoneConnector])
            return tile.asInstanceOf[IRedstoneConnector].weakPowerLevel(side, mask)
        
        val block = Block.blocksList(world.getBlockId(x, y, z))
        if(block == null)
            return 0
        
        if(block.isInstanceOf[IRedstoneConnectorBlock])
            return block.asInstanceOf[IRedstoneConnectorBlock].weakPowerLevel(world, x, y, z, side, mask)
        
        val vmask = vanillaConnectionMask(block, world, x, y, z, side, true)
        if((vmask&mask) > 0)
        {
            var m = world.getIndirectPowerLevelTo(x, y, z, side^1)
            if(m < 15 && block == Block.redstoneWire)
                m = Math.max(m, world.getBlockMetadata(x, y, z))//painful vanilla kludge
            return m
        }
        return 0
    }
    
    def vanillaToSide(vside:Int) = sideVanillaMap(vside+1)
    
    def otherConnectionMask(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, power:Boolean):Int =
        getConnectionMask(world, x+offsetsXForSide(side), y+offsetsYForSide(side), z+offsetsZForSide(side), side^1, power)
        
    def connectionMask(p:TMultiPart, side:Int):Int =
    {
        if(p.isInstanceOf[IRedstonePart] && p.asInstanceOf[IRedstonePart].canConnectRedstone(side))
        {
            if(p.isInstanceOf[IFaceRedstonePart])
                return 1<<rotationTo(side&6, p.asInstanceOf[IFaceRedstonePart].getFace)
            return 0x1F
        }
        return 0
    }
    
    /**
     * @param power If true, don't test canConnectRedstone on blocks, just get a power transmission mask rather than a visual connection
     */
    def getConnectionMask(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, power:Boolean):Int =
    {
        val tile = world.getBlockTileEntity(x, y, z)
        if(tile.isInstanceOf[IRedstoneConnector])
            return tile.asInstanceOf[IRedstoneConnector].getConnectionMask(side)
        
        val block = Block.blocksList(world.getBlockId(x, y, z))
        if(block == null)
            return 0
        
        if(block.isInstanceOf[IRedstoneConnectorBlock])
            return block.asInstanceOf[IRedstoneConnectorBlock].getConnectionMask(world, x, y, z, side)
        
        return vanillaConnectionMask(block, world, x, y, z, side, power)
    }
    
    def vanillaConnectionMask(block:Block, world:IBlockAccess, x:Int, y:Int, z:Int, side:Int, power:Boolean):Int =
    {
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
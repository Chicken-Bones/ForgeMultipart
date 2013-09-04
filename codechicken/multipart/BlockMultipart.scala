package codechicken.multipart

import java.util.List
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import net.minecraft.util.Vec3
import net.minecraft.util.MovingObjectPosition
import java.util.LinkedList
import codechicken.lib.raytracer.IndexedCuboid6
import codechicken.lib.vec.Cuboid6
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.Vector3
import codechicken.lib.vec.BlockCoord
import net.minecraft.entity.player.EntityPlayer
import java.util.Random
import java.util.ArrayList
import net.minecraft.item.ItemStack
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.client.Minecraft
import net.minecraft.util.Icon
import net.minecraft.client.renderer.texture.IconRegister
import codechicken.lib.render.TextureSpecial
import codechicken.lib.render.TextureUtils
import net.minecraft.world.IBlockAccess
import codechicken.lib.raytracer.ExtendedMOP
import scala.collection.JavaConversions._
import codechicken.multipart.scalatraits.TTileChangeTile

object BlockMultipart
{
    def getTile(world:IBlockAccess, x:Int, y:Int, z:Int):TileMultipart = 
    {
        val tile = world.getBlockTileEntity(x, y, z)
        if(tile.isInstanceOf[TileMultipart]) 
            tile.asInstanceOf[TileMultipart] 
        else
            null
    }
    
    def getClientTile(world:IBlockAccess, x:Int, y:Int, z:Int):TileMultipartClient = 
    {
        val tile = world.getBlockTileEntity(x, y, z)
        if(tile.isInstanceOf[TileMultipartClient]) 
            tile.asInstanceOf[TileMultipartClient]
        else
            null
    }
    
    def drawHighlight(world:World, player:EntityPlayer, hit:MovingObjectPosition, frame:Float):Boolean =
    {
        val tile = getTile(world, hit.blockX, hit.blockY, hit.blockZ)
        if(tile == null)
            return false
        
        val hitInfo:(Int, _) = ExtendedMOP.getData(hit)
        return tile.partList(hitInfo._1).drawHighlight(hit, player, frame)
    }
}

trait BlockMultipart extends Block
{
    import BlockMultipart._
    
    override def hasTileEntity(meta:Int = 0) = true
    
    override def isBlockSolidOnSide(world:World, x:Int, y:Int, z:Int, side:ForgeDirection):Boolean =
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) tile.isSolid(side.ordinal()) else false
    }
    
    override def onNeighborBlockChange(world:World, x:Int, y:Int, z:Int, id:Int)
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) 
            tile.onNeighborBlockChange()
    }
    
    override def collisionRayTrace(world:World, x:Int, y:Int, z:Int, start:Vec3, end:Vec3):MovingObjectPosition = 
    {
        val boxes:LinkedList[IndexedCuboid6] = new LinkedList;
        val tile = getTile(world, x, y, z)
        if(tile == null)
            return null
        
        for(i <- 0 until tile.partList.size)
            tile.partList(i).getSubParts.foreach(c => 
                boxes.add(new IndexedCuboid6((i, c.data), c.copy.add(new Vector3(x, y, z)))))
                
        return RayTracer.instance.rayTraceCuboids(new Vector3(start), new Vector3(end), boxes, new BlockCoord(x, y, z), this)        
    }

    override def removeBlockByPlayer(world:World, player:EntityPlayer, x:Int, y:Int, z:Int):Boolean =
    {
        val hit = RayTracer.retraceBlock(world, player, x, y, z)
        val tile = getTile(world, x, y, z)
        
        if(hit == null || tile == null)
        {
            dropAndDestroy(world, x, y, z)
            return true
        }
        
        val hitInfo:(Int, _) = ExtendedMOP.getData(hit)
        if(world.isRemote)
        {
            tile.partList(hitInfo._1).addDestroyEffects(Minecraft.getMinecraft().effectRenderer)
            return true
        }
        
        return tile.harvestPart(hitInfo._1, !player.capabilities.isCreativeMode);
    }
    
    def dropAndDestroy(world:World, x:Int, y:Int, z:Int)
    {
        val tile = getTile(world, x, y, z)
        if(tile != null && !world.isRemote)
            tile.dropItems(getBlockDropped(world, x, y, z, 0, 0))
        
        world.setBlockToAir(x, y, z)
    }
    
    override def quantityDropped(meta:Int, fortune:Int, random:Random) = 0
    
    override def getBlockDropped(world:World, x:Int, y:Int, z:Int, meta:Int, fortune:Int):ArrayList[ItemStack] =
    {
        val ai = new ArrayList[ItemStack]()
        if(world.isRemote)
            return ai
        
        val tile = getTile(world, x, y, z)
        if(tile != null)
            tile.partList.foreach(part => part.getDrops.foreach(item => ai.add(item)))
        return ai
    }
    
    override def addCollisionBoxesToList(world:World, x:Int, y:Int, z:Int, ebb:AxisAlignedBB, list$:List[_], entity:Entity)
    {
        val list = list$.asInstanceOf[List[AxisAlignedBB]]
        val tile = getTile(world, x, y, z)
        if(tile != null)
            tile.partList.foreach(part => 
                part.getCollisionBoxes.foreach{c => 
                        val aabb = c.toAABB().offset(x, y, z)
                        if(aabb.intersectsWith(ebb))
                            list.add(aabb)
                })
    }
    
    override def addBlockHitEffects(world:World, hit:MovingObjectPosition, effectRenderer:EffectRenderer):Boolean =
    {
        val tile = getClientTile(world, hit.blockX, hit.blockY, hit.blockZ)
        if(tile != null)
            tile.partList(ExtendedMOP.getData[(Int, _)](hit)._1).addHitEffects(hit, effectRenderer)
        
        return true
    }
    
    override def addBlockDestroyEffects(world:World, x:Int, y:Int, z:Int, s:Int, effectRenderer:EffectRenderer) = true
    
    override def renderAsNormalBlock() = false
    
    override def isOpaqueCube() = false
    
    override def getRenderType() = TileMultipart.renderID
    
    override def isAirBlock(world:World, x:Int, y:Int, z:Int) = getTile(world, x, y, z) == null || getTile(world, x, y, z).partList.isEmpty
    
    override def getRenderBlockPass = 1
    
    override def canRenderInPass(pass:Int):Boolean = 
    {
        MultipartRenderer.pass = pass
        return true
    }
    
    override def getPickBlock(hit:MovingObjectPosition, world:World, x:Int, y:Int, z:Int):ItemStack =
    {
        val tile = getTile(world, x, y, z)
        if(tile != null)
        {
            val hitInfo:(Int, _) = ExtendedMOP.getData(hit)
            return tile.partList(hitInfo._1).pickItem(hit)
        }
        return null
    }
    
    override def getPlayerRelativeBlockHardness(player:EntityPlayer, world:World, x:Int, y:Int, z:Int):Float = 
    {
        val hit = RayTracer.retraceBlock(world, player, x, y, z)
        val tile = getTile(world, x, y, z)
        if(hit != null && tile != null)
            return tile.partList(ExtendedMOP.getData[(Int, _)](hit)._1).getStrength(hit, player)/30F;
        
        return 1
    }
    
    /**
     * Kludge to set PROTECTED blockIcon to a blank icon 
     */
    override def registerIcons(register:IconRegister)
    {
        val n = getUnlocalizedName();
        val icon = TextureUtils.getBlankIcon(16, register);
        func_111022_d(icon.getIconName)
        super.registerIcons(register)
    }
    
    override def getLightValue(world:IBlockAccess, x:Int, y:Int, z:Int):Int = 
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) tile.getLightValue else 0
    }
    
    override def randomDisplayTick(world:World, x:Int, y:Int, z:Int, random:Random)
    {
        val tile = getClientTile(world, x, y, z)
        if(tile != null) tile.randomDisplayTick(random)
    }
    
    override def onBlockActivated(world:World, x:Int, y:Int, z:Int, player:EntityPlayer, side:Int, hitX:Float, hitY:Float, hitZ:Float):Boolean =
    {
        val hit = RayTracer.retraceBlock(world, player, x, y, z);
        if(hit == null)
            return false
        
        val tile = getTile(world, x, y, z)
        if(tile == null) 
            return false
        
        val hitInfo:(Int, _) = ExtendedMOP.getData(hit)
        return tile.partList(hitInfo._1).activate(player, hit, player.getHeldItem())
    }
    
    override def onBlockClicked(world:World, x:Int, y:Int, z:Int, player:EntityPlayer)
    {
        val hit = RayTracer.retraceBlock(world, player, x, y, z);
        if(hit == null)
            return
        
        val tile = getTile(world, x, y, z)
        if(tile == null) 
            return
        
        val hitInfo:(Int, _) = ExtendedMOP.getData(hit)
        tile.partList(hitInfo._1).click(player, hit, player.getHeldItem())
    }
    
    override def isProvidingStrongPower(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int):Int =
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) return tile.strongPowerLevel(side^1)
        return 0
    }
    
    override def isProvidingWeakPower(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int):Int =
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) return tile.weakPowerLevel(side^1)
        return 0
    }
    
    override def canConnectRedstone(world:IBlockAccess, x:Int, y:Int, z:Int, side:Int):Boolean =
    {
        val tile = getTile(world, x, y, z)
        if(tile != null) return tile.canConnectRedstone(side)
        return false
    }
    
    override def onEntityCollidedWithBlock(world:World, x:Int, y:Int, z:Int, entity:Entity)
    {
        val tile = getTile(world, x, y, z)
        if(tile != null)
            tile.onEntityCollision(entity)
    }
    
    override def onNeighborTileChange(world:World, x:Int, y:Int, z:Int, tileX:Int, tileY:Int, tileZ:Int)
    {
        val tile = getTile(world, x, y, z)
        if(tile != null)
            tile.onNeighborTileChange(tileX, tileY, tileZ)
    }
    
    override def weakTileChanges() = true
    
    override def canProvidePower = true
}

class BlockMultipartImpl(id:Int) extends Block(id, Material.rock) with BlockMultipart
{
    
}
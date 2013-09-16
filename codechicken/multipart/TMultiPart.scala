package codechicken.multipart

import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Vector3
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import codechicken.lib.raytracer.IndexedCuboid6
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.util.Icon
import net.minecraft.client.renderer.RenderBlocks
import codechicken.lib.render.RenderUtils
import codechicken.lib.render.IconTransformation
import codechicken.lib.vec.BlockCoord
import net.minecraft.world.World
import net.minecraft.tileentity.TileEntity
import codechicken.lib.render.CCRenderState
import codechicken.lib.lighting.LazyLightMatrix
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import net.minecraft.entity.Entity
import java.lang.Iterable
import scala.collection.JavaConversions._
import net.minecraft.util.Vec3
import codechicken.lib.raytracer.ExtendedMOP
import codechicken.lib.raytracer.RayTracer

abstract class TMultiPart
{
    var tile:TileMultipart = _
    
    def getTile():TileEntity = tile
    def world() = if(tile == null) null else tile.worldObj
    def x = tile.xCoord
    def y = tile.yCoord
    def z = tile.zCoord
    
    def getType:String
    def bind(t:TileMultipart)
    {
        tile = t
    }
    
    def occlusionTest(npart:TMultiPart):Boolean = true
    def getSubParts:Iterable[IndexedCuboid6] = Seq()
    def getCollisionBoxes:Iterable[Cuboid6] = Seq()
    def collisionRayTrace(start: Vec3, end: Vec3): ExtendedMOP = {
      val offset = new Vector3(x, y, z)
      val boxes = getSubParts.map(c => new IndexedCuboid6(c.data, c.copy.add(offset)))
      return RayTracer.instance.rayTraceCuboids(new Vector3(start), new Vector3(end), boxes.toList,
              new BlockCoord(x, y, z), tile.blockType).asInstanceOf[ExtendedMOP]
    } 
    
    def getDrops:Iterable[ItemStack] = Seq()
    def getStrength(hit:MovingObjectPosition, player:EntityPlayer):Float = 1
    def getLightValue = 0
    
    @SideOnly(Side.CLIENT)
    def addHitEffects(hit:MovingObjectPosition, effectRenderer:EffectRenderer){}
    @SideOnly(Side.CLIENT)
    def addDestroyEffects(effectRenderer:EffectRenderer){}
    @SideOnly(Side.CLIENT)
    def renderStatic(pos:Vector3, olm:LazyLightMatrix, pass:Int){}
    @SideOnly(Side.CLIENT)
    def renderDynamic(pos:Vector3, frame:Float, pass:Int){}
    @SideOnly(Side.CLIENT)
    def drawBreaking(renderBlocks:RenderBlocks){}
    @SideOnly(Side.CLIENT)
    def drawHighlight(hit:MovingObjectPosition, player:EntityPlayer, frame:Float) = false
    
    def read(packet:MCDataInput)
    {
        readDesc(packet)
        tile.markRender()
    }
    def readDesc(packet:MCDataInput){}
    def writeDesc(packet:MCDataOutput){}
    def save(tag:NBTTagCompound){}
    def load(tag:NBTTagCompound){}
    
    /**
     * The part parameter may be null if several things have changed.
     */
    def onPartChanged(part:TMultiPart){}
    def onNeighborChanged(){}
    def onAdded() = onWorldJoin()
    def onRemoved() = onWorldSeparate()
    def onChunkLoad() = onWorldJoin()
    def onChunkUnload() = onWorldSeparate()
    def onWorldSeparate(){}
    def onWorldJoin(){}
    def onConverted() = onAdded()
    def onMoved() = onWorldJoin()
    def preRemove(){}
    
    def doesTick = true
    def update(){}
    def scheduledTick(){}
    
    def pickItem(hit:MovingObjectPosition):ItemStack = null
    def activate(player:EntityPlayer, part:MovingObjectPosition, item:ItemStack):Boolean = false
    def click(player:EntityPlayer, part:MovingObjectPosition, item:ItemStack){}
    def onEntityCollision(entity:Entity){}
    
    def sendDescUpdate() = writeDesc(tile.getWriteStream(this))
    def scheduleTick(ticks:Int) = TickScheduler.scheduleTick(this, ticks)
}
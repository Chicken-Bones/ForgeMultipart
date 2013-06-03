package codechicken.multipart

import codechicken.core.vec.Cuboid6
import codechicken.core.vec.Vector3
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MovingObjectPosition
import codechicken.core.raytracer.IndexedCuboid6
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.util.Icon
import net.minecraft.client.renderer.RenderBlocks
import codechicken.core.render.RenderUtils
import codechicken.core.render.IconTransformation
import codechicken.core.vec.BlockCoord
import net.minecraft.world.World
import net.minecraft.tileentity.TileEntity
import codechicken.core.render.CCRenderState
import codechicken.scala.JSeq
import codechicken.scala.ScalaBridge._
import codechicken.core.lighting.LazyLightMatrix
import codechicken.core.data.MCDataOutput
import codechicken.core.data.MCDataInput

abstract class TMultiPart
{
    var tile:TileMultipart = _
    
    def getTile():TileEntity = tile
    
    def getType:String
    def bind(tile:TileMultipart)
    {
        TMultiPart.this.tile = tile
    }
    
    def getSlotMask:Int = 0
    def blocksRedstone:Boolean = false//Not necessarily sure this is the best way to test this
    
    def occlusionTest(npart:TMultiPart):Boolean = true
    def getSubParts:JSeq[IndexedCuboid6] = Seq()
    def getCollisionBoxes:JSeq[Cuboid6] = Seq()
    
    def getDrops:JSeq[ItemStack] = Seq()
    def getStrength(hit:MovingObjectPosition, player:EntityPlayer):Float = 1
    def getLightValue = 0
    
    @SideOnly(Side.CLIENT)
    def addHitEffects(hit:MovingObjectPosition, effectRenderer:EffectRenderer){}
    @SideOnly(Side.CLIENT)
    def addDestroyEffects(effectRenderer:EffectRenderer){}
    @SideOnly(Side.CLIENT)
    def renderStatic(pos:Vector3, olm:LazyLightMatrix, pass:Int){}
    @SideOnly(Side.CLIENT)
    def renderDynamic(pos:Vector3, frame:Float){}
    @SideOnly(Side.CLIENT)
    def drawBreaking(renderBlocks:RenderBlocks){}
    
    def read(packet:MCDataInput) = readDesc(packet)
    def readDesc(packet:MCDataInput){}
    def writeDesc(packet:MCDataOutput){}
    def save(tag:NBTTagCompound){}
    def load(tag:NBTTagCompound){}
    
    def onWorldJoin(world:World){onPartChanged()}
    def onPartChanged(){}
    def onNeighbourChanged(){}
    
    def activate(player:EntityPlayer, part:MovingObjectPosition, item:ItemStack):Boolean = false
    def click(player:EntityPlayer, part:MovingObjectPosition, item:ItemStack){}
    def doesTick = true
    def update(){}
    def scheduledTick(){}
    def pickItem(hit:MovingObjectPosition):ItemStack = null
    
    def sendDescUpdate() = writeDesc(tile.getWriteStream(this))
    def scheduleTick(ticks:Int) = TickScheduler.scheduleTick(this, ticks)
}

trait TFacePart extends TMultiPart
{
    def solid(side:Int):Boolean = true
}
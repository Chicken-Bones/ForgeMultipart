package codechicken.microblock

import net.minecraft.nbt.NBTTagCompound
import codechicken.lib.lighting.LightMatrix
import codechicken.microblock.MicroMaterialRegistry._
import codechicken.lib.vec.BlockCoord
import org.lwjgl.opengl.GL11
import codechicken.lib.render.CCRenderState
import net.minecraft.world.World
import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Vector3
import codechicken.multipart.TIconHitEffects
import net.minecraft.util.IIcon
import net.minecraft.block.Block
import codechicken.multipart.TCuboidPart
import net.minecraft.util.MovingObjectPosition
import net.minecraft.entity.player.EntityPlayer
import codechicken.multipart.JPartialOcclusion
import codechicken.lib.render.Vertex5
import net.minecraft.item.ItemStack
import scala.collection.mutable.ListBuffer
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import codechicken.lib.render.RenderUtils
import codechicken.lib.render.IFaceRenderer
import codechicken.multipart.TSlottedPart
import scala.collection.JavaConversions._
import codechicken.lib.render.TextureUtils
import net.minecraft.entity.Entity
import net.minecraft.init.Blocks
import cpw.mods.fml.relauncher.{Side, SideOnly}
import codechicken.lib.render.BlockRenderer.BlockFace

object MicroblockRender
{
    def renderHighlight(player:EntityPlayer, hit:MovingObjectPosition, mcrClass:MicroblockClass, size:Int, material:Int)
    {
        mcrClass.placementProperties.placementGrid.render(new Vector3(hit.hitVec), hit.sideHit)

        val placement = MicroblockPlacement(player, hit, size, material, !player.capabilities.isCreativeMode, mcrClass.placementProperties)
        if(placement == null)
            return
        val pos = placement.pos
        val part = placement.part.asInstanceOf[MicroblockClient]

        GL11.glPushMatrix()
        GL11.glTranslated(pos.x+0.5, pos.y+0.5, pos.z+0.5)
        GL11.glScaled(1.002, 1.002, 1.002)
        GL11.glTranslated(-0.5, -0.5, -0.5)
        
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDepthMask(false)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.alphaOverride = 80
        CCRenderState.useNormals = true
        CCRenderState.startDrawing()
        part.render(Vector3.zero, -1)
        CCRenderState.draw()
        
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDepthMask(true)
        GL11.glPopMatrix()
    }

    val face = new BlockFace()
    def renderCuboid(pos:Vector3, mat:IMicroMaterial, pass:Int, c:Cuboid6, faces:Int) {
        CCRenderState.setModel(face)
        for(s <- 0 until 6 if (faces & 1<<s) == 0) {
            face.loadCuboidFace(c, s)
            mat.renderMicroFace(pos, pass, c)
        }
    }
}

trait MicroblockClient extends Microblock with TIconHitEffects with IMicroMaterialRender
{
    def getBrokenIcon(side:Int) = getIMaterial match {
        case null => Blocks.stone.getIcon(0, 0)
        case mat => mat.getBreakingIcon(side)
    }

    override def renderStatic(pos:Vector3, pass:Int) = {
        if(getIMaterial.canRenderInPass(pass)) {
            render(pos, pass)
            true
        }
        else
            false
    }

    def render(pos:Vector3, pass:Int)

    override def getRenderBounds = getBounds
}

abstract class Microblock(var shape:Byte = 0, var material:Int = 0) extends TCuboidPart
{
    def this(size:Int, shape:Int, material:Int) = this((size<<4|shape).toByte, material)
    
    override def getStrength(hit:MovingObjectPosition, player:EntityPlayer) = getIMaterial match {
        case null => super.getStrength(hit, player)
        case mat => mat.getStrength(player)
    }

    override def doesTick = false
    
    def getSize = shape>>4
    
    def getShape = shape&0xF

    def getMaterial = material
    
    def getIMaterial = MicroMaterialRegistry.getMaterial(material)
    
    def itemClassID:Int = -1
    
    override def getDrops =
    {
        var size = getSize
        val items = ListBuffer[ItemStack]()
        for(s <- Seq(4, 2, 1))
        {
            val m = size/s
            size-=m*s
            if(m > 0)
                items+=ItemMicroPart.create(m, s|itemClassID<<8, MicroMaterialRegistry.materialName(material))
        }
        items
    }
    
    override def pickItem(hit:MovingObjectPosition):ItemStack = 
    {
        val size = getSize
        for(s <- Seq(4, 2, 1))
            if(size%s == 0 && size/s >= 1)
                return ItemMicroPart.create(s|itemClassID<<8, MicroMaterialRegistry.materialName(material))
        return null//unreachable
    }
    
    override def writeDesc(packet:MCDataOutput)
    {
        packet.writeByte(shape)
        writeMaterialID(packet, material)
    }
    
    override def readDesc(packet:MCDataInput)
    {
        shape = packet.readByte
        material = readMaterialID(packet)
    }
    
    override def read(packet:MCDataInput)
    {
        readDesc(packet)
        tile.notifyPartChange(this)
        tile.markRender()
    }
    
    override def save(tag:NBTTagCompound)
    {
        tag.setByte("shape", shape)
        tag.setString("material", materialName(material))
    }
    
    override def load(tag:NBTTagCompound)
    {
        shape = tag.getByte("shape")
        material = materialID(tag.getString("material"))
    }
    
    def isTransparent = getIMaterial.isTransparent
    
    override def getLightValue = getIMaterial.getLightValue

    def getResistanceFactor:Float

    override def explosionResistance(entity:Entity) = getIMaterial.explosionResistance(entity)*getResistanceFactor
}

trait CommonMicroblockClient extends CommonMicroblock with MicroblockClient with TMicroOcclusionClient
{
    def render(pos:Vector3, pass:Int) {
        if(pass < 0)
            MicroblockRender.renderCuboid(pos, getIMaterial, pass, getBounds, 0)
        else
            MicroblockRender.renderCuboid(pos, getIMaterial, pass, renderBounds, renderMask)
    }
}

abstract class CommonMicroblock(shape$:Byte = 0, material$:Int = 0) extends Microblock(shape$, material$) with JPartialOcclusion with TMicroOcclusion with TSlottedPart
{
    def microClass:MicroblockClass
    
    def getType = microClass.getName
    
    def getSlot = getShape
    
    def getSlotMask = 1<<getSlot
    
    def getPartialOcclusionBoxes = Seq(getBounds)
    
    override def itemClassID = microClass.classID

    def getResistanceFactor = microClass.getResistanceFactor
}
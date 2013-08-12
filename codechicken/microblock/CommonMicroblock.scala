package codechicken.microblock

import net.minecraft.nbt.NBTTagCompound
import codechicken.lib.lighting.LightMatrix
import codechicken.lib.packet.PacketCustom
import codechicken.microblock.MicroMaterialRegistry._
import codechicken.lib.vec.BlockCoord
import org.lwjgl.opengl.GL11
import codechicken.lib.render.CCRenderState
import net.minecraft.world.World
import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Vector3
import codechicken.lib.lighting.LazyLightMatrix
import codechicken.multipart.TIconHitEffects
import net.minecraft.util.Icon
import net.minecraft.block.Block
import codechicken.multipart.TCuboidPart
import net.minecraft.util.MovingObjectPosition
import net.minecraft.entity.player.EntityPlayer
import codechicken.multipart.JPartialOcclusion
import codechicken.lib.vec.Rotation
import codechicken.multipart.TileMultipart
import codechicken.lib.render.Vertex5
import net.minecraft.item.ItemStack
import scala.collection.mutable.ListBuffer
import codechicken.multipart.TMultiPart
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import codechicken.lib.render.RenderUtils
import codechicken.lib.render.IFaceRenderer
import codechicken.multipart.TSlottedPart
import scala.collection.JavaConversions._
import codechicken.lib.render.TextureUtils

object CommonMicroblock
{
    val face = Array[Vertex5](new Vertex5, new Vertex5, new Vertex5, new Vertex5)
    
    def renderHighlight(world:World, pos:BlockCoord, part:MicroblockClient)
    {
        GL11.glPushMatrix()
        GL11.glTranslated(pos.x+0.5, pos.y+0.5, pos.z+0.5)
        GL11.glScaled(1.002, 1.002, 1.002)
        GL11.glTranslated(-0.5, -0.5, -0.5)
        
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDepthMask(false)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        
        CCRenderState.reset()
        TextureUtils.bindAtlas(0);
        CCRenderState.useNormals(true)
        CCRenderState.setBrightness(world, pos.x, pos.y, pos.z)
        CCRenderState.setAlpha(80)
        CCRenderState.useModelColours(true)
        CCRenderState.startDrawing(7)
        part.render(new Vector3(), null, MicroMaterialRegistry.getMaterial(part.material), part.getBounds, 0)
        CCRenderState.draw()
        
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDepthMask(true)
        GL11.glPopMatrix()
    }
}

trait MicroblockClient extends Microblock with TIconHitEffects
{
import CommonMicroblock._
    
    def getBrokenIcon(side:Int):Icon = 
    {
        val mat = MicroMaterialRegistry.getMaterial(material)
        if(mat != null)
            return mat.getBreakingIcon(side)
        return Block.stone.getIcon(0, 0)
    }
    
    def render(pos:Vector3, olm:LazyLightMatrix, mat:IMicroMaterial, c:Cuboid6, sideMask:Int)
    {
        renderCuboid(pos, olm, mat, c, sideMask)
    }
    
    def renderCuboid(pos:Vector3, olm:LazyLightMatrix, mat:IMicroMaterial, c:Cuboid6, sideMask:Int)
    {
        var lightMatrix:LightMatrix = null
        if(olm != null)
            lightMatrix = olm.lightMatrix
            
        RenderUtils.renderBlock(c, sideMask, new IFaceRenderer()
            {
                def renderFace(face:Array[Vertex5], side:Int) = 
                    mat.renderMicroFace(face, side, pos, lightMatrix, MicroblockClient.this)
            })
    }
}

abstract class Microblock(var shape:Byte = 0, var material:Int = 0) extends TCuboidPart
{    
    def this(size:Int, shape:Int, material:Int) = this((size<<4|shape).toByte, material)
    
    override def getStrength(hit:MovingObjectPosition, player:EntityPlayer):Float = 
    {
        val mat = MicroMaterialRegistry.getMaterial(material)
        if(mat != null)
            return mat.getStrength(player)
        return super.getStrength(hit, player)
    }
    
    override def doesTick = false
    
    def getSize = shape>>4
    
    def getShape = shape&0xF
    
    def getMaterial = material
    
    def itemClassID:Int = -1
    
    override def getDrops() =
    {
        var size = getSize
        val items = ListBuffer[ItemStack]()
        for(s <- Seq(4, 2, 1))
        {
            var m = size/s
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
        writePartID(packet, material)
    }
    
    override def readDesc(packet:MCDataInput)
    {
        shape = packet.readByte
        material = readPartID(packet)
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
    
    def isTransparent = MicroMaterialRegistry.getMaterial(material).isTransparent
    
    override def getLightValue = MicroMaterialRegistry.getMaterial(material).getLightValue
}

trait CommonMicroblockClient extends CommonMicroblock with MicroblockClient with TMicroOcclusionClient
{
    override def renderStatic(pos:Vector3, olm:LazyLightMatrix, pass:Int)
    {
        val mat = MicroMaterialRegistry.getMaterial(material)
        if(mat != null && pass == mat.getRenderPass)
            render(pos, olm, mat, renderBounds, renderMask)
    }
}

abstract class CommonMicroblock(shape$:Byte = 0, material$:Int = 0) extends Microblock(shape$, material$) with JPartialOcclusion with TMicroOcclusion with TSlottedPart
{
    def microClass():MicroblockClass
    
    def getType = microClass.getName
    
    def getSlot = getShape
    
    override def getSlotMask = 1<<getSlot
    
    def getPartialOcclusionBoxes = Seq(getBounds)
    
    override def itemClassID = microClass.classID
}
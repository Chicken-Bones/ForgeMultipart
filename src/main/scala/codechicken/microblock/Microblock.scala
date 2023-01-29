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

abstract class Microblock(var material: Int = 0) extends TCuboidPart {
  var shape: Byte = 0

  def microClass: MicroblockClass

  def getType = microClass.getName

  override def getStrength(hit: MovingObjectPosition, player: EntityPlayer) =
    getIMaterial match {
      case null => super.getStrength(hit, player)
      case mat  => mat.getStrength(player)
    }

  override def doesTick = false

  def getSize = shape >> 4

  def getShape = shape & 0xf

  def setShape(size: Int, slot: Int) = shape = (size << 4 | slot).toByte

  def getMaterial = material

  def getIMaterial = MicroMaterialRegistry.getMaterial(material)

  def itemClassID: Int

  override def getDrops = {
    var size = getSize
    val items = ListBuffer[ItemStack]()
    for (s <- Seq(4, 2, 1)) {
      val m = size / s
      size -= m * s
      if (m > 0)
        items += ItemMicroPart.create(
          m,
          s | itemClassID << 8,
          MicroMaterialRegistry.materialName(material)
        )
    }
    items
  }

  override def pickItem(hit: MovingObjectPosition): ItemStack = {
    val size = getSize
    for (s <- Seq(4, 2, 1))
      if (size % s == 0 && size / s >= 1)
        return ItemMicroPart.create(
          s | itemClassID << 8,
          MicroMaterialRegistry.materialName(material)
        )

    return null // unreachable
  }

  override def writeDesc(packet: MCDataOutput) {
    writeMaterialID(packet, material)
    packet.writeByte(shape)
  }

  override def readDesc(packet: MCDataInput) {
    shape = packet.readByte
  }

  def sendShapeUpdate() {
    getWriteStream.writeByte(shape)
  }

  override def read(packet: MCDataInput) {
    super.read(packet)
    tile.notifyPartChange(this)
  }

  override def save(tag: NBTTagCompound) {
    tag.setByte("shape", shape)
    tag.setString("material", materialName(material))
  }

  override def load(tag: NBTTagCompound) {
    shape = tag.getByte("shape")
    material = materialID(tag.getString("material"))
  }

  def isTransparent = getIMaterial.isTransparent

  override def getLightValue = getIMaterial.getLightValue

  override def explosionResistance(entity: Entity) =
    getIMaterial.explosionResistance(entity) * microClass.getResistanceFactor
}

trait MicroblockClient
    extends Microblock
    with TIconHitEffects
    with IMicroMaterialRender {
  def getBrokenIcon(side: Int) = getIMaterial match {
    case null => Blocks.stone.getIcon(0, 0)
    case mat  => mat.getBreakingIcon(side)
  }

  override def renderStatic(pos: Vector3, pass: Int) = {
    if (getIMaterial.canRenderInPass(pass)) {
      render(pos, pass)
      true
    } else
      false
  }

  def render(pos: Vector3, pass: Int)

  override def getRenderBounds = getBounds
}

trait CommonMicroblockClient
    extends CommonMicroblock
    with MicroblockClient
    with TMicroOcclusionClient {
  def render(pos: Vector3, pass: Int) {
    if (pass < 0)
      MicroblockRender.renderCuboid(pos, getIMaterial, pass, getBounds, 0)
    else
      MicroblockRender.renderCuboid(
        pos,
        getIMaterial,
        pass,
        renderBounds,
        renderMask
      )
  }
}

trait CommonMicroblock
    extends Microblock
    with JPartialOcclusion
    with TMicroOcclusion
    with TSlottedPart {
  def microClass: CommonMicroClass

  def getSlot = getShape
  def getSlotMask = 1 << getSlot
  def getPartialOcclusionBoxes = Seq(getBounds)

  override def itemClassID = microClass.getClassId
}

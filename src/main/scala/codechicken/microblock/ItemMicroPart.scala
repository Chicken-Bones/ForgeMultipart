package codechicken.microblock

import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import ItemMicroPart._
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper
import org.lwjgl.opengl.GL11
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import java.util.List
import net.minecraft.creativetab.CreativeTabs
import codechicken.lib.render.CCRenderState
import net.minecraft.client.renderer.texture.IIconRegister
import codechicken.microblock.handler.MicroblockProxy
import net.minecraft.util.{StatCollector, MovingObjectPosition}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.world.World
import codechicken.lib.raytracer.RayTracer
import codechicken.lib.vec.Vector3
import codechicken.lib.render.TextureUtils
import net.minecraft.util.MovingObjectPosition.MovingObjectType
import CommonMicroClass._

class ItemMicroPart extends Item {
  setUnlocalizedName("microblock")
  setHasSubtypes(true)

  override def getItemStackDisplayName(stack: ItemStack): String = {
    val material = getMaterial(stack)
    val mcrClass = getMicroClass(stack.getItemDamage)
    val size = stack.getItemDamage & 0xff
    if (material == null || mcrClass == null)
      return "Unnamed"

    return StatCollector.translateToLocalFormatted(
      mcrClass.getName + "." + size + ".name",
      material.getLocalizedName
    )
  }

  override def getSubItems(item: Item, tab: CreativeTabs, list$ : List[_]) {
    val list = list$.asInstanceOf[List[ItemStack]]
    for (classId <- 0 until classes.length) {
      val mcrClass = classes(classId)
      if (mcrClass != null)
        for (size <- Seq(1, 2, 4))
          MicroMaterialRegistry.getIdMap.foreach(e =>
            list.add(create(classId << 8 | size, e._1))
          )
    }
  }

  override def registerIcons(register: IIconRegister) {}

  override def onItemUse(
      item: ItemStack,
      player: EntityPlayer,
      world: World,
      x: Int,
      y: Int,
      z: Int,
      s: Int,
      hitX: Float,
      hitY: Float,
      hitZ: Float
  ): Boolean = {
    val material = getMaterialID(item)
    val mcrClass = getMicroClass(item.getItemDamage)
    val size = item.getItemDamage & 0xff
    if (material < 0 || mcrClass == null)
      return false

    val hit = RayTracer.retraceBlock(world, player, x, y, z)
    if (hit != null && hit.typeOfHit == MovingObjectType.BLOCK) {
      val placement = MicroblockPlacement(
        player,
        hit,
        size,
        material,
        !player.capabilities.isCreativeMode,
        mcrClass.placementProperties
      )
      if (placement == null)
        return false

      if (!world.isRemote) {
        placement.place(world, player, item)
        if (!player.capabilities.isCreativeMode)
          placement.consume(world, player, item)

        val sound = MicroMaterialRegistry.getMaterial(material).getSound
        if (sound != null)
          world.playSoundEffect(
            placement.pos.x + 0.5d,
            placement.pos.y + 0.5d,
            placement.pos.z + 0.5d,
            sound.func_150496_b,
            (sound.getVolume + 1.0f) / 2.0f,
            sound.getPitch * 0.8f
          )
      }

      return true
    }

    return false
  }
}

object ItemMicroPart {
  def checkTagCompound(stack: ItemStack) {
    if (!stack.hasTagCompound)
      stack.setTagCompound(new NBTTagCompound())
  }

  def create(damage: Int, material: Int): ItemStack =
    create(damage, MicroMaterialRegistry.materialName(material))

  def create(damage: Int, material: String): ItemStack =
    create(1, damage, material)

  def create(amount: Int, damage: Int, material: String): ItemStack = {
    val stack = new ItemStack(MicroblockProxy.itemMicro, amount, damage)
    checkTagCompound(stack)
    stack.getTagCompound.setString("mat", material)
    return stack
  }

  def getMaterial(stack: ItemStack): IMicroMaterial = {
    checkTagCompound(stack)
    if (!stack.getTagCompound.hasKey("mat"))
      return null

    return MicroMaterialRegistry.getMaterial(
      stack.getTagCompound.getString("mat")
    )
  }

  def getMaterialID(stack: ItemStack): Int = {
    checkTagCompound(stack)
    if (!stack.getTagCompound.hasKey("mat"))
      return 0

    return MicroMaterialRegistry.materialID(
      stack.getTagCompound.getString("mat")
    )
  }
}

object ItemMicroPartRenderer extends IItemRenderer {
  def handleRenderType(item: ItemStack, t: ItemRenderType) = true

  def shouldUseRenderHelper(
      t: ItemRenderType,
      item: ItemStack,
      helper: ItemRendererHelper
  ) = true

  def renderItem(t: ItemRenderType, item: ItemStack, data: Object*) {
    val material = getMaterial(item)
    val mcrClass = getMicroClass(item.getItemDamage)
    val size = item.getItemDamage & 0xff
    if (material == null || mcrClass == null)
      return

    GL11.glPushMatrix()
    if (t == ItemRenderType.ENTITY)
      GL11.glScaled(0.5, 0.5, 0.5)
    if (t == ItemRenderType.INVENTORY || t == ItemRenderType.ENTITY)
      GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

    TextureUtils.bindAtlas(0)
    CCRenderState.reset()
    CCRenderState.useNormals = true
    CCRenderState.pullLightmap()
    CCRenderState.startDrawing()
    val part =
      mcrClass.create(true, getMaterialID(item)).asInstanceOf[MicroblockClient]
    part.setShape(size, mcrClass.itemSlot)
    part.render(new Vector3(0.5, 0.5, 0.5).subtract(part.getBounds.center), -1)
    CCRenderState.draw()
    GL11.glPopMatrix()
  }

  def renderHighlight(
      player: EntityPlayer,
      stack: ItemStack,
      hit: MovingObjectPosition
  ): Boolean = {
    val material = getMaterialID(stack)
    val mcrClass = getMicroClass(stack.getItemDamage)
    val size = stack.getItemDamage & 0xff
    if (material < 0 || mcrClass == null)
      return false

    return MicroMaterialRegistry.renderHighlight(
      player,
      hit,
      mcrClass,
      size,
      material
    )
  }
}

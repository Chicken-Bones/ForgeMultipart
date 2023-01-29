package codechicken.microblock

import codechicken.multipart.IDWriter
import codechicken.lib.packet.PacketCustom
import scala.collection.mutable.HashMap
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import codechicken.multipart.MultiPartRegistry
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.lib.vec.{Cuboid6, Vector3}
import net.minecraft.util.IIcon
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import scala.collection.mutable.ListBuffer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.entity.Entity
import scala.collection.JavaConversions._
import java.util.{Iterator => JIterator}
import net.minecraft.block.Block.SoundType

object MicroMaterialRegistry {

  /** Interface for defining a micro material
    */
  trait IMicroMaterial {

    /** The icon to be used for breaking particles on side
      */
    @SideOnly(Side.CLIENT)
    def getBreakingIcon(side: Int): IIcon

    /** Callback to load icons from the underlying block/etc
      */
    @SideOnly(Side.CLIENT)
    def loadIcons() {}

    /** This function must configure and use the CCRenderState pipeline to draw
      * from the current vertex source. CCRenderState.lightMatrix will be set.
      * @param pass
      *   The current render pass, -1 for inventory rendering
      * @param bounds
      *   The cuboid bounds of the face being rendered
      */
    @SideOnly(Side.CLIENT)
    def renderMicroFace(pos: Vector3, pass: Int, bounds: Cuboid6)

    /** Get the render pass for which this material renders in.
      */
    @SideOnly(Side.CLIENT)
    def canRenderInPass(pass: Int) = pass == 0

    /** Return true if this material is not opaque (glass, ice).
      */
    def isTransparent: Boolean

    /** Return the light level emitted by this material (glowstone)
      */
    def getLightValue: Int

    /** Return the strength of this material
      */
    def getStrength(player: EntityPlayer): Float

    /** Return the localised name of this material (normally the block name)
      */
    def getLocalizedName: String

    /** Get the item that this material is cut from (full block -> slabs)
      */
    def getItem: ItemStack

    /** Get the strength of saw requried to cut this material
      */
    def getCutterStrength: Int

    /** Get the breaking/walking sound
      */
    def getSound: SoundType

    /** Return true if this material is solid and opaque (can run wires on etc)
      */
    def isSolid = !isTransparent

    /** Get the explosion resistance of this part to an explosion caused by
      * entity
      */
    def explosionResistance(entity: Entity): Float
  }

  /** Interface for overriding the default micro placement highlight handler to
    * show the effect of placement on a certain block/part
    */
  trait IMicroHighlightRenderer {

    /** Return true if a custom highlight was rendered and the default should be
      * skipped
      */
    def renderHighlight(
        player: EntityPlayer,
        hit: MovingObjectPosition,
        mcrClass: CommonMicroClass,
        size: Int,
        material: Int
    ): Boolean
  }

  private val typeMap = HashMap[String, IMicroMaterial]()
  private val nameMap = HashMap[String, Int]()
  private var idMap: Array[(String, IMicroMaterial)] = _
  private val idWriter = new IDWriter

  private val highlightRenderers = ListBuffer[IMicroHighlightRenderer]()
  private var maxCuttingStrength: Int = _

  private val remap = HashMap[String, String]()

  /** Register a micro material with unique identifier name
    */
  def registerMaterial(material: IMicroMaterial, name: String) {
    if (MultiPartRegistry.loaded)
      throw new IllegalStateException(
        "You must register your materials in the init methods."
      )

    if (typeMap.contains(name)) {
      logger.error("Material with id " + name + " is already registered.")
      return
    }

    typeMap.put(name, material)
  }

  /** Replace a micro material with unique identifier name
    */
  def replaceMaterial(material: IMicroMaterial, name: String) {
    if (MultiPartRegistry.loaded)
      throw new IllegalStateException(
        "You must register your materials in the init methods."
      )

    if (typeMap.remove(name).isEmpty)
      logger.error("Material with id " + name + " was not registered.")

    logger.debug("Replaced micro material: " + name)

    typeMap.put(name, material)
  }

  /** Registers a highlight renderer
    */
  def registerHighlightRenderer(handler: IMicroHighlightRenderer) {
    highlightRenderers += handler
  }

  def remapName(oldName: String, newName: String): Unit =
    remap.put(oldName, newName)

  private[microblock] def setupIDMap() {
    idMap = typeMap.toList.sortBy(_._1).toArray
    idWriter.setMax(idMap.length)
    nameMap.clear()
    for (i <- 0 until idMap.length)
      nameMap.put(idMap(i)._1, i)
  }

  private[microblock] def calcMaxCuttingStrength() {
    val it = Item.itemRegistry.iterator.asInstanceOf[JIterator[Item]]
    maxCuttingStrength = it.flatMap {
      case saw: Saw => Some(saw.getMaxCuttingStrength)
      case _        => None
    }.max
  }

  private[microblock] def loadIcons() {
    if (idMap != null)
      idMap.foreach(e => e._2.loadIcons())
  }

  def getMaxCuttingStrength = maxCuttingStrength

  def writeIDMap(packet: PacketCustom) {
    packet.writeInt(idMap.size)
    idMap.foreach(e => packet.writeString(e._1))
  }

  def readIDMap(packet: PacketCustom): Seq[String] = {
    val k = packet.readInt()
    idWriter.setMax(k)
    idMap = new Array(k)
    nameMap.clear()
    val missing = ListBuffer[String]()
    for (i <- 0 until k) {
      val s = packet.readString()
      val v = typeMap.get(s)
      if (v.isEmpty)
        missing += s
      else {
        idMap(i) = (s, v.get)
        nameMap.put(s, i)
      }
    }
    return missing
  }

  def writeMaterialID(data: MCDataOutput, id: Int) {
    idWriter.write(data, id)
  }

  def readMaterialID(data: MCDataInput) = idWriter.read(data)

  def materialName(id: Int) = idMap(id)._1

  def materialID(name: String) =
    nameMap.get(remap.getOrElse(name, name)) match {
      case Some(v) => v
      case None =>
        logger.error("Missing mapping for part with ID: " + name)
        0
    }

  def getMaterial(name: String) =
    typeMap.getOrElse(remap.getOrElse(name, name), null)

  def getMaterial(id: Int) = idMap(id)._2

  def getIdMap = idMap

  def renderHighlight(
      player: EntityPlayer,
      hit: MovingObjectPosition,
      mcrClass: CommonMicroClass,
      size: Int,
      material: Int
  ): Boolean = {
    val overridden = highlightRenderers.find(
      _.renderHighlight(player, hit, mcrClass, size, material)
    )
    if (overridden.isDefined)
      return true

    MicroblockRender.renderHighlight(player, hit, mcrClass, size, material)
    return true
  }
}

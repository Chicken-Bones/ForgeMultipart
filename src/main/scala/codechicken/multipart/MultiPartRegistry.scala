package codechicken.multipart

import scala.collection.mutable.{Map => MMap}
import codechicken.lib.packet.PacketCustom
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import net.minecraft.world.World
import codechicken.lib.vec.BlockCoord
import scala.collection.mutable.ListBuffer
import cpw.mods.fml.common.ModContainer
import cpw.mods.fml.common.Loader
import net.minecraft.block.Block
import java.lang.Iterable
import com.google.common.collect.ArrayListMultimap
import scala.collection.JavaConversions._
import net.minecraft.nbt.NBTTagCompound

/** This class handles the registration and internal ID mapping of all multipart
  * classes.
  */
object MultiPartRegistry {

  /** Interface to be registered for constructing parts. Every instance of every
    * multipart is constructed from an implementor of this.
    */
  @Deprecated
  trait IPartFactory {

    /** Create a new instance of the part with the specified type name
      * identifier
      * @param client
      *   If the part instance is for the client or the server
      */
    def createPart(name: String, client: Boolean): TMultiPart
  }

  /** Will replace IPartFactory in 1.8
    */
  trait IPartFactory2 {

    /** Create a new server instance of the part with the specified type name
      * identifier
      * @param nbt
      *   The tag compound that will be passed to part.load, can be used to
      *   change the class of part returned
      */
    def createPart(name: String, nbt: NBTTagCompound): TMultiPart

    /** Create a new client instance of the part with the specified type name
      * identifier
      * @param packet
      *   The packet that will be passed to part.readDesc, can be used to change
      *   the class of part returned
      */
    def createPart(name: String, packet: MCDataInput): TMultiPart
  }

  /** An interface for converting existing blocks/tile entities to multipart
    * versions.
    */
  trait IPartConverter {

    /** Return true if this converter can handle the specific blockID (may or
      * may not actually convert the block)
      */
    def blockTypes: Iterable[Block]

    /** Return a multipart version of the block at pos in world. Return null if
      * no conversion is possible.
      */
    def convert(world: World, pos: BlockCoord): TMultiPart
  }

  private val typeMap = MMap[String, IPartFactory2]()
  private val nameMap = MMap[String, Int]()
  private var idMap: Array[(String, IPartFactory2)] = _
  private val idWriter = new IDWriter
  private val converters = ArrayListMultimap.create[Block, IPartConverter]()
  private val containers = MMap[String, ModContainer]()

  /** The state of the registry. 0 = no parts, 1 = registering, 2 = registered
    */
  private var state: Int = 0

  /** Register a part factory with an array of types it is capable of
    * instantiating. Must be called before postInit
    * @deprecated
    *   Use IPartFactory2
    */
  @Deprecated
  def registerParts(partFactory: IPartFactory, types: Array[String]) {
    registerParts(partFactory.createPart _, types: _*)
  }

  /** Scala function version of registerParts
    * @deprecated
    *   Use IPartFactory2
    */
  @Deprecated
  def registerParts(
      partFactory: (String, Boolean) => TMultiPart,
      types: String*
  ) {
    registerParts(
      new IPartFactory2 {
        override def createPart(name: String, packet: MCDataInput) =
          partFactory(name, true)
        override def createPart(name: String, nbt: NBTTagCompound) =
          partFactory(name, false)
      },
      types: _*
    )
  }

  /** Register a part factory with an array of types it is capable of
    * instantiating. Must be called before postInit
    */
  def registerParts(partFactory: IPartFactory2, types: Array[String]) {
    registerParts(partFactory, types: _*)
  }

  /** Scala va-args version of registerParts
    */
  def registerParts(partFactory: IPartFactory2, types: String*) {
    if (loaded)
      throw new IllegalStateException(
        "Parts must be registered in the init methods."
      )
    state = 1

    val container = Loader.instance.activeModContainer
    if (container == null)
      throw new IllegalStateException(
        "Parts must be registered during the initialization phase of a mod container"
      )

    types.foreach { s =>
      if (typeMap.contains(s))
        throw new IllegalStateException(
          "Part with id " + s + " is already registered."
        )

      typeMap.put(s, partFactory)
      containers.put(s, container)
    }
  }

  /** Register a part converter instance
    */
  def registerConverter(c: IPartConverter) {
    c.blockTypes.foreach(converters.put(_, c))
  }

  private[multipart] def beforeServerStart() {
    idMap = typeMap.toList.sortBy(_._1).toArray
    idWriter.setMax(idMap.length)
    nameMap.clear()
    for (i <- 0 until idMap.length)
      nameMap.put(idMap(i)._1, i)
  }

  private[multipart] def writeIDMap(packet: PacketCustom) {
    packet.writeInt(idMap.length)
    idMap.foreach(e => packet.writeString(e._1))
  }

  private[multipart] def readIDMap(packet: PacketCustom): Seq[String] = {
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

  /** Return true if any multiparts have been registered
    */
  private[multipart] def required = state > 0

  /** Return true if no more parts can be registered
    */
  def loaded = state == 2

  private[multipart] def postInit() {
    state = 2
  }

  /** Writes the id of part to data
    */
  def writePartID(data: MCDataOutput, part: TMultiPart) {
    idWriter.write(data, nameMap.get(part.getType).get)
  }

  /** Uses instantiators to creat a new part from the id read from data
    */
  def readPart(data: MCDataInput) = {
    val e = idMap(idWriter.read(data))
    e._2.createPart(e._1, data)
  }

  /** Uses instantiators to creat a new part from the a tag compound
    */
  def loadPart(name: String, nbt: NBTTagCompound) = typeMap.get(name) match {
    case Some(factory) => factory.createPart(name, nbt)
    case None =>
      logger.error("Missing mapping for part with ID: " + name)
      null
  }

  /** Uses instantiators to create a new part with specified identifier on side
    * @deprecated
    *   currently calls the nbt/packet version with a null parameter, use
    *   readPart or loadPart instead
    */
  @Deprecated
  def createPart(name: String, client: Boolean) = typeMap.get(name) match {
    case Some(factory) =>
      if (client) factory.createPart(name, null: MCDataInput)
      else factory.createPart(name, null: NBTTagCompound)
    case None =>
      logger.error("Missing mapping for part with ID: " + name)
      null
  }

  /** Calls converters to create a multipart version of the block at pos
    */
  def convertBlock(world: World, pos: BlockCoord, block: Block): TMultiPart = {
    for (c <- converters.get(block)) {
      val ret = c.convert(world, pos)
      if (ret != null)
        return ret
    }
    return null
  }

  def getModContainer(name: String) = containers(name)
}

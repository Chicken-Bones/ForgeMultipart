package codechicken.multipart

import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import codechicken.lib.vec.BlockCoord
import codechicken.multipart.handler.MultipartProxy
import codechicken.lib.packet.PacketCustom
import net.minecraft.network.play.server.S23PacketBlockChange
import java.util.BitSet
import scala.collection.mutable.Map
import codechicken.multipart.asm.{ScratchBitSet, MultipartMixinFactory}
import codechicken.multipart.asm.ASMImplicits._

/** This class manages the dynamic construction and allocation of container
  * TileMultipart instances.
  *
  * Classes that extend TileMultipart, adding tile centric logic, optimisations
  * or interfaces, can be registered to a marker interface on a part instance.
  * When a part is added to the tile that implements the certain marker
  * interface, the container tile will be replaced with a class that includes
  * the functionality from the corresponding mixin class.
  *
  * Classes are generated in a similar fashion to the way scala traits are
  * compiled. To see the output, simply enable the config option and look in the
  * asm/multipart folder of you .minecraft directory.
  *
  * There are several mixin traits that come with the API included in the
  * scalatraits package. TPartialOcclusionTile is defined as class instead of
  * trait to give an example for Java programmers.
  */
object MultipartGenerator extends ScratchBitSet {
  private val tileTraitMap = Map[Class[_], BitSet]()
  private val interfaceTraitMap_c = Map[String, String]()
  private val interfaceTraitMap_s = Map[String, String]()
  private val partTraitMap_c = Map[Class[_], BitSet]()
  private val partTraitMap_s = Map[Class[_], BitSet]()
  private val clientTraitId =
    MultipartMixinFactory.registerTrait(classOf[TileMultipartClient])

  private def partTraitMap(client: Boolean) =
    if (client) partTraitMap_c else partTraitMap_s
  private def interfaceTraitMap(client: Boolean) =
    if (client) interfaceTraitMap_c else interfaceTraitMap_s

  private def traitsForPart(part: TMultiPart, client: Boolean) =
    partTraitMap(client).getOrElseUpdate(
      part.getClass, {
        def heirachy(clazz: Class[_]): Seq[Class[_]] = {
          var superClasses: Seq[Class[_]] =
            clazz.getInterfaces.flatMap(c => heirachy(c)) :+ clazz
          if (clazz.getSuperclass != null)
            superClasses = superClasses ++ heirachy(clazz.getSuperclass)
          return superClasses
        }

        val map = interfaceTraitMap(client)
        val traits =
          heirachy(part.getClass).flatMap(c => map.get(c.nodeName)).distinct

        val bitset = new BitSet
        traits.map(MultipartMixinFactory.getId).foreach(bitset.set)
        bitset
      }
    )

  private def setTraits(part: TMultiPart, client: Boolean): BitSet =
    setTraits(Seq(part), client)

  private def setTraits(parts: Iterable[TMultiPart], client: Boolean) = {
    val bitset = freshBitSet
    parts.foreach(p => bitset.or(traitsForPart(p, client)))
    if (client) bitset.set(clientTraitId)
    bitset
  }

  /** Check if part adds any new interfaces to tile, if so, replace tile with a
    * new copy and call tile.addPart(part) returns true if tile was replaced
    */
  private[multipart] def addPart(
      world: World,
      pos: BlockCoord,
      part: TMultiPart
  ): TileMultipart = {
    val (tile, converted) = TileMultipart.getOrConvertTile2(world, pos)
    val bitset = setTraits(part, world.isRemote)

    var ntile = tile
    if (ntile != null) {
      if (converted) { // perform client conversion
        ntile.partList(0).invalidateConvertedTile()
        world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block, 0, 0)
        silentAddTile(world, pos, ntile)
        PacketCustom.sendToChunk(
          new S23PacketBlockChange(pos.x, pos.y, pos.z, world),
          world,
          pos.x >> 4,
          pos.z >> 4
        )
        ntile.partList(0).onConverted()
        ntile.writeAddPart(ntile.partList(0))
      }

      val tileTraits = tileTraitMap(tile.getClass)
      bitset.andNot(tileTraits)
      if (!bitset.isEmpty) {
        bitset.or(tileTraits)
        ntile = MultipartMixinFactory.construct(bitset)
        tile.setValid(false)
        silentAddTile(world, pos, ntile)
        ntile.from(tile)
      }
    } else {
      world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block, 0, 0)
      ntile = MultipartMixinFactory.construct(bitset)
      silentAddTile(world, pos, ntile)
    }
    ntile.addPart_impl(part)
    return ntile
  }

  /** Adds a tile entity to the world without notifying neighbor blocks or
    * adding it to the tick list
    */
  def silentAddTile(world: World, pos: BlockCoord, tile: TileEntity) {
    val chunk = world.getChunkFromBlockCoords(pos.x, pos.z)
    if (chunk != null)
      chunk.func_150812_a(pos.x & 15, pos.y, pos.z & 15, tile)
  }

  /** Check if tile satisfies all the interfaces required by parts. If not,
    * return a new generated copy of tile
    */
  private[multipart] def generateCompositeTile(
      tile: TileEntity,
      parts: Iterable[TMultiPart],
      client: Boolean
  ) = {
    val bitset = setTraits(parts, client)
    if (
      tile != null && tile
        .isInstanceOf[TileMultipart] && bitset == tileTraitMap(tile.getClass)
    )
      tile.asInstanceOf[TileMultipart]
    else
      MultipartMixinFactory.construct(bitset)
  }

  /** Check if there are any redundant interfaces on tile, if so, replace tile
    * with new copy
    */
  private[multipart] def partRemoved(tile: TileMultipart) = {
    val ntile =
      generateCompositeTile(tile, tile.partList, tile.getWorldObj.isRemote)
    if (ntile != tile) {
      tile.setValid(false)
      silentAddTile(tile.getWorldObj, new BlockCoord(tile), ntile)
      ntile.from(tile)
      ntile.notifyTileChange()
    }
    ntile
  }

  /** register s_trait to be applied to tiles containing parts implementing
    * s_interface
    */
  def registerTrait(s_interface: String, s_trait: String): Unit =
    registerTrait(s_interface, s_trait, s_trait)

  /** register traits to be applied to tiles containing parts implementing
    * s_interface c_trait for client worlds (may be null) s_trait for server
    * worlds (may be null)
    */
  def registerTrait(
      s_interface$ : String,
      c_trait$ : String,
      s_trait$ : String
  ) {
    val s_interface = nodeName(s_interface$)
    val c_trait = nodeName(c_trait$)
    val s_trait = nodeName(s_trait$)

    def registerSide(map: Map[String, String], traitName: String) = if (
      traitName != null
    ) {
      if (map.contains(s_interface))
        logger.error("Trait already registered for " + s_interface)
      else {
        map.put(s_interface, traitName)
        MultipartMixinFactory.registerTrait(traitName)
      }
    }
    registerSide(interfaceTraitMap_c, c_trait)
    registerSide(interfaceTraitMap_s, s_trait)
  }

  def registerPassThroughInterface(s_interface: String): Unit =
    registerPassThroughInterface(s_interface, true, true)

  /** A passthrough interface, is an interface to be implemented on the
    * container tile instance, for which all calls are passed directly to the
    * single implementing part. Registering a passthrough interface is
    * equivalent to defining a mixin class as follows.
    *   1. field 'impl' which contains the reference to the corresponding part
    *      2. occlusionTest is overriden to prevent more than one part with
    *      s_interface existing in the block space 3. implementing s_interface
    *      and passing all calls directly to the part instance.
    *
    * This allows compatibility with APIs that expect interfaces on the tile
    * entity.
    */
  def registerPassThroughInterface(
      s_interface: String,
      client: Boolean,
      server: Boolean
  ) {
    val tType = MultipartMixinFactory.generatePassThroughTrait(s_interface)
    if (tType == null)
      return

    registerTrait(
      s_interface,
      if (client) tType else null,
      if (server) tType else null
    )
  }

  private[multipart] def registerTileClass(
      clazz: Class[_ <: TileMultipart],
      traits: BitSet
  ) {
    tileTraitMap.put(clazz, traits.copy)
    MultipartProxy.onTileClassBuilt(clazz)
  }
}

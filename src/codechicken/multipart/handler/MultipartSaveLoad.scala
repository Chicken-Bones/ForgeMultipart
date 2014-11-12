package codechicken.multipart.handler

import net.minecraft.tileentity.TileEntity
import net.minecraft.nbt.NBTTagCompound
import java.util.Map
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.ChunkPosition
import codechicken.multipart.{MultipartHelper, TileMultipart}
import codechicken.lib.asm.ObfMapping
import net.minecraft.world.World
import scala.collection.mutable
import codechicken.multipart.MultipartHelper.IPartTileConverter
import scala.collection.JavaConversions._

/**
 * Hack due to lack of TileEntityLoadEvent in forge
 */
object MultipartSaveLoad
{
    val converters = mutable.MutableList[IPartTileConverter[_]]()
    var loadingWorld: World = _

    class TileNBTContainer extends TileEntity
    {
        var tag: NBTTagCompound = _

        override def readFromNBT(t: NBTTagCompound) {
            super.readFromNBT(t)
            tag = t
        }
    }

    def hookLoader() {
        val field = classOf[TileEntity].getDeclaredField(
            new ObfMapping("net/minecraft/tileentity/TileEntity", "field_145855_i", "Ljava/util/Map;")
                .toRuntime.s_name)
        field.setAccessible(true)
        val map = field.get(null).asInstanceOf[Map[String, Class[_ <: TileEntity]]]
        map.put("savedMultipart", classOf[TileNBTContainer])
    }

    private val classToNameMap = getClassToNameMap

    def registerTileClass(t: Class[_ <: TileEntity]) {
        classToNameMap.put(t, "savedMultipart")
    }

    def getClassToNameMap = {
        val field = classOf[TileEntity].getDeclaredField(
            new ObfMapping("net/minecraft/tileentity/TileEntity", "field_145853_j", "Ljava/util/Map;")
                .toRuntime.s_name)
        field.setAccessible(true)
        field.get(null).asInstanceOf[Map[Class[_ <: TileEntity], String]]
    }

    def tileSwapHook(tile: TileEntity) = {
        val swap = tile match {
            case t:TileNBTContainer if t.tag.getString("id") == "savedMultipart" =>
                TileMultipart.createFromNBT(t.tag)
            case t => converters.find(_.canConvert(t)) match {
                case Some(c) =>
                    val parts = c.convert(t)
                    if(!parts.isEmpty)
                        MultipartHelper.createTileFromParts(parts)
                    else
                        t
                case _ =>
                    t
            }
        }
        swap
    }

    def trackTileEntityMap(c: Chunk) = {
        var result = c.chunkTileEntityMap
        if (!result.isInstanceOf[TileTrackerMap]) {
            val newMap = new TileTrackerMap(c)
            result = newMap
        }
        result
    }

    def trackChunk(c: Chunk) {
        c.chunkTileEntityMap = trackTileEntityMap(c)
    }
}

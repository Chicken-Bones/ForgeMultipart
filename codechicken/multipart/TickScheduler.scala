package codechicken.multipart

import codechicken.core.world.WorldExtensionInstantiator
import codechicken.core.world.WorldExtension
import codechicken.core.world.ChunkExtension
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.World
import net.minecraft.nbt.NBTTagCompound
import scala.collection.mutable.ListBuffer
import net.minecraft.nbt.NBTTagList
import codechicken.core.vec.BlockCoord
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.world.ChunkPosition
import scala.collection.mutable.HashSet

object TickScheduler extends WorldExtensionInstantiator
{
    class PartTickEntry(val part:TMultiPart, var ticks:Int)
    
    class WorldTickScheduler(world:World) extends WorldExtension(world)
    {
        var tickChunks = HashSet[ChunkTickScheduler]()
        
        def scheduleTick(part:TMultiPart, ticks:Int)
        {
            getChunkExtension(part.tile.xCoord>>4, part.tile.zCoord>>4).asInstanceOf[ChunkTickScheduler].scheduleTick(part, ticks)
        }
        
        override def preTick()
        {
            if(!tickChunks.isEmpty)
                tickChunks = tickChunks.filter(_.processTicks())
        }
    }
    
    def createWorldExtension(world:World) = new WorldTickScheduler(world)
    
    class ChunkTickScheduler(chunk:Chunk, world:WorldTickScheduler) extends ChunkExtension(chunk, world)
    {
        import codechicken.multipart.handler.MultipartProxy._
        
        var tickList = ListBuffer[PartTickEntry]()
        
        def scheduleTick(part:TMultiPart, ticks:Int)
        {
            tickList+=new PartTickEntry(part, ticks)
            if(tickList.size == 1)
                world.tickChunks+=this
        }
        
        def processTicks():Boolean =
        {
            tickList = tickList.filter(processTick(_))
            return !tickList.isEmpty
        }
        
        def processTick(e:PartTickEntry):Boolean =
        {
            e.ticks-=1
            if(e.ticks == 0)
            {
                if(e.part.tile != null)
                    e.part.scheduledTick()
                return false
            }
            return true
        }
        
        override def saveData(data:NBTTagCompound)
        {
            val tagList = new NBTTagList
            tickList.foreach(e => {
                val part = e.part
                if(part.tile != null)
                {
                    val tag = new NBTTagCompound
                    tag.setShort("pos", indexInChunk(new BlockCoord(part.tile)).toShort)
                    tag.setByte("i", part.tile.partList.indexOf(part).toByte)
                    tag.setInteger("ticks", e.ticks)
                    tagList.appendTag(tag)
                }
            })
            if(tagList.tagCount > 0)
                data.setTag("multipartTicks", tagList)
        }
        
        override def loadData(data:NBTTagCompound)
        {
            tickList.clear()
            if(!data.hasKey("multipartTicks"))
                return
            
            val tagList = data.getTagList("multipartTicks")
            val cc = new ChunkCoordIntPair(0, 0)
            for(i <- 0 until tagList.tagCount)
            {
                val tag = tagList.tagAt(i).asInstanceOf[NBTTagCompound]
                val pos = indexInChunk(cc, tag.getShort("pos"))
                val tile = chunk.chunkTileEntityMap.get(new ChunkPosition(pos.x, pos.y, pos.z))
                if(tile.isInstanceOf[TileMultipart])
                    tickList+=new PartTickEntry(tile.asInstanceOf[TileMultipart].partList(tag.getByte("i")), tag.getInteger("ticks"))
            }
        }
        
        override def load()
        {
            if(!tickList.isEmpty)
                world.tickChunks+=this
        }
        
        override def unload()
        {
            if(!tickList.isEmpty)
                world.tickChunks-=this
        }
    }
    
    def createChunkExtension(chunk:Chunk, world:WorldExtension) = new ChunkTickScheduler(chunk, world.asInstanceOf[WorldTickScheduler])
    
    def scheduleTick(part:TMultiPart, ticks:Int)
    {
        getExtension(part.tile.worldObj).asInstanceOf[WorldTickScheduler].scheduleTick(part, ticks)
    }
}
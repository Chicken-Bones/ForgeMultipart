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
    class PartTickEntry(val part:TMultiPart, var time:Long, var random:Boolean)
    {
        def this(part:TMultiPart, ticks:Int) = this(part, ticks, false)
    }
    
    private class WorldTickScheduler(world$:World) extends WorldExtension(world$)
    {
        var tickChunks = HashSet[ChunkTickScheduler]()
        private var processing = false
        private val pending = ListBuffer[PartTickEntry]()
        
        def scheduleTick(part:TMultiPart, ticks:Int, random:Boolean)
        {
            if(processing)
                pending+=new PartTickEntry(part, world.getWorldTime()+ticks, random)
            else
                _scheduleTick(part, world.getWorldTime()+ticks, random)
        }
        
        def _scheduleTick(part:TMultiPart, time:Long, random:Boolean)
        {
            if(part.tile != null)
                getChunkExtension(part.tile.xCoord>>4, part.tile.zCoord>>4)
                    .asInstanceOf[ChunkTickScheduler].scheduleTick(part, time, random)
        }
        
        override def preTick()
        {
            processing = true
        }
        
        override def postTick()
        {
            if(!tickChunks.isEmpty)
                tickChunks = tickChunks.filter(_.processTicks())
            
            processing = false
            pending.foreach(e => _scheduleTick(e.part, e.time, e.random))
            pending.clear()
        }
    }
    
    def createWorldExtension(world:World):WorldExtension = new WorldTickScheduler(world)
    
    private class ChunkTickScheduler(chunk$:Chunk, world:WorldTickScheduler) extends ChunkExtension(chunk$, world)
    {
        import codechicken.multipart.handler.MultipartProxy._
        
        var tickList = ListBuffer[PartTickEntry]()
        
        def scheduleTick(part:TMultiPart, time:Long, random:Boolean)
        {
            val it = tickList.iterator
            while(it.hasNext)
            {
                val e = it.next
                if(e.part == part)
                {
                    if(e.random && !random)
                    {
                        e.time = time
                        e.random = random
                    }
                    return
                }
            }
            tickList+=new PartTickEntry(part, time, random)
            if(tickList.size == 1)
                world.tickChunks+=this
        }
        
        def nextRandomTick = world.world.rand.nextInt(800)+800
        
        def processTicks():Boolean =
        {
            tickList = tickList.filter(processTick(_))
            return !tickList.isEmpty
        }
        
        def processTick(e:PartTickEntry):Boolean =
        {
            val time = world.world.getWorldTime
            if(e.time <= time)
            {
                if(e.part.tile != null)
                {
                    if(e.random && e.part.isInstanceOf[IRandomUpdateTick])
                        e.part.asInstanceOf[IRandomUpdateTick].randomUpdate()
                    else
                        e.part.scheduledTick()
                    
                    if(e.part.isInstanceOf[IRandomUpdateTick])
                    {
                        e.time = time+nextRandomTick
                        e.random = true
                        return true
                    }
                }
                return false
            }
            return true
        }
        
        override def saveData(data:NBTTagCompound)
        {
            val tagList = new NBTTagList
            tickList.foreach{e =>
                val part = e.part
                if(part.tile != null && !e.random)
                {
                    val tag = new NBTTagCompound
                    tag.setShort("pos", indexInChunk(new BlockCoord(part.tile)).toShort)
                    tag.setByte("i", part.tile.partList.indexOf(part).toByte)
                    tag.setLong("time", e.time)
                    tagList.appendTag(tag)
                }
            }
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
                    tickList+=new PartTickEntry(tile.asInstanceOf[TileMultipart].partList(tag.getByte("i")), tag.getLong("time"), false)
            }
        }
        
        override def load()
        {
            val it = chunk.chunkTileEntityMap.values.iterator
            while(it.hasNext)
            {
                val t = it.next
                if(t.isInstanceOf[TileMultipart])
                {
                    val tmp = t.asInstanceOf[TileMultipart]
                    tmp.onChunkLoad()
                    tmp.partList.foreach(p =>
                        if(p.isInstanceOf[IRandomUpdateTick])
                            world.scheduleTick(p, nextRandomTick, true))
                }
            }
            
            if(!tickList.isEmpty)
                world.tickChunks+=this
        }
        
        override def unload()
        {
            if(!tickList.isEmpty)
                world.tickChunks-=this
        }
    }
    
    def createChunkExtension(chunk:Chunk, world:WorldExtension):ChunkExtension = new ChunkTickScheduler(chunk, world.asInstanceOf[WorldTickScheduler])
    
    def scheduleTick(part:TMultiPart, ticks:Int)
    {
        getExtension(part.tile.worldObj).asInstanceOf[WorldTickScheduler].scheduleTick(part, ticks, false)
    }
}
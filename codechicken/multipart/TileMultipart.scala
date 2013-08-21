package codechicken.multipart

import net.minecraft.tileentity.TileEntity
import scala.collection.mutable.ListBuffer
import net.minecraft.network.packet.Packet
import codechicken.lib.packet.PacketCustom
import codechicken.multipart.handler.MultipartCPH
import codechicken.lib.vec.BlockCoord
import net.minecraft.world.World
import java.util.List
import scala.collection.JavaConverters._
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.DataInputStream
import codechicken.lib.data.MCDataInputStream
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataOutputStream
import net.minecraft.client.multiplayer.NetClientHandler
import codechicken.lib.vec.Cuboid6
import scala.collection.mutable.HashSet
import codechicken.multipart.handler.MultipartProxy
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import codechicken.lib.vec.Vector3
import net.minecraft.nbt.NBTTagList
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.util.MovingObjectPosition
import scala.collection.mutable.ArrayBuffer
import java.util.Random
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import scala.collection.mutable.Queue
import codechicken.multipart.handler.MultipartSPH
import codechicken.lib.lighting.LazyLightMatrix
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.Entity
import scala.collection.JavaConversions._
import java.util.Collection

class TileMultipart extends TileEntity
{
    var partList = ArrayBuffer[TMultiPart]()
    
    private var doesTick = false
    
    private[multipart] def from(that:TileMultipart)
    {
        copyFrom(that)
        loadFrom(that)
    }
    
    /**
     * This method should be used for copying all the data from the fields in that container tile. 
     * This method will be automatically generated on java tile traits with fields if it is not overridden.
     */
    def copyFrom(that:TileMultipart)
    {
        partList = that.partList
        doesTick = that.doesTick
    }
    
    def loadFrom(that:TileMultipart)
    {
        partList.foreach(_.bind(this))
        if(!isInvalid)
            partList.foreach(_.onWorldJoin())
        if(doesTick)
            setTicking(true)
    }
    
    /**
     * Overidden in TSlottedTile when a part that goes in a slot is added
     */
    def partMap(slot:Int):TMultiPart = null
    
    def jPartList():List[TMultiPart] = partList
    
    override def canUpdate() = doesTick
    
    override def updateEntity()
    {
        super.updateEntity()
        
        TileMultipart.startOperation(this)
        partList.foreach(_.update())
        TileMultipart.finishOperation(this)
    }
    
    override def onChunkUnload()
    {
        partList.foreach(_.onChunkUnload())
    }
    
    def onChunkLoad()
    {
        partList.foreach(_.onChunkLoad())
    }
    
    override def validate()
    {
        val wasInvalid = isInvalid
        super.validate()
        if(wasInvalid)
            partList.foreach(_.onMoved())
    }
    
    override def invalidate()
    {
        super.invalidate()
        if(worldObj != null)
            partList.foreach(_.onWorldSeparate())
    }
    
    /**
     * Called by parts when they have changed in some form that affects the world.
     * Notifies neighbor blocks, parts that share this host and recalculates lighting
     */
    def notifyPartChange(part:TMultiPart)
    {
        internalPartChange(part)
        
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType().blockID)
        worldObj.updateAllLightTypes(xCoord, yCoord, zCoord)
    }
    
    /**
     * Notifies parts sharing this host of a change
     */
    def internalPartChange(part:TMultiPart)
    {
        TileMultipart.startOperation(this)
        partList.foreach{ p =>
            if(part != p)
                p.onPartChanged(part)
        }
        TileMultipart.finishOperation(this)
    }
    
    /**
     * Notifies all parts not in the passed collection of a change from all the parts in the collection
     */
    def multiPartChange(parts:Collection[TMultiPart])
    {
        TileMultipart.startOperation(this)
        partList.foreach{ p =>
            if(!parts.contains(p))
                parts.foreach(p.onPartChanged(_))
        }
        TileMultipart.finishOperation(this)
    }
    
    def onNeighborBlockChange(world:World, x:Int, y:Int, z:Int, id:Int)
    {
        TileMultipart.startOperation(this)
        partList.foreach(_.onNeighborChanged())
        TileMultipart.finishOperation(this)
    }
    
    def getLightValue() = partList.foldLeft(0)((l, p) => Math.max(l, p.getLightValue))
    
    def markDirty()
    {
        worldObj.updateTileEntityChunkAndDoNothing(xCoord, yCoord, zCoord, this)
    }
    
    def isSolid(side:Int):Boolean = 
    {
        val part = partMap(side)
        if(part != null) 
            return part.asInstanceOf[TFacePart].solid(side)
        
        return false
    }
    
    private def setTicking(tick:Boolean)
    {
        if(doesTick == tick)
            return
        
        doesTick = tick
        if(worldObj != null)
        {
            if(tick)
                worldObj.addTileEntity(this)
            else
                worldObj.loadedTileEntityList.remove(this)
        }
    }
    
    def canAddPart(part:TMultiPart):Boolean =
    {
        if(partList.contains(part))
            return false
        
        return occlusionTest(partList, part)
    }
    
    def canReplacePart(opart:TMultiPart, npart:TMultiPart):Boolean = 
    {
        val olist = partList.filterNot(_ == opart)
        if(olist.contains(npart))
            return false
        
        return occlusionTest(olist, npart)
    }
    
    def occlusionTest(parts:Seq[TMultiPart], npart:TMultiPart):Boolean =
    {
        return parts.forall(part => part.occlusionTest(npart) && npart.occlusionTest(part))
    }
    
    def getWriteStream(part:TMultiPart):MCDataOutput = getWriteStream.writeByte(partList.indexOf(part))
    
    private def getWriteStream = MultipartSPH.getTileStream(worldObj, new BlockCoord(this))
    
    private[multipart] def addPart_impl(part:TMultiPart)
    {
        if(!worldObj.isRemote)
            writeAddPart(part)
            
        addPart_do(part)
        part.onAdded()
        partAdded(part)
        notifyPartChange(part)
        markDirty()
        markRender()
    }
    
    private[multipart] def writeAddPart(part:TMultiPart)
    {
        val stream = getWriteStream.writeByte(253)
        MultiPartRegistry.writePartID(stream, part)
        part.writeDesc(stream)
    }
    
    private[multipart] def addPart_do(part:TMultiPart)
    {
        assert(partList.size < 250, "Tried to add more than 250 parts to the one tile. You're doing it wrong")
        
        partList+=part
        bindPart(part)
        part.bind(this)
        
        if(!doesTick && part.doesTick)
            setTicking(true)
    }
    
    /**
     * Bind this part to an internal cache
     */
    def bindPart(part:TMultiPart){}
    
    def partAdded(part:TMultiPart){}
    
    def remPart(part:TMultiPart):TileMultipart =
    {
        assert(!worldObj.isRemote, "Cannot remove multi parts from a client tile")
        
        if(TileMultipart.queueRemoval(this, part))
            return null
        
        remPart_impl(part)
    }
    
    private[multipart] def remPart_impl(part:TMultiPart):TileMultipart =
    {
        remPart_do(part, !worldObj.isRemote)
        
        if(!isInvalid())
        {
            notifyPartChange(part)
            markDirty()
            markRender()
        }
        
        if(!isInvalid())
            return MultipartGenerator.partRemoved(this, part)
        
        return null
    }
    
    private def remPart_do(part:TMultiPart, sendPacket:Boolean):Int =
    {
        val r = partList.indexOf(part)
        if(r < 0)
            throw new IllegalArgumentException("Tried to remove a non-existant part")
        
        partList-=part
        
        if(sendPacket)
            getWriteStream.writeByte(254).writeByte(r)
        
        partRemoved(part, r)
        part.onRemoved()
        part.tile = null
        
        if(partList.isEmpty)
        {
            worldObj.setBlockToAir(xCoord, yCoord, zCoord)
        }
        else
        {
            if(part.doesTick && doesTick)
            {
                var ntick = false
                partList.foreach(part => ntick |= part.doesTick)
                if(!ntick)
                    setTicking(false)
            }
        }
        return r
    }
    
    def partRemoved(part:TMultiPart, p:Int){}

    private[multipart] def loadParts(parts:ListBuffer[TMultiPart])
    {
        clearParts()
        parts.foreach(p => addPart_do(p))
        if(worldObj != null)
            notifyPartChange(null)
    }
    
    def clearParts()
    {
        partList.clear()
    }
    
    def writeDesc(packet:MCDataOutput)
    {
        packet.writeByte(partList.size)
        partList.foreach{part =>
            MultiPartRegistry.writePartID(packet, part)
            part.writeDesc(packet)
        }
    }
    
    def harvestPart(index:Int, drop:Boolean):Boolean = 
    {
        val part = partList(index)
        if(part == null)
            return false
        if(drop)
            dropItems(part.getDrops)
        remPart(part)
        return partList.isEmpty
    }
    
    def dropItems(items:Iterable[ItemStack])
    {
        val pos = Vector3.fromTileEntityCenter(this)
        items.foreach(item => TileMultipart.dropItem(item, worldObj, pos))
    }
    
    def markRender()
    {
        worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord)
    }
    
    override def writeToNBT(tag:NBTTagCompound)
    {
        super.writeToNBT(tag)
        val taglist = new NBTTagList
        partList.foreach{part => 
            val parttag = new NBTTagCompound
            parttag.setString("id", part.getType)
            part.save(parttag)
            taglist.appendTag(parttag)
    }
        tag.setTag("parts", taglist)
    }
    
    def onEntityCollision(entity:Entity)
    {
        TileMultipart.startOperation(this)
        partList.foreach(_.onEntityCollision(entity))
        TileMultipart.finishOperation(this)
    }
    
    def strongPowerLevel(side:Int) = 0
    
    def weakPowerLevel(side:Int) = 0
    
    def canConnectRedstone(side:Int) = false
    
    def notifyNeighborChange(side:Int)
    {
        val pos = new BlockCoord(this).offset(side)
        worldObj.notifyBlocksOfNeighborChange(pos.x, pos.y, pos.z, getBlockType().blockID)
    }
}

class TileMultipartClient extends TileMultipart
{
    def renderStatic(pos:Vector3, olm:LazyLightMatrix, pass:Int)
    {
        partList.foreach(part => part.renderStatic(pos, olm, pass))
    }
    
    def renderDynamic(pos:Vector3, frame:Float, pass:Int)
    {
        partList.foreach(part => part.renderDynamic(pos, frame, pass:Int))
    }
    
    def randomDisplayTick(random:Random){}
    
    override def shouldRenderInPass(pass:Int) = 
    {
        MultipartRenderer.pass = pass
        true
    }
}

object TileMultipart
{
    var renderID:Int = -1
    
    class TileOperationSet
    {
        var tile:TileMultipart = _
        var depth = 0
        private val removalQueue = Queue[TMultiPart]()
        private val additionQueue = Queue[TMultiPart]()
        
        def start(t:TileMultipart)
        {
            tile = t
            depth = 1
        }
        
        def queueAddition(part:TMultiPart)
        {
            if(!additionQueue.contains(part))
                additionQueue+=part
        }
        
        def queueRemoval(part:TMultiPart)
        {
            if(!removalQueue.contains(part))
                removalQueue+=part
        }
        
        def finish()
        {
            if(removalQueue.isEmpty && additionQueue.isEmpty)
                return
            
            var otile = tile
            val world = tile.worldObj
            val pos = new BlockCoord(tile)
            while(!removalQueue.isEmpty)
                otile = otile.remPart_impl(removalQueue.dequeue)
            
            while(!additionQueue.isEmpty)
                MultipartGenerator.addPart(world, pos, additionQueue.dequeue)
        }
    }
    
    class OperationSynchroniser
    {
        private val operations = ArrayBuffer[TileOperationSet]()
        private var depth = 0
        
        def startOperation(tile:TileMultipart)
        {
            var i = 0
            while(i < depth)
            {
                val op = operations(i)
                if(op.tile == tile)
                {
                    op.depth+=1
                    return
                }
                i+=1
            }
            if(depth == operations.length)
                operations+=new TileOperationSet
            operations(depth).start(tile)
            depth+=1
        }
        
        def finishOperation(tile:TileMultipart)
        {
            var i = depth-1
            while(i >= 0)
            {
                val op = operations(i)
                if(op.tile == tile)
                {
                    op.depth-=1
                    if(op.depth == 0)
                    {
                        if(i != depth-1)
                            throw new IllegalStateException("Tried to finish an operation that was not on top")
                        depth-=1
                        op.finish()
                    }
                    return
                }
                i-=1
            }
            throw new IllegalStateException("Inconsistant Operation stack")
        }
        
        def queueRemoval(tile:TileMultipart, part:TMultiPart):Boolean =
        {
            var i = depth-1
            while(i >= 0)
            {
                val op = operations(i)
                if(op.tile == tile)
                {
                    op.queueRemoval(part)
                    return true
                }
                i-=1
            }
            return false
        }
        
        def queueAddition(world:World, pos:BlockCoord, part:TMultiPart):Boolean = 
        {
            var i = depth-1
            while(i >= 0)
            {
                val op = operations(i)
                val opt = op.tile
                if(opt.worldObj == world && opt.xCoord == pos.x && opt.yCoord == pos.y && opt.zCoord == pos.z)
                {
                    op.queueRemoval(part)
                    return true
                }
                i-=1
            }
            return false
        }
    }
    
    private val operationSync = new OperationSynchroniser
    
    def startOperation(tile:TileMultipart) = if(!tile.worldObj.isRemote) operationSync.startOperation(tile)
    
    def finishOperation(tile:TileMultipart) = if(!tile.worldObj.isRemote) operationSync.finishOperation(tile)
        
    def queueRemoval(tile:TileMultipart, part:TMultiPart):Boolean = operationSync.queueRemoval(tile, part)
    
    def queueAddition(world:World, pos:BlockCoord, part:TMultiPart):Boolean = operationSync.queueAddition(world, pos, part)
    
    def getOrConvertTile(world:World, pos:BlockCoord) = getOrConvertTile2(world, pos)._1
    
    def getOrConvertTile2(world:World, pos:BlockCoord):(TileMultipart, Boolean) =
    {
        val t = world.getBlockTileEntity(pos.x, pos.y, pos.z)
        if(t.isInstanceOf[TileMultipart])
            return (t.asInstanceOf[TileMultipart], false)
        
        val id = world.getBlockId(pos.x, pos.y, pos.z)
        val p = MultiPartRegistry.convertBlock(world, pos, id)
        if(p != null)
        {
            val t = MultipartGenerator.generateCompositeTile(null, Seq(p), world.isRemote)
            t.xCoord = pos.x
            t.yCoord = pos.y
            t.zCoord = pos.z
            t.setWorldObj(world)
            t.addPart_do(p)
            return (t, true)
        }
        return (null, false)
    }
    
    def getTile(world:World, pos:BlockCoord):TileMultipart =
    {
        val t = world.getBlockTileEntity(pos.x, pos.y, pos.z)
        if(t.isInstanceOf[TileMultipart])
            return t.asInstanceOf[TileMultipart]
        return null
    }
    
    def canPlacePart(world:World, pos:BlockCoord, part:TMultiPart):Boolean =
    {
        part.getCollisionBoxes.foreach{b => 
            if(!world.checkNoEntityCollision(b.toAABB().offset(pos.x, pos.y, pos.z)))
                return false
        }
        
        val t = getOrConvertTile(world, pos)
        if(t != null)
            return t.canAddPart(part)
        
        if(!replaceable(world, pos))
            return false
        
        return true
    }
    
    def replaceable(world:World, pos:BlockCoord):Boolean = 
    {
        val block = Block.blocksList(world.getBlockId(pos.x, pos.y, pos.z))
        return block == null || block.isAirBlock(world, pos.x, pos.y, pos.z) || block.isBlockReplaceable(world, pos.x, pos.y, pos.z)
    }
    
    def addPart(world:World, pos:BlockCoord, part:TMultiPart):TileMultipart =
    {
        assert(!world.isRemote, "Cannot add multi parts to a client tile.")
        
        if(queueAddition(world, pos, part))
            return null
        
        return MultipartGenerator.addPart(world, pos, part)
    }
    
    def handleDescPacket(world:World, pos:BlockCoord, packet:PacketCustom)
    {
        val nparts = packet.readUByte
        val parts = new ListBuffer[TMultiPart]()
        for(i <- 0 until nparts)
        {
            val part:TMultiPart = MultiPartRegistry.readPart(packet)
            part.readDesc(packet)
            parts+=part
        }
        
        if(parts.size == 0)
            return
        
        val t = world.getBlockTileEntity(pos.x, pos.y, pos.z)
        val tilemp = MultipartGenerator.generateCompositeTile(t, parts, true)
        if(tilemp != t)
            world.setBlockTileEntity(pos.x, pos.y, pos.z, tilemp)
        
        tilemp.loadParts(parts)
        tilemp.markRender()
    }
    
    def handlePacket(pos:BlockCoord, world:World, i:Int, packet:PacketCustom)
    {
        var tilemp = BlockMultipart.getTile(world, pos.x, pos.y, pos.z)
        
        i match
        {
            case 253 => {
                val part = MultiPartRegistry.readPart(packet)
                part.readDesc(packet)
                MultipartGenerator.addPart(world, pos, part)
            }
            case 254 => if(tilemp != null) {
                tilemp.remPart_impl(tilemp.partList(packet.readUByte))
            }
            case _ => if(tilemp != null) {
                tilemp.partList(i).read(packet)
            }
        }
    }
    
    def createFromNBT(tag:NBTTagCompound):TileMultipart =
    {
        val superID = tag.getString("superID")
        //val superClass = TileEntity.nameToClassMap.get(superID)
        
        val partList = tag.getTagList("parts")
        val parts = ListBuffer[TMultiPart]()
        
        for(i <- 0 until partList.tagCount)
        {
            val partTag = partList.tagAt(i).asInstanceOf[NBTTagCompound]
            val partID = partTag.getString("id")
            val part = MultiPartRegistry.createPart(partID, false)
            if(part != null)
            {
                part.load(partTag)
                parts+=part
            }
        }
        
        if(parts.size == 0)
            return null
        
        val tmb = MultipartGenerator.generateCompositeTile(null, parts, false)
        tmb.readFromNBT(tag)
        tmb.loadParts(parts)
        return tmb
    }
    
    def dropItem(stack:ItemStack, world:World, pos:Vector3)
    {
        val item = new EntityItem(world, pos.x, pos.y, pos.z, stack)
        item.motionX = world.rand.nextGaussian() * 0.05
        item.motionY = world.rand.nextGaussian() * 0.05 + 0.2
        item.motionZ = world.rand.nextGaussian() * 0.05
        item.delayBeforeCanPickup = 10
        world.spawnEntityInWorld(item)
    }
}

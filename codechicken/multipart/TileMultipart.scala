package codechicken.multipart

import net.minecraft.tileentity.TileEntity
import scala.collection.mutable.ListBuffer
import net.minecraft.network.packet.Packet
import codechicken.core.packet.PacketCustom
import codechicken.multipart.handler.MultipartCPH
import codechicken.core.vec.BlockCoord
import net.minecraft.world.World
import java.util.List
import scala.collection.JavaConverters._
import net.minecraft.nbt.NBTTagCompound
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.DataInputStream
import codechicken.core.data.MCDataInputStream
import codechicken.core.data.MCDataOutput
import codechicken.core.data.MCDataOutputStream
import net.minecraft.client.multiplayer.NetClientHandler
import codechicken.core.vec.Cuboid6
import scala.collection.mutable.HashSet
import codechicken.multipart.handler.MultipartProxy
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import codechicken.core.vec.Vector3
import net.minecraft.nbt.NBTTagList
import net.minecraft.client.particle.EffectRenderer
import net.minecraft.util.MovingObjectPosition
import scala.collection.mutable.ArrayBuffer
import codechicken.scala.ScalaBridge._
import java.util.Random
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import scala.collection.mutable.Queue
import codechicken.multipart.handler.MultipartSPH
import codechicken.core.lighting.LazyLightMatrix
import net.minecraft.world.ChunkCoordIntPair
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer

trait TileMultipart extends TileEntity
{
    var partList: ArrayBuffer[TMultiPart] = ArrayBuffer()
    var partMap: Array[TMultiPart] = new Array(27)
    
    private var doesTick = false
    
    def loadFrom(that:TileMultipart)//Potentially auto gen this
    {
        partList = that.partList
        partMap = that.partMap
        doesTick = that.doesTick
        
        partList.foreach(_.bind(this))
    }
    
    def jPartList():List[TMultiPart] = partList.asJava
    
    override def canUpdate() = doesTick//TODO: part merging true
    
    override def updateEntity()
    {
        super.updateEntity()
        
        TileMultipartObj.startOperation(this)
        partList.foreach(_.update())
        TileMultipartObj.finishOperation(this)
    }
    
    override def setWorldObj(world:World)
    {
        super.setWorldObj(world)
        partList.foreach(_.onWorldJoin(world))
    }
    
    def loaded() = !isInvalid
    
    def notifyPartChange()
    {
        TileMultipartObj.startOperation(this)
        partList.foreach(_.onPartChanged())
        TileMultipartObj.finishOperation(this)
        
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType().blockID)
        worldObj.updateAllLightTypes(xCoord, yCoord, zCoord)
    }
    
    def onNeighborBlockChange(world:World, x:Int, y:Int, z:Int, id:Int)
    {
        TileMultipartObj.startOperation(this)
        partList.foreach(_.onNeighbourChanged())
        TileMultipartObj.finishOperation(this)
    }
    
    def getLightValue() = partList.foldLeft(0)((l, p) => Math.max(l, p.getLightValue))
    
    def markDirty()
    {
        worldObj.updateTileEntityChunkAndDoNothing(xCoord, yCoord, zCoord, this)
    }
    
    def isSolid(side:Int):Boolean = 
    {
        val part = partMap(PartMap.face(side).i)
        if(part != null) part.asInstanceOf[TFacePart].solid(side) else false
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
            
        val slotMask = part.getSlotMask
        for(i <- 0 until partMap.length)
            if((slotMask&1<<i) != 0 && partMap(i) != null)
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
    
    private[multipart] def getWriteStream = MultipartSPH.getTileStream(worldObj, new BlockCoord(this))
    
    private[multipart] def addPart(part:TMultiPart)
    {
        addPart_do(part)
        notifyPartChange()
        markDirty()
        markRender()
        
        if(!worldObj.isRemote)
            writeAddPart(part)
    }
    
    private[multipart] def writeAddPart(part:TMultiPart)
    {
        val stream = getWriteStream.writeByte(253)
        MultiPartRegistry.writePartID(stream, part)
        part.writeDesc(stream)
    }
    
    private[multipart] def addPart_do(part:TMultiPart)
    {
        if(partList.size >= 250)
            throw new IllegalArgumentException("Tried to add more than 250 parts to the one tile. You're doing it wrong")
        
        partList+=part
        val mask = part.getSlotMask
        for(i <- 0 until 27)
            if ((mask&1<<i) > 0)
                partMap(i) = part
        
        part.bind(this)
        
        if(!doesTick && part.doesTick)
            setTicking(true)
        
        partAdded(part)
    }
    
    def partAdded(part:TMultiPart){}
    
    def remPart(part:TMultiPart):TileMultipart =
    {
        if(TileMultipartObj.queueRemoval(this, part))
            return null
        
        val i = remPart_do(part)
        if(!isInvalid())
        {
            notifyPartChange()
            markDirty()
            markRender()
        }
        
        if(!worldObj.isRemote)
            getWriteStream.writeByte(254).writeByte(i)
        
        if(!isInvalid())
            return MultipartGenerator.partRemoved(this, part)
        
        return null
    }
    
    private def remPart_do(part:TMultiPart):Int =
    {
        val r = partList.indexOf(part)
        if(r < 0)
            throw new IllegalArgumentException("Tried to remove a non-existant part")
        
        partList-=part
        for(i <- 0 until 27)
            if(partMap(i) == part)
                partMap(i) = null
        
        partRemoved(part, r)
        part.bind(null)
        
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
        for(i <- 0 until partMap.length)
            partMap(i) = null
        parts.foreach(p => addPart_do(p))
        if(worldObj != null)
            notifyPartChange()
    }
    
    def clearParts()
    {
        partList.clear()
    }
    
    def writeDesc(packet:PacketCustom)
    {
        packet.writeByte(partList.size)
        partList.foreach{part => {
            MultiPartRegistry.writePartID(packet, part)
            part.writeDesc(packet)
        }}
    }
    
    def harvestPart(index:Int, drop:Boolean):Boolean = 
    {
        val part = partList(index)
        if(part == null)
            return false
        if(drop)
            dropItems(part.getDrops)
        remPart(part)
        return true
    }
    
    def dropItems(items:Seq[ItemStack])
    {
        val pos = Vector3.fromTileEntityCenter(this)
        items.foreach(item => TileMultipartObj.dropItem(item, worldObj, pos))
    }
    
    def markRender()
    {
        worldObj.markBlockForRenderUpdate(xCoord, yCoord, zCoord)
    }
    
    override def writeToNBT(tag:NBTTagCompound)
    {
        super.writeToNBT(tag)
        val taglist = new NBTTagList
        partList.foreach { part => {
            val parttag = new NBTTagCompound
            parttag.setString("id", part.getType)
            part.save(parttag)
            taglist.appendTag(parttag)
        }}
        tag.setTag("parts", taglist)
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

trait TileMultipartClient extends TileMultipart
{
    def renderStatic(pos:Vector3, olm:LazyLightMatrix, pass:Int)
    {
        partList.foreach(part => part.renderStatic(pos, olm, pass))
    }
    
    def renderDynamic(pos:Vector3, frame:Float)
    {
        partList.foreach(part => part.renderDynamic(pos, frame))
    }
    
    def randomDisplayTick(random:Random){}
}

object TileMultipartObj
{
    var renderID:Int = -1
    
    private var operatingTile = new ThreadLocal[TileMultipart]
    private val removalQueue = Queue[TMultiPart]()
    private val additionQueue = Queue[TMultiPart]()
    
    def startOperation(tile:TileMultipart)
    {
        if(operatingTile.get != null)
            throw new IllegalStateException("Recursive operations not implemented")
        
        operatingTile.set(tile)
    }
    
    def finishOperation(tile:TileMultipart)
    {
        operatingTile.remove
        
        if(removalQueue.isEmpty && additionQueue.isEmpty)
            return
        
        var otile = tile
        val world = tile.worldObj
        val pos = new BlockCoord(tile)
        while(!removalQueue.isEmpty)
            otile = otile.remPart(removalQueue.dequeue)
        
        while(!additionQueue.isEmpty)
            MultipartGenerator.addPart(world, pos, additionQueue.dequeue)
    }
    
    def queueRemoval(tile:TileMultipart, part:TMultiPart):Boolean =
    {
        if(tile != operatingTile.get)
            return false
        
        if(!removalQueue.contains(part))
            removalQueue+=part
        return true
    }
    
    def queueAddition(world:World, pos:BlockCoord, part:TMultiPart):Boolean =
    {
        val opt = operatingTile.get
        if(opt == null || opt.worldObj != world || opt.xCoord != pos.x || opt.yCoord != pos.y || opt.zCoord != pos.z)
            return false
        
        if(!additionQueue.contains(part))
            additionQueue+=part
        
        return true
    }
    
    def getOrConvertTile(world:World, pos:BlockCoord):TileMultipart =
    {
        val t = world.getBlockTileEntity(pos.x, pos.y, pos.z)
        if(t.isInstanceOf[TileMultipart])
            return t.asInstanceOf[TileMultipart]
        
        val id = world.getBlockId(pos.x, pos.y, pos.z)
        val p = MultiPartRegistry.convertBlock(world, pos, id)
        if(p != null)
        {
            val t = MultipartGenerator.generateCompositeTile(null, Seq(p), world.isRemote)
            t.xCoord = pos.x
            t.yCoord = pos.y
            t.zCoord = pos.z
            t.invalidate()
            t.setWorldObj(world)
            t.addPart_do(p)
            return t
        }
        return null
    }
    
    def canAddPart(world:World, pos:BlockCoord, part:TMultiPart):Boolean =
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
        if(queueAddition(world, pos, part))
            return null
        
        return MultipartGenerator.addPart(world, pos, part)
    }
    
    def handleDescPacket(world:World, pos:BlockCoord, packet:PacketCustom)
    {
        val nparts = packet.readUnsignedByte
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
                addPart(world, pos, part)
            }
            case 254 => if(tilemp != null) {
                tilemp.remPart(tilemp.partList(packet.readUnsignedByte()))
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
        val item = new EntityItem(world, pos.x, pos.y, pos.z, stack);
        item.motionX = world.rand.nextGaussian() * 0.05;
        item.motionY = world.rand.nextGaussian() * 0.05 + 0.2F;
        item.motionZ = world.rand.nextGaussian() * 0.05;
        world.spawnEntityInWorld(item);
    }
}

package codechicken.multipart
import scala.collection.mutable.{Map => MMap}
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput

/**
 * Static class for packing update data.
 * When a specific property of a part changes and needs sending to the client, a bit can be set in the mask.
 * This bit can then be checked in the writeScheduled callback.
 * This prevents sending multiple packets if the same property updates more than once per tick.
 */
object PacketScheduler
{
    private val map = MMap[TMultiPart, Long]()
    
    /**
     * Add bits to the current update mask for part. (binary OR)
     */
    def schedulePacket(part:TMultiPart, mask:Long) = map.put(part, map.getOrElse(part, 0L)|mask)

    private[multipart] def sendScheduled() {
        map.foreach{ e =>
            val (part, mask) = e
            if(part.tile != null) {
                val ipart = part.asInstanceOf[IScheduledPacketPart]
                val w = part.getWriteStream
                if(ipart.needsLong)
                    w.writeLong(mask)
                else
                    w.writeInt(mask.toInt)
                
                ipart.writeScheduled(mask, w)
            }
        }
        map.clear()
    }
}

/**
 * Callback interface for PacketScheduler
 */
trait IScheduledPacketPart
{
    /**
     * Write scheduled data to the packet, mask is the cumulative mask from calls to schedulePacket
     */
    def writeScheduled(mask:Long, packet:MCDataOutput)
    
    /**
     * If this returns true, the mask will be sent as a long rather than an int. Use if you have more than 32 flags
     */
    def needsLong():Boolean
    
    /**
     * Read data matching mask. Estiablishes a method for subclasses to override. This should be called from read
     */
    def readScheduled(mask:Long, packet:MCDataInput)
}

trait TScheduledPacketPart extends TMultiPart with IScheduledPacketPart
{
    def needsLong() = false
    
    final override def read(packet:MCDataInput) {
        val mask = if(needsLong) packet.readLong else packet.readInt
        readScheduled(mask, packet)
    }
    
    def writeScheduled(mask:Long, packet:MCDataOutput){}
    def readScheduled(mask:Long, packet:MCDataInput){}
}
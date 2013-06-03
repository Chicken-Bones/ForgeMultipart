package codechicken.multipart;

import scala.collection.mutable.HashMap
import codechicken.core.packet.PacketCustom
import codechicken.core.data.MCDataOutput
import codechicken.core.data.MCDataInput
import net.minecraft.world.World
import codechicken.core.vec.BlockCoord
import scala.collection.mutable.ListBuffer

object MultiPartRegistry
{
    trait IPartFactory
    {
        def createPart(name:String, client:Boolean):TMultiPart;
    }
    
    trait IPartConverter
    {
        def canConvert(blockID:Int):Boolean
        def convert(world:World, pos:BlockCoord):TMultiPart
    }
    
    private val typeMap:HashMap[String, (Boolean)=>TMultiPart] = new HashMap
    private val nameMap:HashMap[String, Int] = new HashMap
    private var idMap:Array[(String, (Boolean)=>TMultiPart)] = _
    private val idWriter = IDWriter()
    private val converters:Array[Seq[IPartConverter]] = Array.fill(4096)(Seq())
    
    private var state:Int = 0
    
    def registerParts(partFactory:IPartFactory, types:Array[String])
    {
        registerParts(partFactory.createPart _, types:_*);
    }
    
    def registerParts(partFactory:(String, Boolean)=>TMultiPart, types:String*)
    {
        if(loaded)
            throw new IllegalStateException("You must register your parts in the init methods.")
        state=1
        
        types.foreach{s => 
            if(typeMap.contains(s))
                throw new IllegalStateException("Part with id "+s+" is already registered.");
            
            typeMap.put(s, (c:Boolean) => partFactory(s, c));
        }
    }
    
    def registerConverter(c:IPartConverter)
    {
        for(i <- 0 until 4096)
            if(c.canConvert(i))
                converters(i) = converters(i):+c
    }
    
    def beforeServerStart()
    {
        idMap = typeMap.toList.sortBy(_._1).toArray
        idWriter.setMax(idMap.length)
        nameMap.clear
        for(i <- 0 until idMap.length)
            nameMap.put(idMap(i)._1, i)
    }
    
    def writeIDMap(packet:PacketCustom)
    {
        packet.writeInt(idMap.length)
        idMap.foreach(e => packet.writeString(e._1))
    }
    
    def readIDMap(packet:PacketCustom):Seq[String] =
    {
        val k = packet.readInt()
        idWriter.setMax(k)
        idMap = new Array(k)
        val missing = ListBuffer[String]()
        for(i <- 0 until k)
        {
            val s = packet.readString()
            val v = typeMap.get(s)
            if(v.isEmpty)
                missing+=s
            else
                idMap(i) = (s, v.get)
        }
        return missing
    }
    
    def required = state > 0
    
    def loaded = state == 2
    
    def postInit(){state = 2}
    
    def writePartID(data:MCDataOutput, part:TMultiPart)
    {
        idWriter.write(data, nameMap.get(part.getType).get)
    }
    
    def readPart(data:MCDataInput) = idMap(idWriter.read(data))._2(true)
    
    def createPart(name:String, client:Boolean):TMultiPart = 
    {
        val part = typeMap.get(name)
        if(part.isDefined)
            return part.get(client)
        else
        {
            System.err.println("Missing mapping for part with ID: "+name)
            return null
        }
    }
    
    def convertBlock(world:World, pos:BlockCoord, id:Int):TMultiPart =
    {
        converters(id).foreach{c =>
            val ret = c.convert(world, pos)
            if(ret != null)
                return ret
        }
        return null
    }
}

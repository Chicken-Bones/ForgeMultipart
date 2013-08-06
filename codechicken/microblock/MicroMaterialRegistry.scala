package codechicken.microblock

import codechicken.core.vec.Cuboid6
import codechicken.multipart.IDWriter
import codechicken.core.lighting.LightMatrix
import codechicken.core.packet.PacketCustom
import scala.collection.mutable.HashMap
import codechicken.core.data.MCDataOutput
import codechicken.core.data.MCDataInput
import codechicken.multipart.MultiPartRegistry
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.microblock.handler.MicroblockProxy
import codechicken.core.vec.Vector3
import net.minecraft.util.Icon
import net.minecraft.entity.player.EntityPlayer
import codechicken.core.render.Vertex5
import net.minecraft.item.ItemStack
import scala.collection.mutable.ListBuffer
import net.minecraft.block.StepSound

object MicroMaterialRegistry
{
    trait IMicroMaterial
    {
        @SideOnly(Side.CLIENT)
        def getBreakingIcon(side:Int):Icon
        
        @SideOnly(Side.CLIENT)
        def loadIcons(){}
        
        @SideOnly(Side.CLIENT)
        def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, part:Microblock)
        
        @SideOnly(Side.CLIENT)
        def getRenderPass():Int
        
        def isTransparent():Boolean
        
        def getLightValue():Int
        
        def getStrength(player:EntityPlayer):Float
        
        def getLocalizedName():String
        
        def getItem():ItemStack
        
        def getCutterStrength():Int
        
        def getSound():StepSound
        //todo, get material properties
    }
    
    private val typeMap:HashMap[String, IMicroMaterial] = new HashMap
    private val nameMap:HashMap[String, Int] = new HashMap
    private var idMap:Array[(String, IMicroMaterial)] = _
    private val idWriter = IDWriter()
    
    def registerMaterial(material:IMicroMaterial, name:String)
    {
        if(MultiPartRegistry.loaded)
            throw new IllegalStateException("You must register your parts in the init methods.")
        
        if(typeMap.contains(name))
            throw new IllegalStateException("Material with id "+name+" is already registered.")
        
        System.out.println("Registered micro material: "+name)
        
        typeMap.put(name, material)
    }
    
    def setupIDMap()
    {
        idMap = typeMap.toList.sortBy(_._1).toArray
        idWriter.setMax(idMap.length)
        nameMap.clear
        for(i <- 0 until idMap.length)
            nameMap.put(idMap(i)._1, i)
    }
    
    def writeIDMap(packet:PacketCustom)
    {
        packet.writeInt(idMap.size)
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
    
    def writePartID(data:MCDataOutput, id:Int)
    {
        idWriter.write(data, id)
    }
    
    def readPartID(data:MCDataInput) = idWriter.read(data)
    
    def materialName(id:Int) = idMap(id)._1
    
    def materialID(name:String):Int = 
    {
        val v = nameMap.get(name)
        if(v.isDefined)
            return v.get
        else
        {
            System.err.println("Missing mapping for part with ID: "+name)
            return 0
        }
    }
    
    def getMaterial(name:String) = typeMap.getOrElse(name, null)
    
    def getMaterial(id:Int) = idMap(id)._2
    
    def getIdMap = idMap
}
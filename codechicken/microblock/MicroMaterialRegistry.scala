package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.IDWriter
import codechicken.lib.lighting.LightMatrix
import codechicken.lib.packet.PacketCustom
import scala.collection.mutable.HashMap
import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput
import codechicken.multipart.MultiPartRegistry
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.microblock.handler.MicroblockProxy
import codechicken.lib.vec.Vector3
import net.minecraft.util.Icon
import net.minecraft.entity.player.EntityPlayer
import codechicken.lib.render.Vertex5
import net.minecraft.item.ItemStack
import scala.collection.mutable.ListBuffer
import net.minecraft.block.StepSound
import net.minecraft.util.MovingObjectPosition
import net.minecraft.world.World

object MicroMaterialRegistry
{
    /**
     * Interface for defining a micro material
     */
    trait IMicroMaterial
    {
        /**
         * The icon to be used for breaking particles on side
         */
        @SideOnly(Side.CLIENT)
        def getBreakingIcon(side:Int):Icon
        
        /**
         * Callback to load icons from the underlying block/etc
         */
        @SideOnly(Side.CLIENT)
        def loadIcons(){}
        
        /**
         * This function must render the quad face specified by verts facing on side, at pos (x, y, z)
         * @param lightMatrix A helper class provided for calculating minecraft smooth lighting on the face.
         * @param part An IMicroMaterialRender callback for some information about the caller such as bounds and world for grass decals.
         */
        @SideOnly(Side.CLIENT)
        def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, part:IMicroMaterialRender)
        
        /**
         * Get the render pass for which this material renders in.
         */
        @SideOnly(Side.CLIENT)
        def getRenderPass():Int
        
        /**
         * Return true if this material is not opaque (glass, ice).
         */
        def isTransparent():Boolean
        
        /**
         * Return the light level emitted by this material (glowstone)
         */
        def getLightValue():Int
        
        /**
         * Return the strength of this material
         */
        def getStrength(player:EntityPlayer):Float
        
        /**
         * Return the localised name of this material (normally the block name)
         */
        def getLocalizedName():String
        
        /**
         * Get the item that this material is cut from (full block -> slabs)
         */
        def getItem():ItemStack
        
        /**
         * Get the strength of saw requried to cut this material
         */
        def getCutterStrength():Int
        
        /**
         * Get the breaking/walking sound
         */
        def getSound():StepSound
        
        /**
         * Return true if this material is solid and opaque (can run wires on etc)
         */
        def isSolid() = !isTransparent
    }
    
    /**
     * Interface for overriding the default micro placement highlight handler to show the effect of placement on a certain block/part
     */
    trait IMicroHighlightRenderer
    {
        /**
         * Return true if a custom highlight was rendered and the default should be skipped
         */
        def renderHighlight(world:World, player:EntityPlayer, hit:MovingObjectPosition, mcrClass:MicroblockClass, size:Int, material:Int):Boolean
    }
    
    private val typeMap:HashMap[String, IMicroMaterial] = new HashMap
    private val nameMap:HashMap[String, Int] = new HashMap
    private var idMap:Array[(String, IMicroMaterial)] = _
    private val idWriter = new IDWriter
    
    private val highlightRenderers = ListBuffer[IMicroHighlightRenderer]()
    
    /**
     * Register a micro material with unique identifier name
     */
    def registerMaterial(material:IMicroMaterial, name:String)
    {
        if(MultiPartRegistry.loaded)
            throw new IllegalStateException("You must register your materials in the init methods.")
        
        if(typeMap.contains(name))
            throw new IllegalStateException("Material with id "+name+" is already registered.")
        
        System.out.println("Registered micro material: "+name)
        
        typeMap.put(name, material)
    }
    
    /**
     * Replace a micro material with unique identifier name
     */
    def replaceMaterial(material:IMicroMaterial, name:String) {
        if(MultiPartRegistry.loaded)
            throw new IllegalStateException("You must register your materials in the init methods.")
        
        if(typeMap.remove(name).isEmpty)
            System.err.println("Material with id "+name+" is was not registered.")
        
        System.out.println("Replaced micro material: "+name)
        
        typeMap.put(name, material)
    }
    
    /**
     * Registers a highlight renderer
     */
    def registerHighlightRenderer(handler:IMicroHighlightRenderer)
    {
        highlightRenderers+=handler
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
    
    def renderHighlight(world:World, player:EntityPlayer, hit:MovingObjectPosition, mcrClass:MicroblockClass, size:Int, material:Int):Boolean =
    {
        val overriden = highlightRenderers.find(_.renderHighlight(world, player, hit, mcrClass, size, material))
        if(overriden.isDefined)
            return true
        
        return mcrClass.renderHighlight(world, player, hit, size, material)
    }
}
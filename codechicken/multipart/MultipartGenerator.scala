package codechicken.multipart

import net.minecraft.tileentity.TileEntity
import scala.collection.immutable.Map
import net.minecraft.world.World
import codechicken.core.vec.BlockCoord
import codechicken.multipart.handler.MultipartProxy
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.Side
import codechicken.core.packet.PacketCustom
import net.minecraft.network.packet.Packet53BlockChange
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ListBuffer
import codechicken.multipart.asm.IMultipartFactory
import codechicken.multipart.asm.ScalaCompilerFactory

object MultipartGenerator
{
    private var tileTraitMap:Map[Class[_], Set[String]] = Map()
    private var interfaceTraitMap_c:Map[String, String] = Map()
    private var interfaceTraitMap_s:Map[String, String] = Map()
    private var partTraitMap_c:Map[Class[_], Seq[String]] = Map()
    private var partTraitMap_s:Map[Class[_], Seq[String]] = Map()
    
    var factory:IMultipartFactory = ScalaCompilerFactory
    
    def partTraitMap(client:Boolean) = if(client) partTraitMap_c else partTraitMap_s
    
    def interfaceTraitMap(client:Boolean) = if(client) partTraitMap_c else interfaceTraitMap_s
    
    def traitsForPart(part:TMultiPart, client:Boolean):Seq[String] = 
    {
        var ret = partTraitMap(client).getOrElse(part.getClass, null)
        if(ret == null)
        {
            def heirachy(clazz:Class[_]):Seq[Class[_]] =
            {
                var superClasses:Seq[Class[_]] = clazz.getInterfaces.flatMap(c => heirachy(c)):+clazz
                if(clazz.getSuperclass != null)
                    superClasses = superClasses++heirachy(clazz.getSuperclass)
                return superClasses
            }
            
            ret = heirachy(part.getClass).flatMap(c => interfaceTraitMap_c.get(c.getName)).distinct
            if(client)
                partTraitMap_c = partTraitMap_c+(part.getClass -> ret)
            else
                partTraitMap_s = partTraitMap_s+(part.getClass -> ret)
        }
        return ret
    }
    
    /**
     * Check if part adds any new interfaces to tile, if so, replace tile with a new copy and call tile.addPart(part)
     * returns true if tile was replaced
     */
    private[multipart] def addPart(world:World, pos:BlockCoord, part:TMultiPart):TileMultipart =
    {
        var loaded = TileMultipartObj.getOrConvertTile2(world, pos)
        var partTraits = traitsForPart(part, world.isRemote)
        val tile = loaded._1
        var ntile = tile
        if(ntile != null)
        {
            if(loaded._2)//perform client conversion
            {
                world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block.blockID, 0, 1)
                world.setBlockTileEntity(pos.x, pos.y, pos.z, ntile)
                PacketCustom.sendToChunk(new Packet53BlockChange(pos.x, pos.y, pos.z, world), world, pos.x>>4, pos.z>>4)
                ntile.partList(0).onConverted()
                ntile.writeAddPart(ntile.partList(0))
            }
            
            val tileTraits = tileTraitMap(tile.getClass)
            partTraits = partTraits.filter(!tileTraits(_))
            if(!partTraits.isEmpty)
            {
                ntile = factory.generateTile(partTraits++tileTraits, world.isRemote)
                world.setBlockTileEntity(pos.x, pos.y, pos.z, ntile)
                ntile.loadFrom(tile)
            }
        }
        else
        {
            world.setBlock(pos.x, pos.y, pos.z, MultipartProxy.block.blockID)
            ntile = factory.generateTile(partTraits, world.isRemote)
            world.setBlockTileEntity(pos.x, pos.y, pos.z, ntile)
        }
        ntile.addPart(part)
        return ntile
    }
    
    /**
     * Check if tile satisfies all the interfaces required by parts. If not, return a new generated copy of tile
     */
    private[multipart] def generateCompositeTile(tile:TileEntity, parts:Seq[TMultiPart], client:Boolean):TileMultipart = 
    {
        var partTraits = parts.flatMap(traitsForPart(_, client)).distinct
        if(tile != null && tile.isInstanceOf[TileMultipart])
        {
            var tileTraits = tileTraitMap(tile.getClass)
            if(partTraits.forall(tileTraits(_)) && partTraits.size == tileTraits.size)//equal contents
                return tile.asInstanceOf[TileMultipart]
            
        }
        return factory.generateTile(partTraits, client)
    }
    
    /**
     * Check if there are any redundant interfaces on tile, if so, replace tile with new copy
     */
    private[multipart] def partRemoved(tile:TileMultipart, part:TMultiPart):TileMultipart = 
    {
        val client = tile.worldObj.isRemote
        var partTraits = tile.partList.flatMap(traitsForPart(_, client))
        var testSet = partTraits.toSet
        if(!traitsForPart(part, client).forall(testSet(_)))
        {
            val ntile = factory.generateTile(testSet.toSeq, client)
            tile.worldObj.setBlockTileEntity(tile.xCoord, tile.yCoord, tile.zCoord, ntile)
            ntile.loadFrom(tile)
            return ntile
        }
        return tile
    }
    
    /**
     * register s_trait to be applied to tiles containing parts implementing s_interface
     */
    def registerTrait(s_interface:String, s_trait:String):Unit = registerTrait(s_interface, s_trait, s_trait)
    
    /**
     * register traits to be applied to tiles containing parts implementing s_interface
     * s_trait for server worlds (may be null)
     * c_trait for client worlds (may be null)
     */
    def registerTrait(s_interface:String, c_trait:String, s_trait:String)
    {
        if(c_trait != null)
        {
            if(interfaceTraitMap_c.contains(s_interface))
                System.err.println("Trait already registered for "+s_interface)
            else
                interfaceTraitMap_c = interfaceTraitMap_c+(s_interface->c_trait)
        }
        if(s_trait != null)
        {
            if(interfaceTraitMap_s.contains(s_interface))
                System.err.println("Trait already registered for "+s_interface)
            else
                interfaceTraitMap_s = interfaceTraitMap_s+(s_interface->s_trait)
        }
    }
    
    def registerPassThroughInterface(s_interface:String):Unit = registerPassThroughInterface(s_interface, true, true)
    
    def registerPassThroughInterface(s_interface:String, client:Boolean, server:Boolean)
    {
        val tType = factory.generatePassThroughTrait(s_interface)
        if(client)
        {
            if(interfaceTraitMap_c.contains(s_interface))
                System.err.println("Trait already registered for "+s_interface)
            else
                interfaceTraitMap_c = interfaceTraitMap_c+(s_interface->tType)
        }
        if(server)
        {
            if(interfaceTraitMap_s.contains(s_interface))
                System.err.println("Trait already registered for "+s_interface)
            else
                interfaceTraitMap_s = interfaceTraitMap_s+(s_interface->tType)
        }
    }
    
    def registerTileClass(clazz:Class[_ <: TileEntity], traits:Set[String])
    {
        tileTraitMap=tileTraitMap+(clazz->traits)
        MultipartProxy.onTileClassBuilt(clazz)
    }
}
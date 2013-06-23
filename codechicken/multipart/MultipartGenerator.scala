package codechicken.multipart

import scala.reflect.runtime.universe._
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
    var mirror = scala.reflect.runtime.currentMirror
    
    private var tileTraitMap:Map[Class[_], Set[Type]] = Map()
    private var interfaceTraitMap_c:Map[Type, Type] = Map()
    private var interfaceTraitMap_s:Map[Type, Type] = Map()
    private var partTraitMap_c:Map[Class[_], Seq[Type]] = Map()
    private var partTraitMap_s:Map[Class[_], Seq[Type]] = Map()
    
    var factory:IMultipartFactory = ScalaCompilerFactory
    
    def getType(obj:Any) = mirror.classSymbol(obj.getClass).toType
    
    def partTraitMap(client:Boolean) = if(client) partTraitMap_c else partTraitMap_s
    
    def interfaceTraitMap(client:Boolean) = if(client) partTraitMap_c else interfaceTraitMap_s
    
    def traitsForPart(part:TMultiPart, client:Boolean):Seq[Type] = 
    {
        var ret = partTraitMap(client).getOrElse(part.getClass, null)
        if(ret == null)
        {
            if(client)
            {
                ret = getType(part).baseClasses.flatMap(s => interfaceTraitMap_c.get(s.asClass.toType)).distinct
                partTraitMap_c = partTraitMap_c+(part.getClass -> ret)
            }
            else
            {
                ret = getType(part).baseClasses.flatMap(s => interfaceTraitMap_s.get(s.asClass.toType)).distinct
                partTraitMap_s = partTraitMap_s+(part.getClass -> ret)
            }
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
        val iSymbol = mirror.staticClass(s_interface).asClass
        if(c_trait != null)
        {
            val tSymbol = mirror.staticClass(c_trait).asClass
            if(interfaceTraitMap_c.contains(iSymbol.toType))
                System.err.println("Trait already registered for "+iSymbol)
            else
                interfaceTraitMap_c = interfaceTraitMap_c+(iSymbol.toType->tSymbol.toType)
        }
        if(s_trait != null)
        {
            val tSymbol = mirror.staticClass(s_trait).asClass
            if(interfaceTraitMap_s.contains(iSymbol.toType))
                System.err.println("Trait already registered for "+iSymbol)
            else
                interfaceTraitMap_s = interfaceTraitMap_s+(iSymbol.toType->tSymbol.toType)
        }
    }
    
    def registerPassThroughInterface(s_interface:String):Unit = registerPassThroughInterface(s_interface, true, true)
    
    def registerPassThroughInterface(s_interface:String, client:Boolean, server:Boolean)
    {
        val iSymbol = mirror.staticClass(s_interface).asClass
        val tType = factory.generatePassThroughTrait(iSymbol)
        if(client)
        {
            if(interfaceTraitMap_c.contains(iSymbol.toType))
                System.err.println("Trait already registered for "+iSymbol)
            else
                interfaceTraitMap_c = interfaceTraitMap_c+(iSymbol.toType->tType)
        }
        if(server)
        {
            if(interfaceTraitMap_s.contains(iSymbol.toType))
                System.err.println("Trait already registered for "+iSymbol)
            else
                interfaceTraitMap_s = interfaceTraitMap_s+(iSymbol.toType->tType)
        }
    }
    
    def registerTileClass(clazz:Class[_ <: TileEntity], traits:Set[Type])
    {
        tileTraitMap=tileTraitMap+(clazz->traits)
        MultipartProxy.onTileClassBuilt(clazz)
    }
}
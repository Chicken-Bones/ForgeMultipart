package codechicken.multipart.handler

import codechicken.multipart.BlockMultipart
import cpw.mods.fml.common.network.PacketDispatcher
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.client.registry.ClientRegistry
import net.minecraft.tileentity.TileEntity
import codechicken.core.config.ConfigFile
import cpw.mods.fml.common.registry.GameRegistry
import java.io.File
import codechicken.multipart.handler.MultipartProxy._
import codechicken.multipart.BlockMultipartImpl
import codechicken.core.CommonUtils
import codechicken.multipart.MultipartRenderer
import java.util.HashMap
import net.minecraftforge.common.MinecraftForge
import codechicken.multipart.MultipartGenerator
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.core.packet.PacketCustom
import codechicken.multipart.TileMultipart
import cpw.mods.fml.client.registry.KeyBindingRegistry
import codechicken.multipart.ControlKeyHandler
import cpw.mods.fml.common.network.NetworkRegistry
import cpw.mods.fml.common.registry.TickRegistry

class MultipartProxy_serverImpl
{
    def preInit(cfgdir:File)
    {
        config = new ConfigFile(new File(cfgdir, "multipart.cfg"))
            .setComment("Multipart API config file")
        
        MultipartGenerator.registerTrait("net.minecraftforge.liquids.ITankContainer", "codechicken.multipart.TLiquidTank")
        MultipartGenerator.registerTrait("codechicken.multipart.JPartialOcclusion", "codechicken.multipart.TPartialOcclusionTile")
        MultipartGenerator.registerTrait("codechicken.multipart.IRandomDisplayTick", "codechicken.multipart.TRandomDisplayTickTile", null)
    }
    
    def postInit()
    {
        block = new BlockMultipartImpl(config.getTag("block.id").getIntValue(CommonUtils.getFreeBlockID(1281)))
        block.setUnlocalizedName("ccmultipart")
        
        MinecraftForge.EVENT_BUS.register(MultipartEventHandler)
        PacketCustom.assignHandler(MultipartSPH.channel, MultipartSPH)
        NetworkRegistry.instance.registerConnectionHandler(MultipartEventHandler)
        TickRegistry.registerTickHandler(MultipartEventHandler, Side.SERVER)
    }
    
    def onTileClassBuilt(t:Class[_ <: TileEntity])
    {
        MultipartSaveLoad.registerTileClass(t)   
    }
}

class MultipartProxy_clientImpl extends MultipartProxy_serverImpl
{
    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        super.postInit()
        RenderingRegistry.registerBlockHandler(MultipartRenderer);
        PacketCustom.assignHandler(MultipartCPH.channel, MultipartCPH)
        KeyBindingRegistry.registerKeyBinding(ControlKeyHandler)
    }
    
    @SideOnly(Side.CLIENT)
    override def onTileClassBuilt(t:Class[_ <: TileEntity])
    {
        super.onTileClassBuilt(t)
        ClientRegistry.bindTileEntitySpecialRenderer(t, MultipartRenderer)
    }
}

object MultipartProxy extends MultipartProxy_clientImpl
{
    var block:BlockMultipart = _
    var config:ConfigFile = _
}
package codechicken.microblock.handler;

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.Mod.PreInit
import cpw.mods.fml.common.Mod.Init
import cpw.mods.fml.common.Mod.PostInit
import cpw.mods.fml.common.Mod.ServerAboutToStart
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.Mod.Instance
import codechicken.microblock.MicroMaterialRegistry
import codechicken.microblock.DefaultContent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import codechicken.lib.packet.PacketCustom.CustomTinyPacketHandler

@Mod(modid = "ForgeMicroblock", acceptedMinecraftVersions = "[1.6.2]", 
            dependencies="required-after:ForgeMultipart", modLanguage="scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true, tinyPacketHandler=classOf[CustomTinyPacketHandler])
object MicroblockMod
{
    @PreInit
    def preInit(event:FMLPreInitializationEvent)
    {
        MicroblockProxy.preInit()
        DefaultContent.load()
    }
    
    @Init
    def init(event:FMLInitializationEvent)
    {
        MicroblockProxy.init()
        MicroMaterialRegistry.setupIDMap()
    }
    
    @PostInit
    def postInit(event:FMLPostInitializationEvent)
    {
        MicroblockProxy.postInit()
    }
    
    @ServerAboutToStart
    def beforeServerStart(event:FMLServerAboutToStartEvent)
    {
        MicroMaterialRegistry.setupIDMap()
    }
}
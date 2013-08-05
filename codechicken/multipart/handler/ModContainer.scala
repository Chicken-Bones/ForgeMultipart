package codechicken.multipart.handler

import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.Mod.PreInit
import cpw.mods.fml.common.Mod.PostInit
import cpw.mods.fml.common.Mod.ServerAboutToStart
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent
import codechicken.multipart.MultiPartRegistry
import cpw.mods.fml.relauncher.SideOnly
import codechicken.lib.packet.PacketCustom.CustomTinyPacketHandler

@Mod(modid = "ForgeMultipart", acceptedMinecraftVersions = "[1.6.2]", 
        modLanguage="scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = true, tinyPacketHandler=classOf[CustomTinyPacketHandler])
object MultipartMod
{
    @PreInit
    def preInit(event:FMLPreInitializationEvent)
    {
        MultipartProxy.preInit(event.getModConfigurationDirectory)
    }
    
    @PostInit
    def postInit(event:FMLPostInitializationEvent)
    {
        if(MultiPartRegistry.required)
        {
            MultiPartRegistry.postInit()
            MultipartProxy.postInit()
        }
    }
    
    @ServerAboutToStart
    def beforeServerStart(event:FMLServerAboutToStartEvent)
    {
        MultiPartRegistry.beforeServerStart()
    }
}
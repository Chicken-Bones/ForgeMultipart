package codechicken.microblock.handler;

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.Mod.EventHandler
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.Mod.Instance
import codechicken.microblock.MicroMaterialRegistry
import codechicken.microblock.DefaultContent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import codechicken.microblock.ConfigContent
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import scala.collection.JavaConversions._

@Mod(modid = "ForgeMicroblock", acceptedMinecraftVersions = "[1.6.4]", 
            dependencies="required-after:ForgeMultipart;after:*", modLanguage="scala")
object MicroblockMod
{
    @EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        MicroblockProxy.preInit()
        DefaultContent.load()
        ConfigContent.parse(event.getModConfigurationDirectory)
    }
    
    @EventHandler
    def init(event:FMLInitializationEvent)
    {
        MicroblockProxy.init()
        ConfigContent.load()
    }
    
    @EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        MicroblockProxy.postInit()
        MicroMaterialRegistry.setupIDMap()
    }
    
    @EventHandler
    def beforeServerStart(event:FMLServerAboutToStartEvent)
    {
        MicroMaterialRegistry.setupIDMap()
    }
    
    @EventHandler
    def handleIMC(event:IMCEvent)
    {
        ConfigContent.handleIMC(event.getMessages)
    }
}
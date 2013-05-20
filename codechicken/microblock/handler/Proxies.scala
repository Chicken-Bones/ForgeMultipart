package codechicken.microblock.handler

import codechicken.core.CommonUtils
import net.minecraftforge.common.MinecraftForge
import codechicken.multipart.handler.MultipartProxy._
import codechicken.multipart.TMultiPart
import codechicken.multipart.MultiPartRegistry
import codechicken.microblock.FaceMicroblock
import codechicken.microblock.ItemMicroPart
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import codechicken.microblock.MicroblockClass
import net.minecraftforge.client.MinecraftForgeClient
import codechicken.microblock.ItemMicroPartRenderer
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side

class MicroblockProxy_serverImpl
{
    var item:ItemMicroPart = _
    
    def preInit()
    {
        item = new ItemMicroPart(config.getTag("itemMicro.id").getIntValue(24875))
        MinecraftForge.EVENT_BUS.register(MicroblockEventHandler)
    }
    
    def init()
    {
    }
    
    def postInit()
    {
    }
}

class MicroblockProxy_clientImpl extends MicroblockProxy_serverImpl
{
    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        MinecraftForgeClient.registerItemRenderer(item.itemID, ItemMicroPartRenderer)
    }
}

object MicroblockProxy extends MicroblockProxy_clientImpl
{
}
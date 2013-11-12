package codechicken.microblock.handler

import net.minecraftforge.client.event.TextureStitchEvent
import codechicken.microblock.MicroMaterialRegistry
import net.minecraftforge.event.ForgeSubscribe
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.LoaderState
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import net.minecraft.util.EnumMovingObjectType
import codechicken.microblock.ItemMicroPart
import codechicken.microblock.ItemMicroPartRenderer
import org.lwjgl.opengl.GL11
import codechicken.lib.render.RenderUtils
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.common.network.IConnectionHandler
import cpw.mods.fml.common.network.Player
import net.minecraft.network.INetworkManager
import net.minecraft.network.NetLoginHandler
import codechicken.lib.packet.PacketCustom
import net.minecraft.network.packet.NetHandler
import net.minecraft.network.packet.Packet1Login
import net.minecraft.server.MinecraftServer

object MicroblockEventHandler extends IConnectionHandler
{
    @ForgeSubscribe
    @SideOnly(Side.CLIENT)
    def postTextureStitch(event:TextureStitchEvent.Post)
    {
        if(event.map.textureType == 0 && Loader.instance.hasReachedState(LoaderState.POSTINITIALIZATION))
            MicroMaterialRegistry.getIdMap.foreach(e => e._2.loadIcons())
    }
    
    @ForgeSubscribe
    @SideOnly(Side.CLIENT)
    def drawBlockHighlight(event:DrawBlockHighlightEvent)
    {
        if(event.currentItem != null && event.currentItem.getItem == MicroblockProxy.itemMicro && 
                event.target != null && event.target.typeOfHit == EnumMovingObjectType.TILE)
        {
            GL11.glPushMatrix()
                RenderUtils.translateToWorldCoords(event.player, event.partialTicks)
                if(ItemMicroPartRenderer.renderHighlight(event.player.worldObj, event.player, event.currentItem, event.target))
                    event.setCanceled(true)
            GL11.glPopMatrix()
        }
    }
    
    def connectionReceived(loginHandler:NetLoginHandler, netManager:INetworkManager):String = 
    {
        val packet = new PacketCustom(MicroblockSPH.registryChannel, 1)
        MicroMaterialRegistry.writeIDMap(packet)
        netManager.addToSendQueue(packet.toPacket)
        return null
    }
    
    def clientLoggedIn(netHandler:NetHandler, netManager:INetworkManager, packet:Packet1Login){}
    def playerLoggedIn(player:Player, netHandler:NetHandler, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:String, port:Int, netManager:INetworkManager){}
    def connectionOpened(netHandler:NetHandler, server:MinecraftServer, netManager:INetworkManager){}
    def connectionClosed(netManager:INetworkManager){}
}
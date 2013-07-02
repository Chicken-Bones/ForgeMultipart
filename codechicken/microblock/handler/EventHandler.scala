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
import codechicken.core.render.RenderUtils
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side

object MicroblockEventHandler
{
    @ForgeSubscribe
    @SideOnly(Side.CLIENT)
    def postTextureStitch(event:TextureStitchEvent.Post)
    {
        if(Loader.instance.hasReachedState(LoaderState.POSTINITIALIZATION))
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
}
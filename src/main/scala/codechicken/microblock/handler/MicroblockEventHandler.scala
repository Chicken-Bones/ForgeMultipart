package codechicken.microblock.handler

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import net.minecraftforge.client.event.TextureStitchEvent
import codechicken.microblock.MicroMaterialRegistry
import net.minecraftforge.client.event.DrawBlockHighlightEvent
import org.lwjgl.opengl.GL11
import codechicken.lib.render.RenderUtils
import codechicken.microblock.ItemMicroPartRenderer
import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.LoaderState
import net.minecraft.util.MovingObjectPosition.MovingObjectType

object MicroblockEventHandler {
  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  def postTextureStitch(event: TextureStitchEvent.Post) {
    if (event.map.getTextureType == 0)
      MicroMaterialRegistry.loadIcons()
  }

  @SubscribeEvent
  @SideOnly(Side.CLIENT)
  def drawBlockHighlight(event: DrawBlockHighlightEvent) {
    if (
      event.currentItem != null && event.currentItem.getItem == MicroblockProxy.itemMicro &&
      event.target != null && event.target.typeOfHit == MovingObjectType.BLOCK
    ) {
      GL11.glPushMatrix()
      RenderUtils.translateToWorldCoords(event.player, event.partialTicks)
      if (
        ItemMicroPartRenderer.renderHighlight(
          event.player,
          event.currentItem,
          event.target
        )
      )
        event.setCanceled(true)
      GL11.glPopMatrix()
    }
  }
}

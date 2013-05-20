package codechicken.multipart.minecraft;

import codechicken.core.packet.PacketCustom;
import codechicken.core.raytracer.RayTracer;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Vector3;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TileMultipart;
import codechicken.multipart.TileMultipartObj;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet15Place;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

public class EventHandler
{
    @ForgeSubscribe
    public void playerInteract(PlayerInteractEvent event)
    {
        MovingObjectPosition hit = RayTracer.reTrace(event.entityPlayer.worldObj, event.entityPlayer);
        if(event.action == Action.RIGHT_CLICK_BLOCK && place(event.entityPlayer, event.entityPlayer.worldObj, hit))
            event.setCanceled(true);
    }
    
    public boolean place(EntityPlayer player, World world, MovingObjectPosition hit)
    {
        BlockCoord pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ).offset(hit.sideHit);
        ItemStack held = player.getHeldItem();
        TileMultipart tile = TileMultipartObj.getOrConvertTile(world, pos);
        if(tile == null)
            return false;
        
        TMultiPart part = null;
        if(held.itemID == Block.torchWood.blockID)
            part = TorchPart.placement(world, pos, hit.sideHit);
        
        if(part == null)
            return false;
        if(tile.canAddPart(part))
        {
            if(!world.isRemote)
            {
                TileMultipartObj.addPart(world, pos, part);
                if(!player.capabilities.isCreativeMode)
                {
                    held.stackSize--;
                    if (held.stackSize == 0)
                    {
                        player.inventory.mainInventory[player.inventory.currentItem] = null;
                        MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, held));
                    }
                }
            }
            else
            {
                player.swingItem();
                Vector3 f = new Vector3(hit.hitVec).add(-hit.blockX, -hit.blockY, -hit.blockZ);
                PacketCustom.sendToServer(new Packet15Place(
                        hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, 
                        player.inventory.getCurrentItem(), 
                        (float)f.x, (float)f.y, (float)f.z));
            }
            return true;
        }
        return false;
    }
}

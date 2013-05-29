package codechicken.multipart.minecraft;

import codechicken.core.packet.PacketCustom;
import codechicken.core.raytracer.RayTracer;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Vector3;
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
        if(event.action == Action.RIGHT_CLICK_BLOCK && event.entityPlayer.worldObj.isRemote && place(event.entityPlayer, event.entityPlayer.worldObj))
            event.setCanceled(true);
    }
    
    public static boolean place(EntityPlayer player, World world)
    {
        MovingObjectPosition hit = RayTracer.reTrace(world, player);
        BlockCoord pos = new BlockCoord(hit.blockX, hit.blockY, hit.blockZ).offset(hit.sideHit);
        ItemStack held = player.getHeldItem();
        McBlockPart part = null;
        if(held == null)
            return false;
        
        if(held.itemID == Block.torchWood.blockID)
            part = TorchPart.placement(world, pos, hit.sideHit);
        
        if(part == null)
            return false;

        if(world.isRemote && !player.isSneaking())//attempt to use block activated like normal and tell the server the right stuff
        {
            Vector3 f = new Vector3(hit.hitVec).add(-hit.blockX, -hit.blockY, -hit.blockZ);
            Block block = Block.blocksList[world.getBlockId(hit.blockX, hit.blockY, hit.blockZ)];
            if(block != null && block.onBlockActivated(world, hit.blockX, hit.blockY, hit.blockZ, player, hit.sideHit, (float)f.x, (float)f.y, (float)f.z))
            {
                player.swingItem();
                PacketCustom.sendToServer(new Packet15Place(
                        hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, 
                        player.inventory.getCurrentItem(), 
                        (float)f.x, (float)f.y, (float)f.z));
                return false;
            }
        }
        
        TileMultipart tile = TileMultipartObj.getOrConvertTile(world, pos);
        if(tile == null || !tile.canAddPart(part))
            return false;
        
        if(!world.isRemote)
        {
            TileMultipartObj.addPart(world, pos, part);
            world.playSoundEffect(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5, 
                    part.getBlock().stepSound.getPlaceSound(), 
                    (part.getBlock().stepSound.getVolume() + 1.0F) / 2.0F, 
                    part.getBlock().stepSound.getPitch() * 0.8F);
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
            new PacketCustom(McMultipartSPH.channel, 1).sendToServer();
        }
        return true;
    }
}

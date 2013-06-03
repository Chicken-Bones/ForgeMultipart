package codechicken.multipart.minecraft;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Cuboid6;
import codechicken.core.vec.Vector3;
import codechicken.multipart.IRandomUpdateTick;
import codechicken.multipart.IRedstonePart;

import static net.minecraft.util.Facing.*;

public class RedstoneTorchPart extends TorchPart implements IRedstonePart, IRandomUpdateTick
{
    public static BlockRedstoneTorch torchActive = (BlockRedstoneTorch) Block.torchRedstoneActive;
    public static BlockRedstoneTorch torchIdle = (BlockRedstoneTorch) Block.torchRedstoneIdle;
    
    public class BurnoutEntry
    {
        public BurnoutEntry(long l)
        {
            timeout = l;
        }
        
        long timeout;
        BurnoutEntry next;
    }
    
    private BurnoutEntry burnout;
    
    public RedstoneTorchPart()
    {
    }
    
    public RedstoneTorchPart(int meta)
    {
        super(meta);
    }
    
    @Override
    public Block getBlock()
    {
        return active() ? torchActive : torchIdle;
    }
    
    public boolean active()
    {
        return (meta&0x10) > 0;
    }
    
    @Override
    public String getType()
    {
        return "mc_redtorch";
    }
    
    @Override
    public int sideForMeta(int meta)
    {
        return super.sideForMeta(meta&7);
    }
    
    @Override
    public Cuboid6 getBounds()
    {
        return getBounds(meta&7);
    }
    
    public static McBlockPart placement(World world, BlockCoord pos, int side)
    {
        if(side == 0)
            return null;
        pos = pos.copy().offset(side^1);
        if(!world.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side)))
            return null;
        
        return new RedstoneTorchPart(sideMetaMap[side^1]|0x10);
    }
    
    @Override
    public void randomDisplayTick(Random random)
    {
        if(!active())
            return;
        
        double d0 = getTile().xCoord + 0.5 + (random.nextFloat() - 0.5) * 0.2;
        double d1 = getTile().yCoord + 0.7 + (random.nextFloat() - 0.5) * 0.2;
        double d2 = getTile().zCoord + 0.5 + (random.nextFloat() - 0.5) * 0.2;
        double d3 = 0.22D;
        double d4 = 0.27D;
        
        World world = getTile().worldObj;
        int m = meta&7;
        if (m == 1)
            world.spawnParticle("reddust", d0 - d4, d1 + d3, d2, 0, 0, 0);
        else if (m == 2)
            world.spawnParticle("reddust", d0 + d4, d1 + d3, d2, 0, 0, 0);
        else if (m == 3)
            world.spawnParticle("reddust", d0, d1 + d3, d2 - d4, 0, 0, 0);
        else if (m == 4)
            world.spawnParticle("reddust", d0, d1 + d3, d2 + d4, 0, 0, 0);
        else
            world.spawnParticle("reddust", d0, d1, d2, 0, 0, 0);
    }
    
    @Override
    public ItemStack pickItem(MovingObjectPosition hit)
    {
        return new ItemStack(torchActive);
    }
    
    @Override
    public void onNeighbourChanged()
    {
        if(!getTile().worldObj.isRemote)
        {
            if(!dropIfCantStay() && isBeingPowered() == active())
                scheduleTick(2);
        }
    }
    
    public boolean isBeingPowered()
    {
        int side = metaSideMap[meta&7];
        return getTile().worldObj.getIndirectPowerOutput(
                getTile().xCoord+offsetsXForSide[side], getTile().yCoord+offsetsYForSide[side], getTile().zCoord+offsetsZForSide[side], side);
    }
    
    @Override
    public void scheduledTick()
    {
        if(isBeingPowered() == active())
            toggle();
    }
    
    public void randomUpdate()
    {
        if(!active() && !isBeingPowered())
            scheduledTick();
    }
    
    private boolean burnedOut(boolean add)
    {
        long time = getTile().worldObj.getWorldTime();
        while(burnout != null && burnout.timeout <= time)
            burnout = burnout.next;
        
        if(add)
        {
            BurnoutEntry e = new BurnoutEntry(getTile().worldObj.getWorldTime()+60);
            if(burnout == null)
                burnout = e;
            else
            {
                BurnoutEntry b = burnout;
                while(b.next != null)
                    b = b.next;
                b.next = e;
            }
        }

        if(burnout == null)
            return false;
        
        int i = 0;
        BurnoutEntry b = burnout;
        while(b != null)
        {
            i++;
            b = b.next;
        }
        return i >= 8;
    }
    
    private void toggle()
    {
        if(active())//deactivating
        {
            if(burnedOut(true))
            {
                Vector3 pos = Vector3.fromTileEntityCenter(getTile());
                World world = getTile().worldObj;
                Random rand = world.rand;
                world.playSoundEffect(pos.x+0.5, pos.y+0.5, pos.z+0.5, "random.fizz", 0.5F, 2.6F + (rand.nextFloat() - rand.nextFloat()) * 0.8F);
                
                for (int l = 0; l < 5; ++l)
                    getTile().worldObj.spawnParticle("smoke", 
                            pos.x + rand.nextDouble() * 0.6 + 0.2, 
                            pos.y + rand.nextDouble() * 0.6 + 0.2, 
                            pos.z + rand.nextDouble() * 0.6 + 0.2, 0, 0, 0);
            }
        }
        else if(burnedOut(false))
        {
            return;
        }
        
        meta ^= 0x10;
        sendDescUpdate();
        tile().markDirty();
        tile().notifyPartChange();
        tile().notifyNeighborChange(1);
    }

    @Override
    public void onRemoved()
    {
        if(active())
            tile().notifyNeighborChange(1);
    }
    
    @Override
    public void onAdded()
    {
        if(active())
            tile().notifyNeighborChange(1);
        onNeighbourChanged();
    }

    @Override
    public int strongPowerLevel(int side)
    {
        return side == 1 && active() ? 15 : 0;
    }

    @Override
    public int weakPowerLevel(int side)
    {
        return active() && side != metaSideMap[meta&7] ? 15 : 0;
    }
    
    @Override
    public boolean canConnectRedstone(int side)
    {
        return true;
    }
}

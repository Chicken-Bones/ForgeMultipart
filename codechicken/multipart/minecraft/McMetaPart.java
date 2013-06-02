package codechicken.multipart.minecraft;

import codechicken.core.data.MCDataInput;
import codechicken.core.data.MCDataOutput;
import codechicken.core.vec.BlockCoord;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class McMetaPart extends McBlockPart implements IPartMeta
{
    public byte meta;
    
    public McMetaPart()
    {
    }
    
    public McMetaPart(int meta)
    {
        this.meta = (byte)meta;
    }
    
    @Override
    public void save(NBTTagCompound tag)
    {
        tag.setByte("meta", meta);
    }
    
    @Override
    public void load(NBTTagCompound tag)
    {
        meta = tag.getByte("meta");
    }
    
    @Override
    public void writeDesc(MCDataOutput packet)
    {
        packet.writeByte(meta);
    }
    
    @Override
    public void readDesc(MCDataInput packet)
    {
        meta = packet.readByte();
    }
    
    @Override
    public World getWorld()
    {
        return getTile().worldObj;
    }
    
    @Override
    public int getMetadata()
    {
        return meta;
    }
    
    @Override
    public int getBlockId()
    {
        return getBlock().blockID;
    }
    
    @Override
    public BlockCoord getPos()
    {
        return new BlockCoord(getTile());
    }
}

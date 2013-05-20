package codechicken.multipart.minecraft;

import net.minecraft.world.World;

public interface IPartMeta
{
    public int getMetadata();
    
    public World getWorld();

    public int getBlockId();
}
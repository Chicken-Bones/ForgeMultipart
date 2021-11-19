package codechicken.multipart.minecraft;

import codechicken.lib.vec.BlockCoord;
import net.minecraft.block.Block;
import net.minecraft.world.World;

public interface IPartMeta
{
    public int getMetadata();
    
    public World getWorld();

    public Block getBlock();
    
    public BlockCoord getPos();
}
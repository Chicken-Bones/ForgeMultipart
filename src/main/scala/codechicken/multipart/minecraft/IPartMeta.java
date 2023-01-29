package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;

public interface IPartMeta {

    public int getMetadata();

    public World getWorld();

    public Block getBlock();

    public BlockCoord getPos();
}

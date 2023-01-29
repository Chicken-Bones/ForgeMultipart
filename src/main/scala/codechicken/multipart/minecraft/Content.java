package codechicken.multipart.minecraft;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import codechicken.lib.vec.BlockCoord;
import codechicken.multipart.MultiPartRegistry;
import codechicken.multipart.MultiPartRegistry.IPartConverter;
import codechicken.multipart.MultiPartRegistry.IPartFactory;
import codechicken.multipart.TMultiPart;

public class Content implements IPartFactory, IPartConverter {

    @Override
    public TMultiPart createPart(String name, boolean client) {
        if (name.equals("mc_torch")) return new TorchPart();
        if (name.equals("mc_lever")) return new LeverPart();
        if (name.equals("mc_button")) return new ButtonPart();
        if (name.equals("mc_redtorch")) return new RedstoneTorchPart();

        return null;
    }

    public void init() {
        MultiPartRegistry.registerConverter(this);
        MultiPartRegistry.registerParts(this, new String[] { "mc_torch", "mc_lever", "mc_button", "mc_redtorch" });
    }

    @Override
    public Iterable<Block> blockTypes() {
        return Arrays.asList(
                Blocks.torch,
                Blocks.lever,
                Blocks.stone_button,
                Blocks.wooden_button,
                Blocks.redstone_torch,
                Blocks.unlit_redstone_torch);
    }

    @Override
    public TMultiPart convert(World world, BlockCoord pos) {
        Block b = world.getBlock(pos.x, pos.y, pos.z);
        int meta = world.getBlockMetadata(pos.x, pos.y, pos.z);
        if (b == Blocks.torch) return new TorchPart(meta);
        if (b == Blocks.lever) return new LeverPart(meta);
        if (b == Blocks.stone_button) return new ButtonPart(meta);
        if (b == Blocks.wooden_button) return new ButtonPart(meta | 0x10);
        if (b == Blocks.redstone_torch) return new RedstoneTorchPart(meta);
        if (b == Blocks.unlit_redstone_torch) return new RedstoneTorchPart(meta | 0x10);

        return null;
    }
}

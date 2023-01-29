package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

import codechicken.lib.vec.BlockCoord;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartMetaAccess implements IBlockAccess {

    public IPartMeta part;
    private BlockCoord pos;

    public PartMetaAccess(IPartMeta p) {
        part = p;
        pos = p.getPos();
    }

    @Override
    public Block getBlock(int i, int j, int k) {
        if (i == pos.x && j == pos.y && k == pos.z) return part.getBlock();
        return part.getWorld().getBlock(i, j, k);
    }

    @Override
    public TileEntity getTileEntity(int i, int j, int k) {
        if (i == pos.x && j == pos.y && k == pos.z) throw new IllegalArgumentException("Unsupported Operation");
        return part.getWorld().getTileEntity(i, j, k);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getLightBrightnessForSkyBlocks(int i, int j, int k, int l) {
        return part.getWorld().getLightBrightnessForSkyBlocks(i, j, k, l);
    }

    @Override
    public int getBlockMetadata(int i, int j, int k) {
        if (i == pos.x && j == pos.y && k == pos.z) return part.getMetadata() & 0xF;
        return part.getWorld().getBlockMetadata(i, j, k);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isAirBlock(int i, int j, int k) {
        throw new IllegalArgumentException("Unsupported Operation");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public BiomeGenBase getBiomeGenForCoords(int i, int j) {
        return part.getWorld().getBiomeGenForCoords(i, j);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getHeight() {
        return part.getWorld().getHeight();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean extendedLevelsInChunkCache() {
        return part.getWorld().extendedLevelsInChunkCache();
    }

    @Override
    public int isBlockProvidingPowerTo(int i, int j, int k, int l) {
        throw new IllegalArgumentException("Unsupported Operation");
    }

    @Override
    public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
        return part.getWorld().isSideSolid(x, y, z, side, _default);
    }
}

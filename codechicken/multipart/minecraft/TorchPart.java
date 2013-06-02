package codechicken.multipart.minecraft;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import codechicken.multipart.IRandomDisplayTick;
import codechicken.core.lighting.LazyLightMatrix;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Cuboid6;
import codechicken.core.vec.Vector3;

public class TorchPart extends McSidedMetaPart implements IRandomDisplayTick
{
    public static BlockTorch torch = (BlockTorch) Block.torchWood;
    public static int[] metaSideMap = new int[]{-1, 4, 5, 2, 3, 0};
    public static int[] sideMetaMap = new int[]{5, 0, 3, 4, 1, 2};
    
    public TorchPart()
    {
    }
    
    public TorchPart(int meta)
    {
        super(meta);
    }
    
    @Override
    public Block getBlock()
    {
        return torch;
    }
    
    @Override
    public String getType()
    {
        return "mc_torch";
    }
    
    @Override
    public Cuboid6 getBounds()
    {
        double d = 0.15;
        if (meta == 1)
            return new Cuboid6(0, 0.2, 0.5 - d, d * 2, 0.8, 0.5 + d);
        if (meta == 2)
            return new Cuboid6(1 - d * 2, 0.2, 0.5 - d, 1, 0.8, 0.5 + d);
        if (meta == 3)
            return new Cuboid6(0.5 - d, 0.2, 0, 0.5 + d, 0.8, d * 2);
        if (meta == 4)
            return new Cuboid6(0.5 - d, 0.2, 1 - d * 2, 0.5 + d, 0.8, 1);
        
        d = 0.1;
        return new Cuboid6(0.5 - d, 0, 0.5 - d, 0.5 + d, 0.6, 0.5 + d);
    }
    
    @Override
    public int sideForMeta(int meta)
    {
        return metaSideMap[meta];
    }
    
    public static McBlockPart placement(World world, BlockCoord pos, int side)
    {
        if(side == 0)
            return null;
        pos = pos.copy().offset(side^1);
        if(!world.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side)))
            return null;
        
        return new TorchPart(sideMetaMap[side^1]);
    }

    @Override
    public void randomDisplayTick(Random random)
    {
        double d0 = getTile().xCoord + 0.5;
        double d1 = getTile().yCoord + 0.7;
        double d2 = getTile().zCoord + 0.5;
        double d3 = 0.22D;
        double d4 = 0.27D;
        
        World world = getTile().worldObj;
        if (meta == 1)
        {
            world.spawnParticle("smoke", d0 - d4, d1 + d3, d2, 0, 0, 0);
            world.spawnParticle("flame", d0 - d4, d1 + d3, d2, 0, 0, 0);
        }
        else if (meta == 2)
        {
            world.spawnParticle("smoke", d0 + d4, d1 + d3, d2, 0, 0, 0);
            world.spawnParticle("flame", d0 + d4, d1 + d3, d2, 0, 0, 0);
        }
        else if (meta == 3)
        {
            world.spawnParticle("smoke", d0, d1 + d3, d2 - d4, 0, 0, 0);
            world.spawnParticle("flame", d0, d1 + d3, d2 - d4, 0, 0, 0);
        }
        else if (meta == 4)
        {
            world.spawnParticle("smoke", d0, d1 + d3, d2 + d4, 0, 0, 0);
            world.spawnParticle("flame", d0, d1 + d3, d2 + d4, 0, 0, 0);
        }
        else
        {
            world.spawnParticle("smoke", d0, d1, d2, 0, 0, 0);
            world.spawnParticle("flame", d0, d1, d2, 0, 0, 0);
        }
    }
    
    @Override
    public void renderStatic(Vector3 pos, LazyLightMatrix olm, int pass)
    {
        new RenderBlocks(new PartMetaAccess(this)).renderBlockTorch(torch, getTile().xCoord, getTile().yCoord, getTile().zCoord);
    }
}

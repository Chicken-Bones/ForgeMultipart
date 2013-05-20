package codechicken.multipart.minecraft;

import java.util.Random;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import codechicken.core.vec.Vector3;
import codechicken.multipart.IRandomDisplayTick;
import codechicken.multipart.IconHitEffects;
import codechicken.multipart.JIconHitEffects;
import codechicken.core.lighting.LazyLightMatrix;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TorchPartClient extends TorchPart implements IRandomDisplayTick, JIconHitEffects
{
    public TorchPartClient()
    {
    }
    
    public TorchPartClient(int meta)
    {
        super(meta);
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
    
    @Override
    public Icon getBreakingIcon(Object subPart, int side)
    {
        return torch.getIcon(0, 0);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public Icon getBrokenIcon(int side)
    {
        return torch.getIcon(0, 0);
    }
    
    @Override
    public void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer)
    {
        IconHitEffects.addHitEffects(this, hit, effectRenderer);
    }
    
    @Override
    public void addDestroyEffects(EffectRenderer effectRenderer)
    {
        IconHitEffects.addDestroyEffects(this, effectRenderer, false);
    }
}

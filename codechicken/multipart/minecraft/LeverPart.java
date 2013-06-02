package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLever;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import codechicken.core.lighting.LazyLightMatrix;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Cuboid6;
import codechicken.core.vec.Vector3;

public class LeverPart extends McSidedMetaPart
{
    public static BlockLever lever = (BlockLever) Block.lever;
    public static int[] metaSideMap = new int[]{1, 4, 5, 2, 3, 0, 0, 1};
    public static int[] sideMetaMap = new int[]{6, 0, 3, 4, 1, 2};
    public static int[] metaSwapMap = new int[]{5, 7};
    
    public LeverPart()
    {
    }
    
    public LeverPart(int meta)
    {
        super(meta);
    }
    
    @Override
    public Block getBlock()
    {
        return lever;
    }
    
    @Override
    public String getType()
    {
        return "mc_lever";
    }
    
    @Override
    public Cuboid6 getBounds()
    {
        int m = meta & 7;
        double d = 0.1875;

        if (m == 1)
            return new Cuboid6(0, 0.2, 0.5 - d, d * 2, 0.8, 0.5 + d);
        if (m == 2)
            return new Cuboid6(1 - d * 2, 0.2, 0.5 - d, 1, 0.8, 0.5 + d);
        if (m == 3)
            return new Cuboid6(0.5 - d, 0.2, 0, 0.5 + d, 0.8, d * 2);
        if (m == 4)
            return new Cuboid6(0.5 - d, 0.2, 1 - d * 2, 0.5 + d, 0.8, 1);

        d = 0.25;
        if (m == 0 || m == 7)
            return new Cuboid6(0.5 - d, 0.4, 0.5 - d, 0.5 + d, 1, 0.5 + d);
        
        return new Cuboid6(0.5 - d, 0, 0.5 - d, 0.5 + d, 0.6, 0.5 + d);
    }
    
    @Override
    public int sideForMeta(int meta)
    {
        return metaSideMap[meta&7];
    }

    public static McBlockPart placement(World world, BlockCoord pos, EntityPlayer player, int side)
    {
        pos = pos.copy().offset(side^1);
        if(!world.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side)))
            return null;
        
        int meta = sideMetaMap[side^1];
        if(side < 2 && ((int)(player.rotationYaw / 90 + 0.5) & 1) == 0)
            meta = metaSwapMap[side^1];
        
        return new LeverPart(meta);
    }
    
    @Override
    public void renderStatic(Vector3 pos, LazyLightMatrix olm, int pass)
    {
        new RenderBlocks(new PartMetaAccess(this)).renderBlockLever(lever, getTile().xCoord, getTile().yCoord, getTile().zCoord);
    }

    @Override
    public boolean activate(EntityPlayer player, MovingObjectPosition part, ItemStack item)
    {
        World world = getTile().worldObj;
        if(world.isRemote)
            return true;

        int state = meta&8;
        world.playSoundEffect(getTile().xCoord + 0.5, getTile().yCoord + 0.5, getTile().zCoord + 0.5, "random.click", 0.3F, state > 0 ? 0.6F : 0.5F);
        meta = (byte) (meta^8);
        sendDescUpdate();
        tile().notifyPartChange();
        tile().markDirty();
        return true;
    }
    
    public void drawBreaking(RenderBlocks renderBlocks)
    {
        IBlockAccess actual = renderBlocks.blockAccess;
        renderBlocks.blockAccess = new PartMetaAccess(this);
        renderBlocks.renderBlockLever(lever, getTile().xCoord, getTile().yCoord, getTile().zCoord);
        renderBlocks.blockAccess = actual;
    }
}

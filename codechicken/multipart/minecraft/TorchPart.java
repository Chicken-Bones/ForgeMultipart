package codechicken.multipart.minecraft;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTorch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import codechicken.scala.JSeq;
import codechicken.scala.ScalaBridge;
import codechicken.multipart.JCuboidPart;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.TMultiPart;
import codechicken.core.data.MCDataInput;
import codechicken.core.data.MCDataOutput;
import codechicken.core.inventory.InventoryUtils;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Cuboid6;
import codechicken.core.vec.Vector3;

public class TorchPart extends JCuboidPart implements JNormalOcclusion, IPartMeta
{
    public static BlockTorch torch = (BlockTorch) Block.torchWood;
    public static int[] metaSideMap = new int[]{-1, 4, 5, 2, 3, 0};
    public static int[] sideMetaMap = new int[]{5, 0, 3, 4, 1, 2};
    public byte meta;
    
    public TorchPart()
    {
    }
    
    public TorchPart(int meta)
    {
        this.meta = (byte)meta;
    }
    
    @Override
    public String getType()
    {
        return "mc_torch";
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
    public boolean occlusionTest(TMultiPart npart)
    {
        return NormalOcclusionTest.apply(this, npart);
    }
    
    public JSeq<Cuboid6> getOcclusionBoxes()
    {
        return ScalaBridge.seq(getBounds());
    }
    
    @Override
    public Cuboid6 getBounds()
    {
        double d = 0.15;
        if (meta == 1)
            return new Cuboid6(0, 0.2, 0.5 - d, d * 2, 0.8, 0.5 + d);
        else if (meta == 2)
            return new Cuboid6(1 - d * 2, 0.2, 0.5 - d, 1, 0.8, 0.5 + d);
        else if (meta == 3)
            return new Cuboid6(0.5 - d, 0.2, 0, 0.5 + d, 0.8, d * 2);
        else if (meta == 4)
            return new Cuboid6(0.5 - d, 0.2, 1 - d * 2, 0.5 + d, 0.8, 1);
        else
        {
            d = 0.1;
            return new Cuboid6(0.5 - d, 0, 0.5 - d, 0.5 + d, 0.6, 0.5 + d);
        }
    }
    
    @Override
    public JSeq<Cuboid6> getCollisionBoxes()
    {
        return ScalaBridge.seq();
    }
    
    public void onNeighbourChanged()
    {
        TileEntity tile = getTile();
        if(!tile.worldObj.isRemote)
        {
            BlockCoord pos = new BlockCoord(tile).offset(metaSideMap[meta]);
            if(!tile.worldObj.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(metaSideMap[meta]^1)))
                drop();
        }
    }

    public void drop()
    {
        tile().remPart(this);
        InventoryUtils.dropItem(new ItemStack(torch), getTile().worldObj, Vector3.fromTileEntityCenter(getTile()));
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
        return torch.blockID;
    }
    
    @Override
    public int getLightValue()
    {
        return Block.lightValue[getBlockId()];
    }
    
    public static TMultiPart placement(World world, BlockCoord pos, int side)
    {
        if(side == 0)
            return null;
        pos = pos.copy().offset(side^1);
        if(!world.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(side)))
            return null;
        
        return new TorchPart(sideMetaMap[side^1]);
    }
    
    @Override
    public JSeq<ItemStack> getDrops()
    {
        return ScalaBridge.seq(new ItemStack(torch));
    }
    
    @Override
    public ItemStack pickItem(MovingObjectPosition hit)
    {
        return new ItemStack(torch);
    }
    
    @Override
    public float getStrength(MovingObjectPosition hit, EntityPlayer player)
    {
        return torch.getPlayerRelativeBlockHardness(player, player.worldObj, hit.blockX, hit.blockY, hit.blockZ)*30;
    }
}

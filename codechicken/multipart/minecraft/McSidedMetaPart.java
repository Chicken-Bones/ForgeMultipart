package codechicken.multipart.minecraft;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import codechicken.core.vec.BlockCoord;
import codechicken.core.vec.Vector3;
import codechicken.multipart.TileMultipartObj;

public abstract class McSidedMetaPart extends McMetaPart
{
    public McSidedMetaPart()
    {
    }
    
    public McSidedMetaPart(int meta)
    {
        super(meta);
    }
    
    public abstract int sideForMeta(int meta);
    
    public void onNeighbourChanged()
    {
        if(!getTile().worldObj.isRemote)
            dropIfCantStay();
    }
    
    public boolean dropIfCantStay()
    {
        BlockCoord pos = new BlockCoord(getTile()).offset(sideForMeta(meta));
        if(!getTile().worldObj.isBlockSolidOnSide(pos.x, pos.y, pos.z, ForgeDirection.getOrientation(sideForMeta(meta)^1)))
        {
            drop();
            return true;
        }
        return false;
    }

    public void drop()
    {
        tile().remPart(this);
        TileMultipartObj.dropItem(new ItemStack(getBlock()), getTile().worldObj, Vector3.fromTileEntityCenter(getTile()));
    }
}

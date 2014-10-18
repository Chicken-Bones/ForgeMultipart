package codechicken.multipart.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;

import com.google.common.collect.Maps;

public class TileTrackerMap implements Map<ChunkPosition, TileEntity>
{
    private final Map<ChunkPosition, TileEntity> delegate;
    private final Chunk owner;

    public TileTrackerMap(Map<ChunkPosition, TileEntity> del, Chunk ownr) {
        owner = ownr;
        Map<ChunkPosition, TileEntity> swap = swapMulti(del);
        del.clear();
        del.putAll(swap);
        delegate = del;
    }
    
    private Map<ChunkPosition, TileEntity> swapMulti(Map<? extends ChunkPosition, ? extends TileEntity> m) {
        Map<ChunkPosition, TileEntity> swap = Maps.newHashMapWithExpectedSize(m.size());
        for (Entry<? extends ChunkPosition, ? extends TileEntity> e : m.entrySet()) {
            swap.put(e.getKey(), swap(e.getValue()));
        }
        return swap;
    }
    
    private TileEntity swap(TileEntity tile) {
        TileEntity newTile = MultipartSaveLoad.tileSwapHook(tile);
        if (newTile != tile) {
            newTile.xCoord = tile.xCoord;
            newTile.yCoord = tile.yCoord;
            newTile.zCoord = tile.zCoord;
            owner.removeTileEntity(newTile.xCoord, newTile.yCoord, newTile.zCoord);
            owner.addTileEntity(newTile);
            MultipartSPH.getDescPacket(owner, Arrays.asList(newTile).iterator())
                .sendPacketToAllAround(newTile.xCoord, newTile.yCoord, newTile.zCoord, 64, owner.worldObj.provider.dimensionId);
        }
        return newTile;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public TileEntity get(Object key) {
        return delegate.get(key);
    }

    @Override
    public TileEntity put(ChunkPosition key, TileEntity value) {
        TileEntity old = get(key);
        if (swap(value) != value) {
            return old;
        }
        return delegate.put(key, value);
    }

    @Override
    public TileEntity remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(Map<? extends ChunkPosition, ? extends TileEntity> m) {
        if (!swapMulti(m).equals(m)) {
            return;
        }
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<ChunkPosition> keySet() {
        return delegate.keySet();
    }

    @Override
    public Collection<TileEntity> values() {
        return delegate.values();
    }

    @Override
    public Set<java.util.Map.Entry<ChunkPosition, TileEntity>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }
}

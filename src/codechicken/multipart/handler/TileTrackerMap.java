package codechicken.multipart.handler;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.chunk.Chunk;

import com.google.common.collect.Maps;

public class TileTrackerMap
        extends HashMap<ChunkPosition, TileEntity> implements
        Map<ChunkPosition, TileEntity> {
    private static final long serialVersionUID = -6346731905063668635L;
    private final Chunk owner;
    private final HashMap<ChunkPosition, TileEntity> delegate;

    @SuppressWarnings("unchecked")
    public TileTrackerMap(Chunk ownr) {
        owner = ownr;
        // NB: required because some mods expect HashMap, so we do too.
        delegate = (HashMap<ChunkPosition, TileEntity>) ownr.chunkTileEntityMap;
        swapCurrentTiles();
    }

    /**
     * Swap all tiles in the chunk. Probably not safe to call after players
     * join.
     */
    private void swapCurrentTiles() {
        for (Entry<ChunkPosition, TileEntity> entry : delegate.entrySet()) {
            TileEntity before = entry.getValue();
            TileEntity after = MultipartSaveLoad.tileSwapHook(before);
            if (before != after) {
                after.setWorldObj(before.getWorldObj());
                after.validate();
                before.invalidate();
                entry.setValue(after);
            }
        }
    }

    /**
     * In-place swap of tiles in the given map.
     * 
     * @param m
     *            - map to swap tiles in
     */
    private void swapMulti(Map<ChunkPosition, TileEntity> m) {
        for (Entry<ChunkPosition, TileEntity> e : m.entrySet()) {
            TileEntity before = e.getValue();
            TileEntity after = swap(before);
            if (before != after) {
                e.setValue(after);
            }
        }
    }

    /**
     * Do a full swap of a tile, replacing it entirely.
     * 
     * @param tile
     *            - input tile
     * @return the result of attempting to swap {@code tile}.
     */
    private TileEntity swap(TileEntity tile) {
        TileEntity newTile = MultipartSaveLoad.tileSwapHook(tile);
        if (newTile != tile) {
            newTile.xCoord = tile.xCoord;
            newTile.yCoord = tile.yCoord;
            newTile.zCoord = tile.zCoord;
            owner.removeTileEntity(newTile.xCoord, newTile.yCoord,
                                   newTile.zCoord);
            owner.addTileEntity(newTile);
            MultipartSPH
                    .getDescPacket(owner, Arrays.asList(newTile).iterator())
                    .sendPacketToAllAround(newTile.xCoord, newTile.yCoord,
                                           newTile.zCoord, 64,
                                           owner.worldObj.provider.dimensionId);
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
    public TileEntity get(Object key) {
        return delegate.get(key);
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public TileEntity put(ChunkPosition key, TileEntity value) {
        return delegate.put(key, swap(value));
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public void putAll(Map<? extends ChunkPosition, ? extends TileEntity> m) {
        Map<ChunkPosition, TileEntity> copy = Maps.newHashMap(m);
        swapMulti(copy);
        delegate.putAll(copy);
    }

    @Override
    public TileEntity remove(Object key) {
        return delegate.remove(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public Object clone() {
        return delegate.clone();
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
    public Set<Entry<ChunkPosition, TileEntity>> entrySet() {
        return delegate.entrySet();
    }
}

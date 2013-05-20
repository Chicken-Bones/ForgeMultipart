package codechicken.multipart;

public enum PartMap
{
    BOTTOM(0),
    TOP(1),
    NORTH(2),
    SOUTH(3),
    WEST(4),
    EAST(5),
    CENTER(6),
    CORNER_NNN(7),
    CORNER_NPN(8),
    CORNER_NNP(9),
    CORNER_NPP(10),
    CORNER_PNN(11),
    CORNER_PPN(12),
    CORNER_PNP(13),
    CORNER_PPP(14),
    EDGE_NYN(15),
    EDGE_NYP(16),
    EDGE_PYN(17),
    EDGE_PYP(18),
    EDGE_NNZ(19),
    EDGE_PNZ(20),
    EDGE_NPZ(21),
    EDGE_PPZ(22),
    EDGE_XNN(23),
    EDGE_XPN(24),
    EDGE_XNP(25),
    EDGE_XPP(26);
    
    public final int i;
    public final int mask;
    
    private PartMap(int i)
    {
        this.i = i;
        mask = 1<<i;
    }
    
    public static PartMap face(int i)
    {
        return values()[i];
    }
    
    public static PartMap edge(int i)
    {
        return values()[i+15];
    }
    
    public static PartMap corner(int i)
    {
        return values()[i+7];
    }
    
    public static int edgeAxisMask(int e)
    {
        switch(e>>2)
        {
            case 0: return 6;
            case 1: return 5;
            case 2: return 3;
        }
        throw new IllegalArgumentException("Switch Falloff");
    }
    
    public static int unpackEdgeBits(int e)
    {
        switch(e>>2)
        {
            case 0: return (e&3)<<1;
            case 1: return (e&2)>>1|(e&1)<<2;
            case 2: return (e&3);
        }
        throw new IllegalArgumentException("Switch Falloff");
    }
    
    public static int packEdgeBits(int e, int bits)
    {
        switch(e>>2)
        {
            case 0: return e&0xC|bits>>1;
            case 1: return e&0xC|(bits&4)>>2|(bits&1)<<1;
            case 2: return e&0xC|bits&3;
        }
        throw new IllegalArgumentException("Switch Falloff");
    }
}

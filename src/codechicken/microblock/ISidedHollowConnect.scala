package codechicken.microblock

/**
 * Implement this on center parts that connect through hollow covers to have the covers render their hollows to fit the connection size
 */
trait ISidedHollowConnect
{
    /**
     * @param side The side of the block on which the cover resides
     * @return The size (width and height) of the connection in pixels. Must be be less than 12 and more than 0
     */
    def getHollowSize(side:Int):Int
}
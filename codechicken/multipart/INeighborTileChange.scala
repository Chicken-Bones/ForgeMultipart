package codechicken.multipart

trait INeighborTileChange {
    def weakTileChanges():Boolean
    
    def onNeighborTileChanged(side:Int, weak:Boolean)
}
package codechicken.microblock

trait MicroblockClass
{
    var classID:Int = _
    
    def itemSlot = 3
    
    def getName:String
    
    def create(client:Boolean):CommonMicroblock
    
    def create(size:Int, slot:Int, material:Int, client:Boolean):CommonMicroblock
    
    def placementProperties:PlacementProperties
    
    def register(id:Int) = MicroblockClassRegistry.registerMicroClass(this, id)

    def getResistanceFactor:Float
}
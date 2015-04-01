package codechicken.microblock

trait MicroblockClass
{
    var classID:Int = _
    
    def itemSlot = 3
    
    def getName:String
    
    def create(client:Boolean, material:Int):CommonMicroblock
    
    def placementProperties:PlacementProperties
    
    def register(id:Int) = MicroblockClassRegistry.registerMicroClass(this, id)

    def getResistanceFactor:Float
}
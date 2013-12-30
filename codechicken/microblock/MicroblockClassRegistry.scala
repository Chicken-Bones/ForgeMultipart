package codechicken.microblock

import codechicken.multipart.MultiPartRegistry

object MicroblockClassRegistry
{
    val classes = new Array[MicroblockClass](256)
    
    def getMicroClass(modelId:Int) = classes(modelId>>8)
    
    def registerMicroClass(mcrClass:MicroblockClass, id:Int)
    {
        if(classes(id) != null)
            throw new IllegalArgumentException("Microblock class id "+id+" is already taken by "+classes(id).getName+" when adding "+mcrClass.getName)
        
        classes(id) = mcrClass
        mcrClass.classID = id
        MultiPartRegistry.registerParts((_, c:Boolean) => mcrClass.create(c), mcrClass.getName)
    }
}
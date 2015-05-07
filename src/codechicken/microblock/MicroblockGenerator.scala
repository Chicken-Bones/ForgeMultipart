package codechicken.microblock

import codechicken.multipart.asm.ASMMixinFactory
import java.util.BitSet

object MicroblockGenerator extends ASMMixinFactory(classOf[Microblock], classOf[Int])
{
    trait IGeneratedMaterial
    {
        def addTraits(traits:BitSet, mcrClass:MicroblockClass, client:Boolean)
    }

    //scratch bitset
    private val bitset = new BitSet

    def create(mcrClass: MicroblockClass, material: Int, client: Boolean) = {
        bitset.clear()
        bitset.set(mcrClass.baseTraitId)
        if(client) bitset.set(mcrClass.clientTraitId)

        MicroMaterialRegistry.getMaterial(material) match {
            case genMat:IGeneratedMaterial => genMat.addTraits(bitset, mcrClass, client)
            case _ =>
        }

        construct(bitset, material:Integer)
    }
}

package codechicken.microblock

import codechicken.multipart.asm.{ScratchBitSet, ASMMixinFactory}
import java.util.BitSet

object MicroblockGenerator
    extends ASMMixinFactory(classOf[Microblock], classOf[Int])
    with ScratchBitSet {
  trait IGeneratedMaterial {
    def addTraits(traits: BitSet, mcrClass: MicroblockClass, client: Boolean)
  }

  def create(mcrClass: MicroblockClass, material: Int, client: Boolean) = {
    val bitset = freshBitSet
    bitset.set(mcrClass.baseTraitId)
    if (client) bitset.set(mcrClass.clientTraitId)

    MicroMaterialRegistry.getMaterial(material) match {
      case genMat: IGeneratedMaterial =>
        genMat.addTraits(bitset, mcrClass, client)
      case _ =>
    }

    construct(bitset, material: Integer)
  }
}

package codechicken.multipart.asm
import java.util.BitSet

trait ScratchBitSet {
  private val bitSets = new ThreadLocal[BitSet]

  def getBitSet = bitSets.get match {
    case null =>
      val bitset = new BitSet
      bitSets.set(bitset)
      bitset
    case bitset => bitset
  }

  def freshBitSet = {
    val bitset = getBitSet
    bitset.clear()
    bitset
  }
}

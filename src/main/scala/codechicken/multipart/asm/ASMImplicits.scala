package codechicken.multipart.asm

import java.util.BitSet

object ASMImplicits {
  implicit class ExtBitSet(val bitset: BitSet) extends AnyVal {
    def set(b: BitSet) = {
      bitset.clear()
      bitset.or(b)
      bitset
    }

    def copy = new BitSet().set(bitset)
  }

  def nodeName(name: String) =
    if (name == null) null else name.replace('.', '/')

  implicit class ExtClass(val clazz: Class[_]) extends AnyVal {
    def nodeName = ASMImplicits.nodeName(clazz.getName)
  }
}

package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import codechicken.lib.vec.Scale
import codechicken.lib.vec.Vector3
import Vector3._

object CornerPlacement extends PlacementProperties {
  def microClass = CornerMicroClass

  def placementGrid = CornerPlacementGrid

  def opposite(slot: Int, side: Int) = ((slot - 7) ^ (1 << (side >> 1))) + 7
}

object CornerMicroClass extends CommonMicroClass {
  var aBounds: Array[Cuboid6] = new Array(256)

  for (s <- 0 until 8) {
    val rx = if ((s & 4) != 0) -1 else 1
    val ry = if ((s & 1) != 0) -1 else 1
    val rz = if ((s & 2) != 0) -1 else 1
    val transform = new Scale(new Vector3(rx, ry, rz)).at(center)

    for (t <- 1 until 8) {
      val d = t / 8d
      aBounds(t << 4 | s) = new Cuboid6(0, 0, 0, d, d, d).apply(transform)
    }
  }

  def getName = "mcr_cnr"

  def baseTrait = classOf[CornerMicroblock]
  def clientTrait = classOf[CommonMicroblockClient]

  override def itemSlot = 7

  def placementProperties = CornerPlacement

  def getResistanceFactor = 1
}

trait CornerMicroblock extends CommonMicroblock {
  override def setShape(size: Int, slot: Int) = shape =
    (size << 4 | (slot - 7)).toByte

  def microClass = CornerMicroClass

  def getBounds = CornerMicroClass.aBounds(shape)

  override def getSlot = getShape + 7
}

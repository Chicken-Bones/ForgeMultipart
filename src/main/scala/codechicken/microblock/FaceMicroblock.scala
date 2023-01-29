package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import codechicken.multipart.TFacePart
import codechicken.lib.vec.Vector3
import codechicken.lib.vec.Rotation
import Rotation._
import Vector3._

object FacePlacement extends PlacementProperties {
  def microClass = FaceMicroClass

  def placementGrid = FacePlacementGrid

  def opposite(slot: Int, side: Int) = slot ^ 1

  override def expand(slot: Int, side: Int) = sneakOpposite(slot, side)

  override def sneakOpposite(slot: Int, side: Int) = slot == (side ^ 1)
}

class FaceMicroClass

object FaceMicroClass extends CommonMicroClass {
  var aBounds: Array[Cuboid6] = new Array(256)

  for (s <- 0 until 6) {
    val transform = sideRotations(s).at(center)
    for (t <- 1 until 8) {
      val d = t / 8d
      aBounds(t << 4 | s) = new Cuboid6(0, 0, 0, 1, d, 1).apply(transform)
    }
  }

  def getName = "mcr_face"

  def itemSlot = 3

  def baseTrait = classOf[FaceMicroblock]
  def clientTrait = classOf[FaceMicroblockClient]

  def placementProperties = FacePlacement

  def getResistanceFactor = 1
}

trait FaceMicroblockClient extends CommonMicroblockClient {
  override def render(pos: Vector3, pass: Int) {
    if (pass < 0)
      MicroblockRender.renderCuboid(pos, getIMaterial, pass, getBounds, 0)
    else if (isTransparent)
      MicroblockRender.renderCuboid(
        pos,
        getIMaterial,
        pass,
        renderBounds,
        renderMask
      )
    else {
      val mat = getIMaterial
      MicroblockRender.renderCuboid(
        pos,
        mat,
        pass,
        renderBounds,
        renderMask | 1 << getSlot
      )
      MicroblockRender.renderCuboid(
        pos,
        mat,
        pass,
        Cuboid6.full,
        ~(1 << getSlot)
      )
    }
  }
}

trait FaceMicroblock extends CommonMicroblock with TFacePart {
  def microClass = FaceMicroClass

  def getBounds = FaceMicroClass.aBounds(shape)

  override def solid(side: Int) = getIMaterial.isSolid
}

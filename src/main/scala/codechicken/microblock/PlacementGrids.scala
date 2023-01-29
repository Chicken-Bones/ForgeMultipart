package codechicken.microblock

import codechicken.lib.vec.Vector3
import codechicken.lib.vec.BlockCoord
import codechicken.lib.vec.Rotation
import Rotation._
import org.lwjgl.opengl.GL11._
import codechicken.multipart.PartMap

trait PlacementGrid {
  def getHitSlot(vhit: Vector3, side: Int): Int

  def render(hit: Vector3, side: Int) {
    glTransformFace(hit, side)
    glLineWidth(2)
    glColor4f(0, 0, 0, 1)
    glBegin(GL_LINES)
    drawLines()
    glEnd()
    glPopMatrix()
  }

  def drawLines() {}

  def glTransformFace(hit: Vector3, side: Int) {
    val pos = new BlockCoord(hit)
    glPushMatrix()
    glTranslated(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
    sideRotations(side).glApply()
    val rhit = new Vector3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5)
      .subtract(hit)
      .apply(sideRotations(side ^ 1).inverse)
    glTranslated(0, rhit.y - 0.002, 0)
  }
}

class FaceEdgeGrid(size: Double) extends PlacementGrid {
  override def drawLines() {
    glVertex3d(-0.5, 0, -0.5)
    glVertex3d(-0.5, 0, 0.5)

    glVertex3d(-0.5, 0, 0.5)
    glVertex3d(0.5, 0, 0.5)

    glVertex3d(0.5, 0, 0.5)
    glVertex3d(0.5, 0, -0.5)

    glVertex3d(0.5, 0, -0.5)
    glVertex3d(-0.5, 0, -0.5)

    glVertex3d(0.5, 0, 0.5)
    glVertex3d(size, 0, size)

    glVertex3d(-0.5, 0, 0.5)
    glVertex3d(-size, 0, size)

    glVertex3d(0.5, 0, -0.5)
    glVertex3d(size, 0, -size)

    glVertex3d(-0.5, 0, -0.5)
    glVertex3d(-size, 0, -size)

    glVertex3d(-size, 0, -size)
    glVertex3d(-size, 0, size)

    glVertex3d(-size, 0, size)
    glVertex3d(size, 0, size)

    glVertex3d(size, 0, size)
    glVertex3d(size, 0, -size)

    glVertex3d(size, 0, -size)
    glVertex3d(-size, 0, -size)
  }

  def getHitSlot(vhit: Vector3, side: Int) = {
    val s1 = (side + 2) % 6
    val s2 = (side + 4) % 6
    val u = vhit.copy.add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
    val v = vhit.copy.add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))

    if (Math.abs(u) < size && Math.abs(v) < size)
      side ^ 1
    else if (Math.abs(u) > Math.abs(v))
      if (u > 0) s1 else s1 ^ 1
    else if (v > 0) s2
    else s2 ^ 1
  }
}

object FacePlacementGrid extends FaceEdgeGrid(1 / 4d)

object CornerPlacementGrid extends PlacementGrid {
  override def drawLines() {
    glVertex3d(-0.5, 0, -0.5)
    glVertex3d(-0.5, 0, 0.5)

    glVertex3d(-0.5, 0, 0.5)
    glVertex3d(0.5, 0, 0.5)

    glVertex3d(0.5, 0, 0.5)
    glVertex3d(0.5, 0, -0.5)

    glVertex3d(0.5, 0, -0.5)
    glVertex3d(-0.5, 0, -0.5)

    glVertex3d(0, 0, -0.5)
    glVertex3d(0, 0, 0.5)

    glVertex3d(-0.5, 0, 0)
    glVertex3d(0.5, 0, 0)
  }

  def getHitSlot(vhit: Vector3, side: Int): Int = {
    val s1 = ((side & 6) + 3) % 6
    val s2 = ((side & 6) + 5) % 6
    val u = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
    val v = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))

    val bu = if (u >= 0) 1 else 0
    val bv = if (v >= 0) 1 else 0
    val bw = (side & 1) ^ 1

    return 7 + (bw << (side >> 1) |
      bu << (s1 >> 1) |
      bv << (s2 >> 1))
  }
}

object EdgePlacementGrid extends PlacementGrid {
  override def drawLines() {
    glVertex3d(-0.5, 0, -0.5)
    glVertex3d(-0.5, 0, 0.5)

    glVertex3d(-0.5, 0, 0.5)
    glVertex3d(0.5, 0, 0.5)

    glVertex3d(0.5, 0, 0.5)
    glVertex3d(0.5, 0, -0.5)

    glVertex3d(0.5, 0, -0.5)
    glVertex3d(-0.5, 0, -0.5)

    glVertex3d(0.25, 0, -0.5)
    glVertex3d(0.25, 0, 0.5)

    glVertex3d(-0.25, 0, -0.5)
    glVertex3d(-0.25, 0, 0.5)

    glVertex3d(-0.5, 0, 0.25)
    glVertex3d(0.5, 0, 0.25)

    glVertex3d(-0.5, 0, -0.25)
    glVertex3d(0.5, 0, -0.25)
  }

  override def getHitSlot(vhit: Vector3, side: Int): Int = {
    val s1 = (side + 2) % 6
    val s2 = (side + 4) % 6
    val u = vhit.copy.add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
    val v = vhit.copy.add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))

    if (Math.abs(u) < 4 / 16d && Math.abs(v) < 4 / 16d)
      return -1

    if (Math.abs(u) > 4 / 16d && Math.abs(v) > 4 / 16d)
      return PartMap.edgeBetween(
        if (u > 0) s1 else s1 ^ 1,
        if (v > 0) s2 else s2 ^ 1
      )

    val s =
      if (Math.abs(u) > Math.abs(v))
        if (u > 0) s1 else s1 ^ 1
      else if (v > 0) s2
      else s2 ^ 1

    return PartMap.edgeBetween(side ^ 1, s)
  }
}

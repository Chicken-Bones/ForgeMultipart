package codechicken.multipart.scalatraits

import net.minecraft.inventory.{IInventory, ISidedInventory}
import codechicken.multipart.{TMultiPart, TileMultipart}
import java.util.LinkedList
import net.minecraft.item.ItemStack
import net.minecraft.entity.player.EntityPlayer
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

trait TIInventoryTile extends TileMultipart with ISidedInventory {
  var invList = new LinkedList[IInventory]()
  var slotMap = Array[(IInventory, Int)]()

  override def copyFrom(that: TileMultipart) {
    super.copyFrom(that)
    if (that.isInstanceOf[TIInventoryTile]) {
      invList = that.asInstanceOf[TIInventoryTile].invList
      rebuildSlotMap()
    }
  }

  override def bindPart(part: TMultiPart) {
    super.bindPart(part)
    if (part.isInstanceOf[IInventory]) {
      invList += part.asInstanceOf[IInventory]
      rebuildSlotMap()
    }
  }

  override def partRemoved(part: TMultiPart, p: Int) {
    super.partRemoved(part, p)
    if (part.isInstanceOf[IInventory]) {
      invList -= part.asInstanceOf[IInventory]
      rebuildSlotMap()
    }
  }

  override def clearParts() {
    super.clearParts()
    invList.clear()
    slotMap = Array()
  }

  def rebuildSlotMap() {
    slotMap = Array.ofDim(invList.map(_.getSizeInventory).sum)
    var i = 0
    for (inv <- invList; s <- 0 until inv.getSizeInventory) {
      slotMap(i) = (inv, s)
      i += 1
    }
  }

  def getSizeInventory: Int = slotMap.length

  def getStackInSlot(i: Int) = {
    val (inv, slot) = slotMap(i)
    inv.getStackInSlot(slot)
  }

  def decrStackSize(i: Int, j: Int) = {
    val (inv, slot) = slotMap(i)
    inv.decrStackSize(slot, j)
  }

  def getStackInSlotOnClosing(i: Int) = {
    val (inv, slot) = slotMap(i)
    inv.getStackInSlotOnClosing(slot)
  }

  def setInventorySlotContents(i: Int, itemstack: ItemStack) = {
    val (inv, slot) = slotMap(i)
    inv.setInventorySlotContents(slot, itemstack)
  }

  def getInventoryName = "Multipart Inventory"

  def hasCustomInventoryName = false

  def getInventoryStackLimit = 64

  def isUseableByPlayer(entityplayer: EntityPlayer) = true

  def openInventory() {}

  def closeInventory() {}

  def isItemValidForSlot(i: Int, itemstack: ItemStack) = {
    val (inv, slot) = slotMap(i)
    inv.isItemValidForSlot(slot, itemstack)
  }

  def getAccessibleSlotsFromSide(side: Int) = {
    val buf = new ArrayBuffer[Int]()
    var base = 0
    for (inv <- invList) {
      inv match {
        case isided: ISidedInventory =>
          buf ++= isided.getAccessibleSlotsFromSide(side).map(_ + base)
        case _ =>
      }
      base += inv.getSizeInventory
    }
    buf.toArray
  }

  def canInsertItem(i: Int, itemstack: ItemStack, j: Int) = {
    val (inv, slot) = slotMap(i)
    inv match {
      case isided: ISidedInventory => isided.canInsertItem(slot, itemstack, j)
      case _                       => true
    }
  }

  def canExtractItem(i: Int, itemstack: ItemStack, j: Int) = {
    val (inv, slot) = slotMap(i)
    inv match {
      case isided: ISidedInventory => isided.canExtractItem(slot, itemstack, j)
      case _                       => true
    }
  }
}

/** To handle obfuscation issues, this is registered as a java trait.
  */
class JInventoryTile extends TileMultipart with TIInventoryTile {}

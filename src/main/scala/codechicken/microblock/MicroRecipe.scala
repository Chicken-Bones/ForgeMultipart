package codechicken.microblock

import net.minecraft.item.crafting.IRecipe
import net.minecraft.world.World
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import codechicken.microblock.handler.MicroblockProxy._
import net.minecraftforge.oredict.RecipeSorter

object MicroRecipe extends IRecipe {
  RecipeSorter.register(
    "fmp:micro",
    getClass,
    RecipeSorter.Category.SHAPED,
    "after:forge:shapelessore"
  )

  def getRecipeOutput = ItemMicroPart.create(1, 1, "tile.stone")

  def getRecipeSize = 9

  def matches(icraft: InventoryCrafting, world: World) =
    getCraftingResult(icraft) != null

  def getCraftingResult(icraft: InventoryCrafting): ItemStack = {
    var res = getHollowResult(icraft)
    if (res != null) return res
    res = getGluingResult(icraft)
    if (res != null) return res
    res = getThinningResult(icraft)
    if (res != null) return res
    res = getSplittingResult(icraft)
    if (res != null) return res
    res = getHollowFillResult(icraft)
    return res
  }

  def create(
      amount: Int,
      mcrClass: Int,
      size: Int,
      material: Int
  ): ItemStack = {
    if (size == 8) {
      val item = MicroMaterialRegistry.getMaterial(material).getItem.copy
      item.stackSize = amount
      return item
    }
    return ItemMicroPart.create(
      amount,
      mcrClass << 8 | size,
      MicroMaterialRegistry.materialName(material)
    )
  }

  def microMaterial(item: ItemStack) =
    if (item.getItem == itemMicro)
      ItemMicroPart.getMaterialID(item)
    else
      findMaterial(item)

  def microClass(item: ItemStack) =
    if (item.getItem == itemMicro)
      item.getItemDamage >> 8
    else
      0

  def microSize(item: ItemStack) =
    if (item.getItem == itemMicro)
      item.getItemDamage & 0xff
    else
      8

  def getHollowResult(icraft: InventoryCrafting): ItemStack = {
    if (icraft.getStackInRowAndColumn(1, 1) != null) return null

    val first = icraft.getStackInRowAndColumn(0, 0)
    if (first == null || first.getItem != itemMicro || microClass(first) != 0)
      return null
    val size = microSize(first)
    val material = microMaterial(first)

    for (i <- 1 to 8 if i != 4) {
      val item = icraft.getStackInSlot(i)
      if (
        item == null || item.getItem != itemMicro ||
        microMaterial(
          item
        ) != material || item.getItemDamage != first.getItemDamage
      )
        return null
    }
    return create(8, 1, size, material)
  }

  def getGluingResult(icraft: InventoryCrafting): ItemStack = {
    var size = 0
    var count = 0
    var smallest = 0
    var mcrClass = 0
    var material = 0
    for (i <- 0 until 9) {
      val item = icraft.getStackInSlot(i)
      if (item != null) {
        if (item.getItem != itemMicro) return null
        if (count == 0) {
          size = microSize(item)
          mcrClass = microClass(item)
          material = microMaterial(item)
          count = 1
          smallest = size
        } else if (
          microClass(item) != mcrClass || microMaterial(item) != material
        ) return null
        else if (mcrClass >= 2 && microSize(item) != smallest) return null
        else {
          smallest = Math.min(smallest, microSize(item))
          count += 1
          size += microSize(item)
        }
      }
    }

    if (count <= 1) return null

    mcrClass match {
      case 3 =>
        count match {
          case 2 => create(1, 0, smallest, material)
          case _ => null
        }
      case 2 =>
        count match {
          case 2 => create(1, 3, smallest, material)
          case 4 => create(1, 0, smallest, material)
          case _ => null
        }
      case 1 | 0 =>
        val base = Seq(1, 2, 4).find(s => (s & size) != 0)
        if (base.isEmpty)
          create(size / 8, 0, 8, material)
        else if (base.get <= smallest)
          null
        else
          create(size / base.get, mcrClass, base.get, material)
      case _ => null
    }
  }

  def getSaw(icraft: InventoryCrafting): (Saw, Int, Int) = {
    for (r <- 0 until 3)
      for (c <- 0 until 3) {
        val item = icraft.getStackInRowAndColumn(c, r)
        if (item != null && item.getItem.isInstanceOf[Saw])
          return (item.getItem.asInstanceOf[Saw], r, c)
      }
    return (null, 0, 0)
  }

  def canCut(saw: Saw, sawItem: ItemStack, material: Int): Boolean = {
    val sawStrength = saw.getCuttingStrength(sawItem)
    val matStrength =
      MicroMaterialRegistry.getMaterial(material).getCutterStrength
    return sawStrength >= matStrength || sawStrength == MicroMaterialRegistry.getMaxCuttingStrength
  }

  def getThinningResult(icraft: InventoryCrafting): ItemStack = {
    val (saw, row, col) = getSaw(icraft)
    if (saw == null)
      return null

    val item = icraft.getStackInRowAndColumn(col, row + 1)
    if (item == null)
      return null

    val size = microSize(item)
    val material = microMaterial(item)
    val mcrClass = microClass(item)
    if (
      size == 1 || material < 0 || !canCut(
        saw,
        icraft.getStackInRowAndColumn(col, row),
        material
      )
    )
      return null

    for (r <- 0 until 3)
      for (c <- 0 until 3)
        if (
          (c != col || r != row && r != row + 1) &&
          icraft.getStackInRowAndColumn(c, r) != null
        )
          return null

    return create(2, mcrClass, size / 2, material)
  }

  def findMaterial(item: ItemStack): Int =
    MicroMaterialRegistry.getIdMap.find { m =>
      val mitem = m._2.getItem
      item.getItem == mitem.getItem &&
      item.getItemDamage == mitem.getItemDamage &&
      ItemStack.areItemStackTagsEqual(item, mitem)
    } match {
      case None            => -1
      case Some((name, m)) => MicroMaterialRegistry.materialID(name)
    }

  val splitMap = Map(0 -> 3, 1 -> 3, 3 -> 2)
  def getSplittingResult(icraft: InventoryCrafting): ItemStack = {
    val (saw, row, col) = getSaw(icraft)
    if (saw == null) return null
    val item = icraft.getStackInRowAndColumn(col + 1, row)
    if (item == null || item.getItem != itemMicro) return null
    val mcrClass = microClass(item)
    val material = microMaterial(item)
    if (!canCut(saw, icraft.getStackInRowAndColumn(col, row), material))
      return null
    val split = splitMap.get(mcrClass)
    if (split.isEmpty) return null

    for (r <- 0 until 3)
      for (c <- 0 until 3)
        if (
          (r != row || c != col && c != col + 1) &&
          icraft.getStackInRowAndColumn(c, r) != null
        )
          return null

    return create(2, split.get, microSize(item), material)
  }

  def getHollowFillResult(icraft: InventoryCrafting): ItemStack = {
    var cover: ItemStack = null
    for (i <- 0 until 9) {
      val item = icraft.getStackInSlot(i)
      if (item != null) {
        if (item.getItem != itemMicro || cover != null || microClass(item) != 1)
          return null
        else cover = item
      }
    }
    if (cover == null) return null
    return create(1, 0, microSize(cover), microMaterial(cover))
  }
}

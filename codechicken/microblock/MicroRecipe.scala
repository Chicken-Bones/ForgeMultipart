package codechicken.microblock

import net.minecraft.item.crafting.IRecipe
import net.minecraft.world.World
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import codechicken.microblock.handler.MicroblockProxy._

object MicroRecipe extends IRecipe
{
    def getRecipeOutput():ItemStack = new ItemStack(sawStone)
    
    def getRecipeSize():Int = 9
    
    def matches(icraft:InventoryCrafting, world:World) = getCraftingResult(icraft) != null
    
    def getCraftingResult(icraft:InventoryCrafting):ItemStack =
    {
        var res = getHollowResult(icraft)
        if(res != null) return res
        res = getGluingResult(icraft)
        if(res != null) return res
        res = getThinningResult(icraft)
        if(res != null) return res
        res = getSplittingResult(icraft)
        if(res != null) return res
        res = getHollowFillResult(icraft)
        return res
    }
    
    def create(amount:Int, mcrClass:Int, size:Int, material:Int) =
    {
        ItemMicroPart.create(amount, mcrClass<<8|size, MicroMaterialRegistry.materialName(material))
    }
    
    def microMaterial(item:ItemStack) = ItemMicroPart.getMaterialID(item)
    
    def microClass(item:ItemStack) = item.getItemDamage >> 8
    
    def microSize(item:ItemStack) = item.getItemDamage & 0xFF
    
    def getHollowResult(icraft:InventoryCrafting):ItemStack =
    {
        if(icraft.getStackInRowAndColumn(1, 1) != null) return null
        
        val first = icraft.getStackInRowAndColumn(0, 0)
        if(first == null || first.getItem != itemMicro || microClass(first) != 0) return null
        val size = microSize(first)
        val material = microMaterial(first)
        
        for(i <- 1 to 8 if(i != 4))
        {
            val item = icraft.getStackInSlot(i)
            if(item == null || item.getItem != itemMicro || 
                    microMaterial(item) != material || item.getItemDamage != first.getItemDamage)
                return null
        }
        return create(8, 1, size, material)
    }
    
    def getGluingResult(icraft:InventoryCrafting):ItemStack = 
    {
        var size = 0
        var count = 0
        var smallest = 0
        var mcrClass = 0
        var material = 0
        for(i <- 0 until 9)
        {
            val item = icraft.getStackInSlot(i)
            if(item != null)
            {
                if(item.getItem != itemMicro) return null
                if(count == 0)
                {
                    size = microSize(item)
                    mcrClass = microClass(item)
                    material = microMaterial(item)
                    count = 1
                    smallest = size
                }
                else if(microClass(item) != mcrClass || microMaterial(item) != material) return null
                else if(mcrClass >= 2 && microSize(item) != smallest) return null
                else
                {
                    smallest= Math.min(count, microSize(item))
                    count+=1
                    size+=microSize(item)
                }
            }
        }
        
        if(count <= 1) return null
        
        val matName = MicroMaterialRegistry.materialName(material)
        mcrClass match {
            case 3 => count match {
                case 2 => create(1, 0, smallest, material)
                case _ => null
            } 
            case 2 => count match {
                case 2 => create(1, 3, smallest, material)
                case 4 => create(1, 0, smallest, material)
                case _ => null
            }
            case 1|0 => {
                val base = Seq(1, 2, 4).find(s => (s&size) != 0)
                if(base.isEmpty)
                {
                    val stack = MicroMaterialRegistry.getMaterial(material).getItem
                    stack.stackSize = size/8
                    stack
                }
                else if(base.get <= smallest)
                    null
                else
                    create(size/base.get, mcrClass, base.get, material)
            }
            case _ => null
        }
    }
    
    def getSaw(icraft:InventoryCrafting):(Saw, Int, Int) = 
    {
        for(r <- 0 until 3)
            for(c <- 0 until 3)
            {
                val item = icraft.getStackInRowAndColumn(c, r)
                if(item != null && item.getItem.isInstanceOf[Saw])
                    return (item.getItem.asInstanceOf[Saw], r, c)
            }
        return null
    }
    
    def canCut(saw:Saw, material:Int) = saw.getCuttingStrength >= MicroMaterialRegistry.getMaterial(material).getCutterStrength
    
    def getThinningResult(icraft:InventoryCrafting):ItemStack = 
    {
        val saw = getSaw(icraft)
        if(saw == null) return null
        val item = icraft.getStackInRowAndColumn(saw._3, saw._2+1)
        if(item == null) return null
        var size = 0
        var material = 0
        if(item.getItem == itemMicro)
        {
            size = microSize(item)
            if(size == 1) return null
            material = microMaterial(item)
        }
        else
        {
            MicroMaterialRegistry.getIdMap.find{m => val mitem = m._2.getItem; 
                    item.itemID == mitem.itemID && 
                    item.getItemDamage == mitem.getItemDamage && 
                    ItemStack.areItemStacksEqual(item, mitem)} match {
                case None => return null
                case Some((name, m)) => {
                    material = MicroMaterialRegistry.materialID(name)
                    size = 8
                }
            }
        }
        if(!canCut(saw._1, material)) return null
        
        for(r <- 0 until 3)
            for(c <- 0 until 3)
                if((c != saw._3 || r != saw._2 && r != saw._2+1) &&
                        icraft.getStackInRowAndColumn(c, r) != null)
                    return null
        
        return create(2, microClass(item), size/2, material)
    }
    
    val splitMap = Map(0 -> 3, 1 -> 3, 3 -> 2)
    def getSplittingResult(icraft:InventoryCrafting):ItemStack = 
    {
        val saw = getSaw(icraft)
        if(saw == null) return null
        val item = icraft.getStackInRowAndColumn(saw._3+1, saw._2)
        if(item == null || item.getItem != itemMicro) return null
        val mcrClass = microClass(item)
        val material = microMaterial(item)
        if(!canCut(saw._1, material)) return null
        val split = splitMap.get(mcrClass)
        if(split.isEmpty)return null
        
        for(r <- 0 until 3)
            for(c <- 0 until 3)
                if((r != saw._2 || c != saw._3 && c != saw._3+1) &&
                        icraft.getStackInRowAndColumn(c, r) != null)
                    return null
        
        return create(2, split.get, microSize(item), material)
    }
    
    def getHollowFillResult(icraft:InventoryCrafting):ItemStack = 
    {
        var cover:ItemStack = null
        for(i <- 0 until 9)
        {
            val item = icraft.getStackInSlot(i)
            if(item != null)
            {
                if(item.getItem != itemMicro || cover != null || microClass(item) != 1) return null
                else cover = item
            }
        }
        if(cover == null) return null
        return create(1, 0, microSize(cover), microMaterial(cover))
    }
}
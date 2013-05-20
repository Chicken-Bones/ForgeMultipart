package codechicken.microblock

import net.minecraft.block.Block._
import BlockMicroMaterial._
import MicroMaterialRegistry._

object DefaultContent 
{
    def load()
    {
        FaceMicroClass.register(0)
        HollowMicroClass.register(1)
        CornerMicroClass.register(2)
        EdgeMicroClass.register(3)
        PostMicroClass.register()
        
        createAndRegister(stone)
        createAndRegister(dirt)
        createAndRegister(cobblestone)
        createAndRegister(planks, 0 to 3)
        createAndRegister(wood, 0 to 3)
        createAndRegister(leaves, 0 to 3)
        createAndRegister(sponge)
        createAndRegister(glass)
        createAndRegister(blockLapis)
        createAndRegister(sandStone, 0 to 2)
        createAndRegister(cloth, 0 to 15)
        createAndRegister(blockGold)
        createAndRegister(blockIron)
        createAndRegister(brick)
        createAndRegister(bookShelf)
        createAndRegister(cobblestoneMossy)
        createAndRegister(obsidian)
        createAndRegister(blockDiamond)
        createAndRegister(ice)
        createAndRegister(snow)
        createAndRegister(blockClay)
        createAndRegister(netherrack)
        createAndRegister(slowSand)
        createAndRegister(glowStone)
        createAndRegister(stoneBrick, 0 to 3)
        createAndRegister(netherBrick)
        createAndRegister(whiteStone)
        createAndRegister(blockEmerald)
        createAndRegister(blockRedstone)
        createAndRegister(blockNetherQuartz)
        registerMaterial(new GrassMicroMaterial, grass.getUnlocalizedName)
        registerMaterial(new TopMicroMaterial(mycelium), mycelium.getUnlocalizedName)
    }
}
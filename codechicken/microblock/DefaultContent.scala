package codechicken.microblock

import net.minecraft.init.Blocks._
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
        createAndRegister(log, 0 to 3)
        createAndRegister(log2, 0 to 1, "tile.log2")
        createAndRegister(leaves, 0 to 3)
        createAndRegister(sponge)
        createAndRegister(glass)
        createAndRegister(lapis_block)
        createAndRegister(sandstone, 0 to 2)
        createAndRegister(wool, 0 to 15)
        createAndRegister(gold_block)
        createAndRegister(iron_block)
        createAndRegister(brick_block)
        createAndRegister(bookshelf)
        createAndRegister(mossy_cobblestone)
        createAndRegister(obsidian)
        createAndRegister(diamond_block)
        createAndRegister(ice)
        createAndRegister(snow)
        createAndRegister(clay)
        createAndRegister(netherrack)
        createAndRegister(soul_sand)
        createAndRegister(glowstone)
        createAndRegister(stonebrick, 0 to 3)
        createAndRegister(nether_brick)
        createAndRegister(end_stone)
        createAndRegister(emerald_block)
        createAndRegister(redstone_block)
        createAndRegister(quartz_block)
        createAndRegister(stained_hardened_clay, 0 to 15)
        createAndRegister(hardened_clay)
        createAndRegister(coal_block)
        createAndRegister(packed_ice)
        createAndRegister(stained_glass)
        registerMaterial(new GrassMicroMaterial, grass.getUnlocalizedName)
        registerMaterial(new TopMicroMaterial(mycelium), mycelium.getUnlocalizedName)
    }
}
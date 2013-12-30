package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import net.minecraft.world.World

/**
 * Wrapper class blocks/tiles/parts wanting to use the micro material system to render part of their models.
 */
trait IMicroMaterialRender {
    /**
     * May be null for inventory rendering.
     */
    def world:World
    def x:Int
    def y:Int
    def z:Int
    
    /**
     * Return the bounds of the part for texture mapping side decals like grass
     */
    def getRenderBounds:Cuboid6
}
package codechicken.microblock

import codechicken.lib.vec.Cuboid6
import net.minecraft.world.World

trait IMicroMaterialRender {
    /**
     * May be null for inventory rendering.
     */
    def world():World
    def x():Int
    def y():Int
    def z():Int
    
    /**
     * Return the bounds of the part for texture mapping side decals like grass
     */
    def getRenderBounds():Cuboid6
}
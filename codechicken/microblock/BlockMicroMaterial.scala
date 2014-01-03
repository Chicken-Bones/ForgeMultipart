package codechicken.microblock

import net.minecraft.block.Block
import codechicken.microblock.MicroMaterialRegistry.IMicroMaterial
import net.minecraft.util.Icon
import codechicken.lib.lighting.LC
import codechicken.lib.lighting.LightMatrix
import cpw.mods.fml.relauncher.SideOnly
import cpw.mods.fml.relauncher.Side
import codechicken.lib.render.MultiIconTransformation
import codechicken.lib.render.IUVTransformation
import net.minecraft.item.ItemStack
import codechicken.lib.vec.Vector3
import net.minecraft.entity.player.EntityPlayer
import codechicken.lib.render.Vertex5
import codechicken.lib.render.UV
import net.minecraft.client.renderer.Tessellator
import codechicken.lib.render.CCRenderState
import codechicken.lib.vec.Rotation
import net.minecraftforge.common.MinecraftForge
import codechicken.microblock.handler.MicroblockProxy
import net.minecraft.entity.Entity

/**
 * Standard micro material class suitable for most blocks.
 */
class BlockMicroMaterial(val block:Block, val meta:Int = 0) extends IMicroMaterial
{
    @SideOnly(Side.CLIENT)
    var icont:MultiIconTransformation = _
    
    @SideOnly(Side.CLIENT)
    override def loadIcons()
    {
        val iblock = Block.blocksList(block.blockID)//Reacquire the block instance incase a mod replaced it.
        val icons = new Array[Icon](6)
        
        if(iblock == null)
            for(i <- 0 until 6)
                icons(i) = MicroblockProxy.renderBlocks.getIconSafe(null)
        else
            for(i <- 0 until 6)
                icons(i) = MicroblockProxy.renderBlocks.getIconSafe(iblock.getIcon(i, meta))
        
        icont = new MultiIconTransformation(icons:_*)
    }
    
    def getRenderPass = if(block.canRenderInPass(1)) 1 else 0
    
    def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, part:IMicroMaterialRender)
    {
        renderMicroFace(verts, side, pos, lightMatrix, getColour(part), icont)
    }
    
    def renderMicroFace(verts:Array[Vertex5], side:Int, pos:Vector3, lightMatrix:LightMatrix, colour:Int, uvt:IUVTransformation)
    {
        val uv = new UV
        val t = Tessellator.instance
        var i = 0
        while(i < 4)
        {
            if(CCRenderState.useNormals)
            {
                val n = Rotation.axes(side%6)
                t.setNormal(n.x.toFloat, n.y.toFloat, n.z.toFloat)
            }
            val vert = verts(i)
            if(lightMatrix != null)
            {
                val lc = LC.computeO(vert.vec, side)
                if(CCRenderState.useModelColours)
                    lightMatrix.setColour(t, lc, colour)
                lightMatrix.setBrightness(t, lc)
            }
            else 
            {
                if(CCRenderState.useModelColours)
                    CCRenderState.vertexColour(colour)
            }
            uvt.transform(uv.set(vert.uv))
            t.addVertexWithUV(vert.vec.x+pos.x, vert.vec.y+pos.y, vert.vec.z+pos.z, uv.u, uv.v)
            i+=1
        }
    }
    
    def getColour(part:IMicroMaterialRender) = 
        if(part.world == null) 
            block.getBlockColor<<8|0xFF
        else 
            block.colorMultiplier(part.world, part.x, part.y, part.z)<<8|0xFF
    
    @SideOnly(Side.CLIENT)
    def getBreakingIcon(side:Int) = block.getIcon(side, meta)
    
    def getItem = new ItemStack(block, 1, meta)
    
    def getLocalizedName = getItem.getDisplayName
    
    def getStrength(player:EntityPlayer) = player.getCurrentPlayerStrVsBlock(block, false, meta)/block.blockHardness
    
    def isTransparent = !block.isOpaqueCube
    
    def getLightValue = Block.lightValue(block.blockID)
    
    def toolClasses = Seq("axe", "pickaxe", "shovel")
    
    def getCutterStrength = toolClasses.foldLeft(0)((level, tool) => Math.max(level, MinecraftForge.getBlockHarvestLevel(block, meta, tool)))
    
    def getSound = block.stepSound

    def explosionResistance(entity:Entity):Float = block.getExplosionResistance(entity)
}

/**
 * Utility functions for cleaner registry code
 */
object BlockMicroMaterial
{
    def materialKey(block:Block, meta:Int) = block.getUnlocalizedName+(if(meta > 0) "_"+meta else "")
    
    def createAndRegister(block:Block)
    {
        createAndRegister(block, 0)
    }
    
    def createAndRegister(block:Block, meta:Int)
    {
        MicroMaterialRegistry.registerMaterial(new BlockMicroMaterial(block, meta), materialKey(block, meta))
    }
    
    def createAndRegister(block:Block, meta:Seq[Int])
    {
        meta.foreach(m => createAndRegister(block, m))
    }
}
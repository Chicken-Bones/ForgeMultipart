package codechicken.microblock

import codechicken.lib.vec.Vector3
import org.lwjgl.opengl.GL11
import codechicken.lib.vec.BlockCoord
import codechicken.lib.vec.Rotation
import Rotation._

trait PlacementGrid
{
    def getHitSlot(vhit:Vector3, side:Int):Int
    
    def render(hit:Vector3, side:Int)
    
    def glTransformFace(hit:Vector3, side:Int)
    {
        val pos = new BlockCoord(hit)
        GL11.glPushMatrix()
        GL11.glTranslated(pos.x+0.5, pos.y+0.5, pos.z+0.5)
        sideRotations(side).glApply()
        val rhit = new Vector3(pos.x+0.5, pos.y+0.5, pos.z+0.5).subtract(hit).apply(sideRotations(side^1).inverse)
        GL11.glTranslated(0, rhit.y-0.002, 0)
    }
}

class FaceEdgeGrid(size:Double) extends PlacementGrid
{
    def render(hit:Vector3, side:Int)
    {
        glTransformFace(hit, side)
        
        GL11.glLineWidth(2)
        GL11.glColor4f(0, 0, 0, 1)
        GL11.glBegin(GL11.GL_LINES)
            GL11.glVertex3d(-0.5, 0,-0.5)
            GL11.glVertex3d(-0.5, 0, 0.5)
            
            GL11.glVertex3d(-0.5, 0, 0.5)
            GL11.glVertex3d( 0.5, 0, 0.5)
            
            GL11.glVertex3d( 0.5, 0, 0.5)
            GL11.glVertex3d( 0.5, 0,-0.5)
            
            GL11.glVertex3d( 0.5, 0,-0.5)
            GL11.glVertex3d(-0.5, 0,-0.5)
            
            GL11.glVertex3d(0.5, 0, 0.5)
            GL11.glVertex3d(size, 0, size)
            
            GL11.glVertex3d(-0.5, 0, 0.5)
            GL11.glVertex3d(-size, 0, size)
            
            GL11.glVertex3d(0.5, 0, -0.5)
            GL11.glVertex3d(size, 0, -size)
            
            GL11.glVertex3d(-0.5, 0, -0.5)
            GL11.glVertex3d(-size, 0, -size)
            
            GL11.glVertex3d(-size, 0,-size)
            GL11.glVertex3d(-size, 0, size)
            
            GL11.glVertex3d(-size, 0, size)
            GL11.glVertex3d( size, 0, size)
            
            GL11.glVertex3d( size, 0, size)
            GL11.glVertex3d( size, 0,-size)
            
            GL11.glVertex3d( size, 0,-size)
            GL11.glVertex3d(-size, 0,-size)
        GL11.glEnd()
        GL11.glPopMatrix()
    }
    
    def getHitSlot(vhit:Vector3, side:Int):Int = 
    {
        val s1 = (side+2)%6
        val s2 = (side+4)%6
        val u = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
        val v = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))
        
        if(Math.abs(u) < size && Math.abs(v) < size)
            return side^1
        if(Math.abs(u) > Math.abs(v))
            return if(u > 0) s1 else s1^1
        else
            return if(v > 0) s2 else s2^1
    }
}

object FacePlacementGrid extends FaceEdgeGrid(1/4D)

object CornerPlacementGrid extends PlacementGrid
{
    def render(hit:Vector3, side:Int)
    {
        glTransformFace(hit, side)
        GL11.glLineWidth(2)
        GL11.glColor4f(0, 0, 0, 1)
        GL11.glBegin(GL11.GL_LINES)
        
            GL11.glVertex3d(-0.5, 0,-0.5)
            GL11.glVertex3d(-0.5, 0, 0.5)
            
            GL11.glVertex3d(-0.5, 0, 0.5)
            GL11.glVertex3d( 0.5, 0, 0.5)
            
            GL11.glVertex3d( 0.5, 0, 0.5)
            GL11.glVertex3d( 0.5, 0,-0.5)
            
            GL11.glVertex3d( 0.5, 0,-0.5)
            GL11.glVertex3d(-0.5, 0,-0.5)
            
            GL11.glVertex3d(0, 0,-0.5)
            GL11.glVertex3d(0, 0, 0.5)
            
            GL11.glVertex3d(-0.5, 0, 0)
            GL11.glVertex3d( 0.5, 0, 0)
        
        GL11.glEnd()
        GL11.glPopMatrix()
    }
    
    def getHitSlot(vhit:Vector3, side:Int):Int = 
    {
        val s1 = ((side&6)+3)%6
        val s2 = ((side&6)+5)%6
        val u = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
        val v = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))
        
        val bu = if(u >= 0) 1 else 0
        val bv = if(v >= 0) 1 else 0
        val bw = (side&1)^1
        
        return 7+(
                bw<<(side>>1)|
                bu<<(s1>>1)|
                bv<<(s2>>1))
    }
}

object EdgePlacementGrid extends FaceEdgeGrid(1/4D)
{
    override def getHitSlot(vhit:Vector3, side:Int):Int =
    {
        val s1 = (side+2)%6
        val s2 = (side+4)%6
        val u = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s1))
        val v = vhit.copy().add(-0.5, -0.5, -0.5).scalarProject(Rotation.axes(s2))
        
        if(Math.abs(u) < 4/16D && Math.abs(v) < 4/16D)
            return -1
        var b = side&1
        if(Math.abs(u) > Math.abs(v))
        {
            if(u < 0) b^=1
            return 15+((s2&6)<<1 | b<<1 | side&1^1)
        }
        else
        {
            if(v < 0) b^=1
            return 15+((s1&6)<<1 | (side&1^1)<<1 | b)
        }
    }
}
package codechicken.microblock

import codechicken.multipart.TMultiPart
import codechicken.lib.vec.Cuboid6
import codechicken.multipart.PartMap._
import codechicken.lib.data.MCDataInput

/**
 * Suite of classes for performing microblock style occlusion and shrink rendering. Use at your own peril.
 */
object MicroOcclusion
{
    def shrink(renderBounds:Cuboid6, b:Cuboid6, side:Int) = side match
    {
        case -1 => 
        case 0 => if(renderBounds.min.y < b.max.y)renderBounds.min.y = b.max.y
        case 1 => if(renderBounds.max.y > b.min.y)renderBounds.max.y = b.min.y
        case 2 => if(renderBounds.min.z < b.max.z)renderBounds.min.z = b.max.z
        case 3 => if(renderBounds.max.z > b.min.z)renderBounds.max.z = b.min.z
        case 4 => if(renderBounds.min.x < b.max.x)renderBounds.min.x = b.max.x
        case 5 => if(renderBounds.max.x > b.min.x)renderBounds.max.x = b.min.x
    }
    
    def shrinkFrom(p:JMicroShrinkRender, other:JMicroShrinkRender, renderBounds:Cuboid6):Int =
    {
        if(shrinkTest(p, other))
        {
            shrink(renderBounds, other.getBounds, shrinkSide(p.getSlot, other.getSlot))
        }
        else if(other.getSlot < 6 && !other.isTransparent)//other gets full face, we didn't shrink, flag rendermask
        {
            if(other.getSlot match {
                case 0 => renderBounds.min.y <= 0
                case 1 => renderBounds.max.y >= 1
                case 2 => renderBounds.min.z <= 0
                case 3 => renderBounds.max.z >= 1
                case 4 => renderBounds.min.x <= 0
                case 5 => renderBounds.max.x >= 1
            })
            return 1<<other.getSlot
        }
        return 0
    }
    
    def shrink(p:JMicroShrinkRender, renderBounds:Cuboid6, m:Int):Int =
    {
        var renderMask = 0
        val part = p.asInstanceOf[TMultiPart]
        val tile = part.tile
        for(i <- 0 until m)
            if(i != p.getSlot)
                tile.partMap(i) match {
                    case other:JMicroShrinkRender => 
                        renderMask |= shrinkFrom(p, other, renderBounds)
                    case _ =>
                }
        return renderMask
    }
    
    def shrinkSide(s1:Int, s2:Int):Int = {
        if(s2 < 6)//other is a cover
            return s2
        if(s1 < 15)//both corners
        {
            val c1 = s1-7
            val c2 = s2-7
            return c1^c2 match//different axis bits will become 1, same 0
            {
                case 1 => c2&1
                case 2 => 2|(c2&2)>>1
                case 4 => 4|(c2&4)>>2
                case _ => -1
            }
        }
        if(s2 < 15)//edge, other corner
        {
            val e1 = s1-15
            val c2 = s2-7
            val ebits = unpackEdgeBits(e1)
            if((c2&edgeAxisMask(e1)) != ebits)
                return -1
            
            return (e1&0xC)>>1|(c2&(~ebits))>>(e1>>2)
        }
        //both edges
        {
            val e1 = s1-15
            val e2 = s2-15
            val e1bits = unpackEdgeBits(e1)
            val e2bits = unpackEdgeBits(e2)
            if((e1&0xC) == (e2&0xC))//same axis
            {
                return e1bits^e2bits match
                {
                    case 1 => if((e2bits&1) == 0) 0 else 1
                    case 2 => if((e2bits&2) == 0) 2 else 3
                    case 4 => if((e2bits&4) == 0) 4 else 5
                    case _ => -1
                }
            }
            else
            {
                val mask = edgeAxisMask(e1)&edgeAxisMask(e2)
                if((e1bits&mask) != (e2bits&mask))
                    return -1
                
                return e1>>2 match
                {
                    case 0 => if((e2bits&1) == 0) 0 else 1
                    case 1 => if((e2bits&2) == 0) 2 else 3
                    case 2 => if((e2bits&4) == 0) 4 else 5
                }
            }
        }
        
        throw new IllegalArgumentException("Switch Falloff")
    }
    
    def recalcBounds(p:JMicroShrinkRender, renderBounds:Cuboid6) =
    {
        if(p.getSlot < 6)
            shrink(p, renderBounds, 6)
        else if(p.getSlot < 15)
            shrink(p, renderBounds, 15)
        else 
            shrink(p, renderBounds, 27)
    }
    
    def shapePriority(slot:Int):Int = 
        if(slot < 6)
            return 2
        else if(slot < 15)
            return 1
        else
            return 0
    
    def shrinkTest(a:JMicroShrinkRender, b:JMicroShrinkRender):Boolean =
    {
        if(a.getPriorityClass != b.getPriorityClass) return a.getPriorityClass < b.getPriorityClass
        
        val shape1 = shapePriority(a.getSlot)
        val shape2 = shapePriority(b.getSlot)
        
        if(shape1 != shape2) return shape1 < shape2
        if(a.getSlot < 6)//transparency takes precedence for covers
        {
            if(a.isTransparent != b.isTransparent) return a.isTransparent
            if(a.getSize != b.getSize) return a.getSize < b.getSize
        }
        else
        {
            if(a.getSize != b.getSize) return a.getSize < b.getSize
            if(a.isTransparent != b.isTransparent) return a.isTransparent
        }
        return a.getSlot < b.getSlot
    }
}

trait JMicroShrinkRender
{
    def getPriorityClass:Int
    def getSlot:Int
    def getSize:Int
    def isTransparent:Boolean
    def getBounds:Cuboid6
}

trait TMicroOcclusionClient extends TMicroOcclusion with JMicroShrinkRender
{
    var renderBounds:Cuboid6 = _
    var renderMask:Int = _
    
    override def onPartChanged(part:TMultiPart)
    {
        super.onPartChanged(part)
        recalcBounds()
    }
    
    override def onAdded()
    {
        super.onAdded()
        recalcBounds()
    }

    override def read(packet:MCDataInput)
    {
        super.read(packet)
        recalcBounds()
    }
    
    def recalcBounds()
    {
        renderBounds = getBounds.copy
        renderMask = MicroOcclusion.recalcBounds(this, renderBounds)
    }
    
    override def getPriorityClass = 0
}

trait TMicroOcclusion extends TMultiPart
{
    import MicroOcclusion._
    
    def getSlot:Int
    def getSize:Int
    def getMaterial:Int
    def getBounds:Cuboid6
    
    override def occlusionTest(npart:TMultiPart):Boolean = 
    {
        if(!super.occlusionTest(npart))
            return false
        
        if(!npart.isInstanceOf[TMicroOcclusion])
            return true
        
        val mpart = npart.asInstanceOf[TMicroOcclusion]
        val shape1 = shapePriority(getSlot)
        val shape2 = shapePriority(mpart.getSlot)
        
        if(mpart.getSize + getSize > 8)//intersecting if opposite
        {
            if(shape1 == 2 && shape2 == 2)
                if(mpart.getSlot == (getSlot^1))
                    return false
            
            if(mpart.getMaterial != getMaterial)
            {
                if(shape1 == 1 && shape2 == 1)
                {
                    val axisMask = (getSlot-7)^(mpart.getSlot-7)
                    if(axisMask == 3 || axisMask == 5 || axisMask == 6)
                        return false
                }
                
                if(shape1 == 0 && shape2 == 1)
                    if(!edgeCornerOcclusionTest(this, mpart))
                        return false
                
                if(shape1 == 1 && shape2 == 0)
                    if(!edgeCornerOcclusionTest(mpart, this))
                        return false
                
                if(shape1 == 0 && shape2 == 0)
                {
                    val e1 = getSlot-15
                    val e2 = mpart.getSlot-15
                    if((e1&0xC) == (e2&0xC) && ((e1&3)^(e2&3)) == 3)
                        return false
                }
            }
        }
        
        return true
    }
    
    def edgeCornerOcclusionTest(edge:TMicroOcclusion, corner:TMicroOcclusion):Boolean =
    {
        ((corner.getSlot-7)&edgeAxisMask(edge.getSlot-15)) == unpackEdgeBits(edge.getSlot-15)
    }
}
package codechicken.multipart.asm

import scala.reflect.runtime.universe._
import codechicken.multipart.TileMultipart

trait IMultipartFactory
{
    def generatePassThroughTrait(iSymbol:ClassSymbol):Type
    
    def generateTile(types:Seq[Type], client:Boolean):TileMultipart
}
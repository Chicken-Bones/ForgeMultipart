package codechicken.multipart.asm

import scala.reflect.runtime.universe._
import codechicken.multipart.TileMultipart

trait IMultipartFactory
{
    def generatePassThroughTrait(s_interface:String):String
    
    def generateTile(types:Seq[String], client:Boolean):TileMultipart
}
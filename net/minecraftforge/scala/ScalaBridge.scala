package net.minecraftforge.scala

import java.util.List
import java.util.Map
import scala.collection.JavaConverters._
import codechicken.core.vec.Cuboid6
import java.util.Collection

object ScalaBridge
{
    implicit def wrapSeq[T](seq:Seq[T]) = new ScalaSeq[T](seq)
    implicit def unwrapSeq[T](JSeq:JSeq[T]) = JSeq.seq
    
    def toSeq[T](c:List[T]):JSeq[T] = c.asScala
    def toSeq[T](a:Array[T]):JSeq[T] = a.toSeq
    
    def seq[T]():JSeq[T] = Seq()
    def seq[T](t1:T):JSeq[T] = Seq(t1)
    def seq[T](t1:T, t2:T):JSeq[T] = Seq(t1, t2)
    def seq[T](t1:T, t2:T, t3:T):JSeq[T] = Seq(t1, t2, t3)
    def seq[T](t1:T, t2:T, t3:T, t4:T):JSeq[T] = Seq(t1, t2, t3, t4)
}

class ScalaSeq[T](val seq:Seq[T]) extends JSeq[T]

trait JSeq[T]
{
    def seq():Seq[T]
}
package codechicken.multipart.asm

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.util.{List => JList}
import scala.collection.JavaConverters._
import scala.collection.JavaConversions._

import ScalaSignature._

object ScalaSignature {
  object Bytes {
    def apply(arr: Array[Byte]): Bytes = Bytes(arr, 0, arr.length)
  }

  case class Bytes(arr: Array[Byte], pos: Int, len: Int) {
    def reader = new ByteCodeReader(this)
    def section = arr.drop(pos).take(len)
  }

  trait Flags {
    def hasFlag(flag: Int): Boolean

    def isPrivate = hasFlag(0x00000004)
    def isProtected = hasFlag(0x00000008)
    def isAbstract = hasFlag(0x00000080)
    def isDeferred = hasFlag(0x00000100)
    // abstract for methods
    def isMethod = hasFlag(0x00000200)
    def isModule = hasFlag(0x00000400)
    // object module class
    def isInterface = hasFlag(0x00000800)
    def isParam = hasFlag(0x00002000)
    def isStatic = hasFlag(0x00800000)
    def isTrait = hasFlag(0x02000000)
    def isAccessor = hasFlag(0x08000000)
  }
}

class ScalaSignature(val bytes: Bytes) {
  val major = bytes.arr(0).toInt
  val minor = bytes.arr(1).toInt
  val table = {
    val bcr = bytes.reader
    bcr.pos = 2
    Array.tabulate(bcr.readNat) { i =>
      val start = bcr.pos
      val tpe = bcr.readByte
      val len = bcr.readNat
      bcr.advance(len)(new SigEntry(i, start, Bytes(bytes.arr, bcr.pos, len)))
    }
  }

  trait SymbolRef extends Flags {
    def full: String
    def flags: Int
    def hasFlag(flag: Int) = (flags & flag) != 0
  }

  trait ClassSymbolRef extends SymbolRef {
    def name: String
    def owner: SymbolRef
    def flags: Int
    def infoId: Int

    def full = owner.full + "." + name
    override def toString = getClass.getName.replaceAll(".+\\$", "") +
      "(" + name + "," + owner + "," + flags.toHexString + "," + infoId + ")"

    def isObject = false
    def info: ClassType = evalT(infoId)
    def jParent = info.parent.jName
    def jInterfaces = info.interfaces.map(_.jName)
  }

  case class ClassSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends ClassSymbolRef

  case class ObjectSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends ClassSymbolRef {
    override def isObject = true
  }

  case class MethodSymbol(
      name: String,
      owner: SymbolRef,
      flags: Int,
      infoId: Int
  ) extends SymbolRef {
    override def toString =
      "MethodSymbol(" + name + "," + owner + "," + flags.toHexString + "," + infoId + ")"
    def full = owner.full + "." + name

    def info: TMethodType = evalT(infoId)
    def jDesc = info.jDesc
  }

  case class ExternalSymbol(name: String) extends SymbolRef {
    override def toString = name
    def full = name
    def flags = 0
  }

  case object NoSymbol extends SymbolRef {
    def full = "<no symbol>"
    def flags = 0
  }

  trait TMethodType {
    def jDesc = "(" + params
      .map(m => m.info.returnType.jDesc)
      .mkString + ")" + returnType.jDesc
    def returnType: TypeRef
    def params: List[MethodSymbol]
  }

  case class ClassType(owner: SymbolRef, parents: List[TypeRef]) {
    def parent = parents.head
    def interfaces = parents.drop(1)
  }

  case class MethodType(returnType: TypeRef, params: List[MethodSymbol])
      extends TMethodType

  case class ParameterlessType(returnType: TypeRef) extends TMethodType {
    def params = List()
  }

  trait TypeRef {
    def sym: SymbolRef
    def name = sym.full

    def jName = name.replace('.', '/') match {
      case "scala/AnyRef" | "scala/Any" => "java/lang/Object"
      case s                            => s
    }

    def jDesc = name match {
      case "scala.Array"   => null
      case "scala.Long"    => "J"
      case "scala.Int"     => "I"
      case "scala.Short"   => "S"
      case "scala.Byte"    => "B"
      case "scala.Double"  => "D"
      case "scala.Float"   => "F"
      case "scala.Boolean" => "Z"
      case "scala.Unit"    => "V"
      case _               => "L" + jName + ";"
    }
  }

  case class TypeRefType(owner: TypeRef, sym: SymbolRef, typArgs: List[TypeRef])
      extends TMethodType
      with TypeRef {
    def params = List()

    def returnType = this

    override def jDesc = name match {
      case "scala.Array" => "[" + typArgs(0).jDesc
      case _             => super.jDesc
    }
  }

  case class ThisType(sym: SymbolRef) extends TypeRef

  case class SingleType(owner: TypeRef, sym: SymbolRef) extends TypeRef {
    override def jName = super.jName + "$"
  }

  case object NoType extends TypeRef {
    def sym = null
    override def name = "<no type>"
  }

  case class SigEntry(index: Int, start: Int, bytes: Bytes) {
    def id = bytes.arr(start)
    def delete() = bytes.arr(start) = 3
    override def toString =
      "SigEntry(" + index + "," + id + "," + bytes.len + " bytes)"
  }

  trait Literal {
    def value: Any
  }
  case class BooleanLiteral(value: Boolean) extends Literal
  case class ByteLiteral(value: Byte) extends Literal
  case class ShortLiteral(value: Short) extends Literal
  case class CharLiteral(value: Char) extends Literal
  case class IntLiteral(value: Int) extends Literal
  case class LongLiteral(value: Long) extends Literal
  case class FloatLiteral(value: Float) extends Literal
  case class DoubleLiteral(value: Double) extends Literal
  case object NullLiteral extends Literal {
    override def value = null
  }
  case class StringLiteral(value: String) extends Literal
  case class TypeLiteral(value: TypeRef) extends Literal
  case class EnumLiteral(value: ExternalSymbol) extends Literal
  case class ArrayLiteral(value: List[_]) extends Literal

  case class AnnotationInfo(
      owner: SymbolRef,
      annType: TypeRef,
      values: Map[String, Literal]
  ) {
    def getValue[T](name: String) = values(name).asInstanceOf[T]
  }

  def evalS(i: Int): String = {
    val e = table(i)
    val bc = e.bytes
    val bcr = bc.reader
    e.id match {
      case 1 | 2 => bcr.readString(bc.len)
      case 3     => NoSymbol.full
      case 9 | 10 =>
        var s = evalS(bcr.readNat)
        if (bc.pos + bc.len > bcr.pos)
          s = evalS(bcr.readNat) + "." + s
        s
    }
  }

  def evalT[T](i: Int) = eval(i).asInstanceOf[T]

  def evalList[T](bcr: ByteCodeReader) = {
    val l = List.newBuilder
    while (bcr.more)
      l += evalT(bcr.readNat)
    l.result()
  }

  def eval(i: Int): Any = {
    // only parse the ones that matter for this project
    val e = table(i)
    val bcr = e.bytes.reader

    def nat = bcr.readNat
    def evalS = this.evalS(nat)
    def evalT[T] = this.evalT[T](nat)
    def evalList[T] = this.evalList[T](bcr)

    e.id match {
      case 1 | 2   => this.evalS(i)
      case 3       => NoSymbol
      case 6       => ClassSymbol(evalS, evalT, nat, nat)
      case 7       => ObjectSymbol(evalS, evalT, nat, nat)
      case 8       => MethodSymbol(evalS, evalT, nat, nat)
      case 9 | 10  => ExternalSymbol(this.evalS(i))
      case 11 | 12 => NoType // 12 is actually NoPrefixType (no lower bound)
      case 13      => ThisType(evalT)
      case 14      => SingleType(evalT, evalT)
      case 16      => TypeRefType(evalT, evalT, evalList)
      case 19      => ClassType(evalT, evalList)
      case 20      => MethodType(evalT, evalList)
      case 21 | 48 =>
        ParameterlessType(
          evalT
        ) // 48 is actually a bounded super type, but it should work fine for this project
      case 25 => BooleanLiteral(bcr.readLong != 0)
      case 26 => ByteLiteral(bcr.readLong.toByte)
      case 27 => ShortLiteral(bcr.readLong.toShort)
      case 28 => CharLiteral(bcr.readLong.toChar)
      case 29 => IntLiteral(bcr.readLong.toInt)
      case 30 => LongLiteral(bcr.readLong)
      case 31 =>
        FloatLiteral(java.lang.Float.intBitsToFloat(bcr.readLong.toInt))
      case 32 => DoubleLiteral(java.lang.Double.longBitsToDouble(bcr.readLong))
      case 33 => StringLiteral(evalS)
      case 34 => NullLiteral
      case 35 => TypeLiteral(evalT)
      case 36 => EnumLiteral(evalT)
      case 40 =>
        AnnotationInfo(
          evalT,
          evalT,
          evalList.grouped(2).map(g => (g(0), g(1))).toMap
        )
      case 44 => ArrayLiteral(evalList)
      case _  => e
    }
  }

  def collect[T](id: Int) = (0 until table.length).collect {
    case i if table(i).id == id => evalT(i): T
  }

  def findObject(name: String) = collect[ObjectSymbol](7).find(_.full == name)
  def findClass(name: String) =
    collect[ClassSymbol](6).find(c => !c.isModule && c.full == name)
}

class ByteCodeReader(val bc: Bytes) {
  var pos = bc.pos

  def more = pos < bc.pos + bc.len

  def readString(len: Int) =
    advance(len)(new String(bc.arr.drop(pos).take(len)))

  def readByte = advance(1)(bc.arr(pos))

  def readNat = {
    var r = 0
    var b = 0
    do {
      b = readByte
      r = r << 7 | b & 0x7f
    } while ((b & 0x80) != 0)
    r
  }

  def readLong = {
    var l = 0L
    while (more) {
      l <<= 8
      l |= readByte & 0xff
    }
    l
  }

  def advance[A](len: Int)(r: A) = {
    if (pos + len > bc.pos + bc.len)
      throw new IllegalArgumentException("Ran off the end of bytecode")
    pos += len
    r
  }
}

object ScalaSigReader {
  def decode(s: String) = {
    val bytes = s.getBytes
    bytes take ByteCodecs.decode(bytes)
  }

  def encode(b: Array[Byte]) = {
    val bytes = ByteCodecs.encode8to7(b)
    var i = 0
    while (i < bytes.length) {
      bytes(i) = ((bytes(i) + 1) & 0x7f).toByte
      i += 1
    }
    new String(bytes.take(bytes.length - 1), "UTF-8")
  }

  def read(ann: AnnotationNode): ScalaSignature = new ScalaSignature(
    Bytes(decode(ann.values.get(1).asInstanceOf[String]))
  )

  def write(sig: ScalaSignature, ann: AnnotationNode) =
    ann.values.set(1, encode(sig.bytes.arr))

  def ann(cnode: ClassNode): Option[AnnotationNode] =
    cnode.visibleAnnotations match {
      case null => None
      case a => a.find(ann => ann.desc.equals("Lscala/reflect/ScalaSignature;"))
    }
}

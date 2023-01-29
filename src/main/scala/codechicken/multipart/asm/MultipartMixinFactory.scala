package codechicken.multipart.asm

import ASMMixinCompiler._
import codechicken.lib.asm.ASMHelper._
import codechicken.lib.asm.{ObfMapping, CC_ClassWriter}
import org.objectweb.asm.{Label, FieldVisitor, MethodVisitor}
import org.objectweb.asm.Opcodes._
import org.objectweb.asm.tree.{MethodNode, ClassNode}
import codechicken.multipart.{MultipartGenerator, TileMultipart}
import java.util.BitSet
import ASMImplicits._

import scala.collection.JavaConversions._

object MultipartMixinFactory extends ASMMixinFactory(classOf[TileMultipart]) {
  override protected def autoCompleteJavaTrait(cnode: ClassNode) {
    if (
      !cnode.fields.isEmpty && findMethod(
        new ObfMapping(
          cnode.name,
          "copyFrom",
          "(Lcodechicken/multipart/TileMultipart;)V"
        ),
        cnode
      ) == null
    ) {
      val mv = cnode.visitMethod(
        ACC_PUBLIC,
        "copyFrom",
        "(Lcodechicken/multipart/TileMultipart;)V",
        null,
        null
      )
      mv.visitVarInsn(ALOAD, 0)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        "codechicken/multipart/TileMultipart",
        "copyFrom",
        "(Lcodechicken/multipart/TileMultipart;)V"
      )

      mv.visitVarInsn(ALOAD, 1)
      mv.visitTypeInsn(INSTANCEOF, cnode.name)
      val end = new Label()
      mv.visitJumpInsn(IFEQ, end)

      cnode.fields.foreach { f =>
        mv.visitVarInsn(ALOAD, 0)
        mv.visitVarInsn(ALOAD, 1)
        mv.visitFieldInsn(GETFIELD, cnode.name, f.name, f.desc)
        mv.visitFieldInsn(PUTFIELD, cnode.name, f.name, f.desc)
      }

      mv.visitLabel(end)
      mv.visitInsn(RETURN)
      mv.visitMaxs(2, 2)
    }
  }

  def generatePassThroughTrait(s_interface: String): String = {
    val iname = nodeName(s_interface)
    val tname = {
      var simpleName = iname.substring(iname.lastIndexOf('/') + 1)
      if (simpleName.startsWith("I")) simpleName = simpleName.substring(1)
      "T" + simpleName + "$$PassThrough"
    }
    val vname = "impl"
    val idesc = "L" + iname + ";"

    val inode = classNode(s_interface)
    if (inode == null) {
      logger.error(
        "Unable to generate pass through trait for: " + s_interface + " class not found."
      )
      return null
    }
    if ((inode.access & ACC_INTERFACE) == 0)
      throw new IllegalArgumentException(s_interface + " is not an interface.")

    val cw = new CC_ClassWriter(0)
    var mv: MethodVisitor = null
    var fv: FieldVisitor = null

    cw.visit(
      V1_6,
      ACC_PUBLIC | ACC_SUPER,
      tname,
      null,
      "codechicken/multipart/TileMultipart",
      Array(iname)
    )

    {
      fv = cw.visitField(ACC_PRIVATE, vname, idesc, null, null)
      fv.visitEnd()
    }
    {
      mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null)
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        "codechicken/multipart/TileMultipart",
        "<init>",
        "()V"
      )
      mv.visitInsn(RETURN)
      mv.visitMaxs(1, 1)
      mv.visitEnd()
    }
    {
      mv = cw.visitMethod(
        ACC_PUBLIC,
        "bindPart",
        "(Lcodechicken/multipart/TMultiPart;)V",
        null,
        null
      )
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        "codechicken/multipart/TileMultipart",
        "bindPart",
        "(Lcodechicken/multipart/TMultiPart;)V"
      )
      mv.visitVarInsn(ALOAD, 1)
      mv.visitTypeInsn(INSTANCEOF, iname)
      val l2 = new Label()
      mv.visitJumpInsn(IFEQ, l2)
      val l3 = new Label()
      mv.visitLabel(l3)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitTypeInsn(CHECKCAST, iname)
      mv.visitFieldInsn(PUTFIELD, tname, vname, idesc)
      mv.visitLabel(l2)
      mv.visitFrame(F_SAME, 0, null, 0, null)
      mv.visitInsn(RETURN)
      mv.visitMaxs(2, 2)
      mv.visitEnd()
    }
    {
      mv = cw.visitMethod(
        ACC_PUBLIC,
        "partRemoved",
        "(Lcodechicken/multipart/TMultiPart;I)V",
        null,
        null
      )
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitVarInsn(ILOAD, 2)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        "codechicken/multipart/TileMultipart",
        "partRemoved",
        "(Lcodechicken/multipart/TMultiPart;I)V"
      )
      mv.visitVarInsn(ALOAD, 1)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitFieldInsn(GETFIELD, tname, vname, idesc)
      val l2 = new Label()
      mv.visitJumpInsn(IF_ACMPNE, l2)
      val l3 = new Label()
      mv.visitLabel(l3)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitInsn(ACONST_NULL)
      mv.visitFieldInsn(PUTFIELD, tname, vname, idesc)
      mv.visitLabel(l2)
      mv.visitFrame(F_SAME, 0, null, 0, null)
      mv.visitInsn(RETURN)
      mv.visitMaxs(3, 3)
      mv.visitEnd()
    }
    {
      mv = cw.visitMethod(
        ACC_PUBLIC,
        "canAddPart",
        "(Lcodechicken/multipart/TMultiPart;)Z",
        null,
        null
      )
      mv.visitCode()
      mv.visitVarInsn(ALOAD, 0)
      mv.visitFieldInsn(GETFIELD, tname, vname, idesc)
      val l1 = new Label()
      mv.visitJumpInsn(IFNULL, l1)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitTypeInsn(INSTANCEOF, iname)
      mv.visitJumpInsn(IFEQ, l1)
      val l2 = new Label()
      mv.visitLabel(l2)
      mv.visitInsn(ICONST_0)
      mv.visitInsn(IRETURN)
      mv.visitLabel(l1)
      mv.visitFrame(F_SAME, 0, null, 0, null)
      mv.visitVarInsn(ALOAD, 0)
      mv.visitVarInsn(ALOAD, 1)
      mv.visitMethodInsn(
        INVOKESPECIAL,
        "codechicken/multipart/TileMultipart",
        "canAddPart",
        "(Lcodechicken/multipart/TMultiPart;)Z"
      )
      mv.visitInsn(IRETURN)
      mv.visitMaxs(2, 2)
      mv.visitEnd()
    }

    def methods(cnode: ClassNode): Map[String, MethodNode] = {
      val m = cnode.methods.map(m => (m.name + m.desc, m)).toMap
      if (cnode.interfaces != null)
        m ++ cnode.interfaces.flatMap(i => methods(classNode(i)))
      else
        m
    }

    def generatePassThroughMethod(m: MethodNode) {
      mv = cw.visitMethod(
        ACC_PUBLIC,
        m.name,
        m.desc,
        m.signature,
        Array(m.exceptions: _*)
      )
      mv.visitVarInsn(ALOAD, 0)
      mv.visitFieldInsn(GETFIELD, tname, vname, idesc)
      finishBridgeCall(mv, m.desc, INVOKEINTERFACE, iname, m.name, m.desc)
    }

    methods(inode).values.foreach(generatePassThroughMethod)

    cw.visitEnd()
    internalDefine(tname, cw.toByteArray)
    registerTrait(tname)
    return tname
  }

  override protected def onCompiled(
      clazz: Class[_ <: TileMultipart],
      traitSet: BitSet
  ) =
    MultipartGenerator.registerTileClass(clazz, traitSet)
}

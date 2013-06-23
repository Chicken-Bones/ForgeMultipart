package codechicken.multipart.asm

import scala.reflect.runtime.universe._
import scala.tools.reflect.ToolBox
import Flag._
import codechicken.multipart.TileMultipart
import codechicken.multipart.TileMultipartClient
import codechicken.multipart.handler.MultipartProxy
import cpw.mods.fml.common.FMLCommonHandler
import codechicken.multipart.TMultiPart
import cpw.mods.fml.relauncher.Side
import codechicken.multipart.MultipartGenerator

object ScalaCompilerFactory extends IMultipartFactory
{
    private var ugenid = 0
    
    var mirror = scala.reflect.runtime.currentMirror
    var tb = mirror.mkToolBox()
    
    def Apply(f:Tree, args:Tree*) = scala.reflect.runtime.universe.Apply(f, List(args:_*))
    def Apply(f:Tree, args:List[Tree]) = scala.reflect.runtime.universe.Apply(f, args)
    def TypeApply(f:Tree, args:Tree*) = scala.reflect.runtime.universe.TypeApply(f, List(args:_*))
    def TypeApply(f:Tree, args:List[Tree]) = scala.reflect.runtime.universe.TypeApply(f, args)
    def Invoke(s:Tree, n:TermName, args:Tree*) = Apply(Select(s, n), args:_*)
    def literalUnit = Literal(Constant(()))
    def const(o:Any) = Literal(Constant(o))
    def PkgIdent(s:String):Tree = Ident(mirror.staticClass(s))
    
    def typeSymbol(s:String):TypeSymbol = mirror.staticClass(s)
    
    def defaultConstructor() = 
        DefDef(
            Modifiers(), 
            nme.CONSTRUCTOR, //method name
            List(), //type params
            List(List()), //params
            TypeTree(), //return type
            Block(
                Invoke(//body
                    Super(This(tpnme.EMPTY), tpnme.EMPTY), 
                    nme.CONSTRUCTOR
                )
            )
        )
    
    def normalClassDef(mods:FlagSet, name:String, parents:List[TypeSymbol], methods:List[Tree]) = 
        ClassDef(
            Modifiers(mods),
            name,
            List(),
            Template(
                parents.map(Ident(_)),
                emptyValDef,
                methods
            )
        )
    
    def uniqueName(prefix:String):String = {
        val ret = prefix+"$$"+ugenid
        ugenid += 1
        return ret
    }
    
    abstract class Generator
    {
        def generate():TileMultipart
    }
    
    object SuperSet
    {
        val TileMultipartType = typeOf[TileMultipart]
        val TileMultipartClientType = typeOf[TileMultipartClient]
        def apply(types:Seq[Type], client:Boolean) = new SuperSet(types, client)
    }
    
    class SuperSet(types:Seq[Type], client:Boolean)
    {
        import SuperSet._
        val set = baseType+:types.sortWith(_.toString < _.toString)
        
        def interfaces = set
        def baseType = if(client) TileMultipartClientType else TileMultipartType
        
        override def equals(obj:Any) = obj match
        {
            case x:SuperSet => set == x.set
            case _ => false
        }
        
        override def hashCode() = set.hashCode
        
        def generate():TileMultipart = 
        {
            return generatorMap.getOrElse(this, gen_sync).generate
        }
        
        def gen_sync():Generator = tb.synchronized
        {
            return generatorMap.getOrElse(this, {
                var gen = generator
                generatorMap = generatorMap+(this->gen)
                gen
            })
        }
        
        def generator():Generator = 
        {
            val s = System.currentTimeMillis
            val defClass = 
                normalClassDef(
                    NoFlags,
                    uniqueName("TileMultipart_cmp"),
                    set.map(_.typeSymbol.asType).toList, 
                    List(
                        defaultConstructor
                    )
                )
            val defGenClass = 
                normalClassDef(
                    FINAL, 
                    uniqueName("TileMultipart_gen"), 
                    List(typeOf[Generator].typeSymbol.asType), 
                    List(//methods
                        defaultConstructor, 
                        DefDef(
                            Modifiers(OVERRIDE), 
                            "generate":TermName, 
                            List(), 
                            List(List()), 
                            TypeTree(), 
                            Invoke(
                                New(Ident(defClass.name)), 
                                nme.CONSTRUCTOR
                            )
                        )
                    )
                )
            val constructGenClass = 
                Invoke(//return new generator instance
                    New(Ident(defGenClass.name)), 
                    nme.CONSTRUCTOR
                )
            
            val v = tb.eval(Block(defClass, defGenClass, constructGenClass)).asInstanceOf[Generator]
            val dummy = v.generate
            MultipartGenerator.registerTileClass(dummy.getClass, types.toSet)
            println("Generation ["+types.mkString(", ")+"] took: "+(System.currentTimeMillis-s))
            return v.asInstanceOf[Generator]
        }
    }
    
    private var generatorMap:Map[SuperSet, Generator] = Map()
    
    SuperSet(Seq(), false).generate//default impl, boots generator
    if(FMLCommonHandler.instance.getEffectiveSide == Side.CLIENT)
        SuperSet(Seq(), true).generate
    
    private def symbolToValDef(m:Symbol) =
        ValDef(Modifiers(PARAM), m.asTerm.name.decoded, Ident(m), EmptyTree)
    
    private def passThroughMethod(tname:String, m:MethodSymbol):Tree =
        DefDef(
            Modifiers(Flag.OVERRIDE), 
            m.name, 
            List(), 
            m.paramss.map(_.map(m => symbolToValDef(m))), 
            TypeTree(), 
            Apply(
               Select(
                   Select(This(tname), "impl"), 
                   m.name.toTermName),
               m.paramss.flatMap(_.map{m => 
                   Ident(m.name)
               })
            )
        )
    
    private def passThroughTraitName(iName:String) = 
        "T" + (if(iName.startsWith("I")) iName.substring(1) else iName)
    
    def generatePassThroughTrait(iSymbol:ClassSymbol):Type =
    {
        val tname = uniqueName(passThroughTraitName(iSymbol.name.decoded))
        val methods = iSymbol.toType.members.filter(_.isJava).map(m => passThroughMethod(tname, m.asMethod))
        val traitDef = 
            normalClassDef(
                ABSTRACT | INTERFACE | TRAIT, 
                tname, 
                List(
                    typeSymbol("codechicken.multipart.TileMultipart"), 
                    iSymbol), 
                List(
                    ValDef(//pass through field
                        Modifiers(MUTABLE | DEFAULTINIT), 
                        "impl", 
                        Ident(iSymbol), 
                        EmptyTree),
                    DefDef(
                        Modifiers(OVERRIDE), 
                        "partAdded":TermName, 
                        List(),
                        List(
                            List(
                                ValDef(Modifiers(PARAM), "part", PkgIdent("codechicken.multipart.TMultiPart"), EmptyTree)
                            )),
                        TypeTree(), 
                        Block(
                            If(
                                TypeApply(
                                    Select(Ident("part"), "isInstanceOf"), 
                                    Ident(iSymbol)), 
                                Apply(
                                    Select(This(tname), "impl_$eq"), 
                                    TypeApply(
                                        Select(Ident("part"), "asInstanceOf"), 
                                        Ident(iSymbol))), 
                                    literalUnit), 
                            Apply(
                                Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), "partAdded"), 
                                Ident("part")
                            )
                        )),
                    DefDef(
                        Modifiers(OVERRIDE), 
                        "partRemoved":TermName, 
                        List(), 
                        List(
                            List(
                                ValDef(Modifiers(PARAM), "part", PkgIdent("codechicken.multipart.TMultiPart"), EmptyTree),
                                ValDef(Modifiers(PARAM), "p", PkgIdent("scala.Int"), EmptyTree)
                            )), 
                        TypeTree(), 
                        Block(
                            If(
                                Apply(
                                    Select(Ident("part"), "$eq$eq"), 
                                    Select(This(tname), "impl")), 
                                Apply(
                                    Select(This(tname), "impl_$eq"), 
                                    const(null)), 
                                literalUnit), 
                            Apply(
                                Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), "partRemoved"), 
                                Ident("part"),
                                Ident("p")
                            )
                        )), 
                    DefDef(
                        Modifiers(OVERRIDE), 
                        "occlusionTest":TermName, 
                        List(),
                        List(
                            List(
                                ValDef(Modifiers(PARAM), "parts", 
                                    AppliedTypeTree(
                                        PkgIdent("scala.collection.Seq"),
                                        List(PkgIdent("codechicken.multipart.TMultiPart"))), 
                                    EmptyTree),
                                ValDef(Modifiers(PARAM), "npart", PkgIdent("codechicken.multipart.TMultiPart"), EmptyTree)
                            )), 
                        TypeTree(), 
                        Invoke(
                            Invoke(
                                Select(
                                    TypeApply(
                                        Select(Ident("npart"), "isInstanceOf"), 
                                        Ident(iSymbol)), 
                                    "unary_$bang"), 
                                "$bar$bar", 
                                Invoke(
                                    Select(This(tname), "impl"), 
                                    "$eq$eq", 
                                    const(null))), 
                            "$amp$amp",
                            Invoke(
                                Super(This(tpnme.EMPTY), tpnme.EMPTY), 
                                "occlusionTest", 
                                Ident("parts"),
                                Ident("npart")
                            )
                        )
                    )
                )
                ++methods
            )
                
        val retType = 
            TypeApply(
                Ident("classOf"), 
                Ident(tname:TypeName))
        
        val clazz = tb.eval(Block(traitDef, retType)).asInstanceOf[Class[_]]
        return mirror.classSymbol(clazz).asType.toType
    }
    
    def generateTile(types:Seq[Type], client:Boolean) = SuperSet(types, client).generate        
}
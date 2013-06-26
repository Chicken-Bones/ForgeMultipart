package codechicken.multipart.asm

import scala.reflect.runtime.{universe => ru}
import ru._
import Flag._
import scala.tools.reflect.ToolBox
import scala.collection.mutable.{Map => MMap}
import codechicken.multipart.TileMultipart
import codechicken.multipart.TileMultipartClient
import codechicken.multipart.handler.MultipartProxy
import cpw.mods.fml.common.FMLCommonHandler
import codechicken.multipart.TMultiPart
import cpw.mods.fml.relauncher.Side
import codechicken.multipart.MultipartGenerator

/**
 * Adds top level code gen to toolboxes
 * Scala 2.10 implementaton for https://github.com/scala/scala/pull/2662
 */
class ExtendedToolbox(tb:ToolBox[ru.type])
{
    def define(tree: ImplDef, pkg:String): Symbol = 
    {
        val compiler = tb.asInstanceOf[{ def compiler: scala.tools.nsc.Global }].compiler
        val importer = compiler.mkImporter(ru)
        val exporter = importer.reverse
        val ctree: compiler.ImplDef = importer.importTree(tree).asInstanceOf[compiler.ImplDef]
        def defineInternal(ctree: compiler.ImplDef): compiler.Symbol = 
        {
            import compiler._

            val pdef = PackageDef(Ident(pkg:TermName), List(ctree))
            val unit = new CompilationUnit(scala.reflect.internal.util.NoSourceFile)
            unit.body = pdef
            
            val run = new Run
            reporter.reset()
            run.compileUnits(List(unit), run.namerPhase)
            compiler.asInstanceOf[{ def throwIfErrors(): Unit }].throwIfErrors()

            ctree.symbol
        }
        val csym: compiler.Symbol = defineInternal(ctree)
        val usym = exporter.importSymbol(csym)
        usym
    }
}

object ExtendedToolbox
{
    implicit def extend(tb:ToolBox[ru.type]) = new ExtendedToolbox(tb)
}

object ScalaCompilerFactory extends IMultipartFactory
{
    import ExtendedToolbox._
    
    private var ugenid = 0
    
    val mirror = scala.reflect.runtime.currentMirror
    val tb = mirror.mkToolBox()
    private val definedTypes = MMap[String, TypeSymbol]()
    
    def getType(obj:Any) = mirror.classSymbol(obj.getClass).toType
    
    def Apply(f:Tree, args:Tree*) = scala.reflect.runtime.universe.Apply(f, List(args:_*))
    def Apply(f:Tree, args:List[Tree]) = scala.reflect.runtime.universe.Apply(f, args)
    def TypeApply(f:Tree, args:Tree*) = scala.reflect.runtime.universe.TypeApply(f, List(args:_*))
    def TypeApply(f:Tree, args:List[Tree]) = scala.reflect.runtime.universe.TypeApply(f, args)
    def Invoke(s:Tree, n:TermName, args:Tree*) = Apply(Select(s, n), args:_*)
    def literalUnit = Literal(Constant(()))
    def const(o:Any) = Literal(Constant(o))
    def PkgIdent(s:String):Tree = Ident(typeSymbol(s))
    
    def typeSymbol(s:String) = definedTypes.getOrElse(s, mirror.staticClass(s))
    
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
    
    def define(tree: ImplDef): TypeSymbol =
    {
        val sym = tb.define(tree, "scfactory").asType
        definedTypes+=((sym.fullName, sym))
        return sym
    }
    
    abstract class Constructor
    {
        def generate():TileMultipart
    }
    
    object SuperSet
    {
        val TileMultipartType = classOf[TileMultipart].getName
        val TileMultipartClientType = classOf[TileMultipartClient].getName
        def apply(types:Seq[String], client:Boolean) = new SuperSet(types, client)
    }
    
    class SuperSet(types:Seq[String], client:Boolean)
    {
        import SuperSet._
        val set = baseType+:types.sorted
        
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
        
        def gen_sync():Constructor = tb.synchronized
        {
            return generatorMap.getOrElse(this, {
                var gen = generator
                generatorMap = generatorMap+(this->gen)
                gen
            })
        }
        
        def generator():Constructor = 
        {
            val startTime = System.currentTimeMillis
            val defClass = 
                normalClassDef(
                    NoFlags,
                    uniqueName("TileMultipart_cmp"),
                    set.map(s => typeSymbol(s)).toList, 
                    List(
                        defaultConstructor
                    )
                )
            val csym = define(defClass)
            
            val defClassOf = TypeApply(Ident("classOf"), Ident(csym))
            val cmpClass = tb.eval(defClassOf).asInstanceOf[Class[_ <: TileMultipart]]
            MultipartGenerator.registerTileClass(cmpClass, types.toSet)
            
            val defGenClass = 
                normalClassDef(
                    FINAL, 
                    uniqueName("TileMultipart_gen"), 
                    List(typeOf[Constructor].typeSymbol.asType), 
                    List(//methods
                        defaultConstructor, 
                        DefDef(
                            Modifiers(OVERRIDE), 
                            "generate":TermName, 
                            List(), 
                            List(List()), 
                            TypeTree(), 
                            Invoke(
                                New(Ident(csym)), 
                                nme.CONSTRUCTOR
                            )
                        )
                    )
                )
            val gsym = define(defGenClass)
            
            val constructGenClass = 
                Invoke(//return new generator instance
                    New(Ident(gsym)), 
                    nme.CONSTRUCTOR
                )
            
            val genInst = tb.eval(Block(defGenClass, constructGenClass)).asInstanceOf[Constructor]
            println("Generation ["+types.mkString(", ")+"] took: "+(System.currentTimeMillis-startTime))
            return genInst
        }
    }
    
    private var generatorMap:Map[SuperSet, Constructor] = Map()
    
    SuperSet(Seq(), false).generate//default impl, boots generator
    if(FMLCommonHandler.instance.getEffectiveSide == Side.CLIENT)
        SuperSet(Seq(), true).generate
    
    def registerTrait(s_interface:String, s_trait:String, client:Boolean){}
    
    def generatePassThroughTrait(s_interface:String):String =
    {
        def passThroughTraitName(iName:String) = 
                "T" + (if(iName.startsWith("I")) iName.substring(1) else iName)
        
        val iSymbol = mirror.staticClass(s_interface)
        val tname = uniqueName(passThroughTraitName(iSymbol.name.decoded))
        val vname = tname+"_impl"
        
        def symbolToValDef(m:Symbol) =
            ValDef(Modifiers(PARAM), m.asTerm.name.decoded, Ident(m), EmptyTree)
    
        def passThroughMethod(tname:String, vname:String, m:MethodSymbol):Tree =
            DefDef(
                Modifiers(Flag.OVERRIDE), 
                m.name, 
                List(), 
                m.paramss.map(_.map(m => symbolToValDef(m))), 
                TypeTree(), 
                Apply(
                   Select(
                       Select(This(tname), vname), 
                       m.name.toTermName),
                   m.paramss.flatMap(_.map{m => 
                       Ident(m.name)
                   })
                )
            )
            
        val methods = iSymbol.toType.members.filter(_.isJava).map(m => passThroughMethod(tname, vname, m.asMethod))
        val traitDef = 
            normalClassDef(
                ABSTRACT | TRAIT, 
                tname, 
                List(
                    typeSymbol("codechicken.multipart.TileMultipart"), 
                    iSymbol), 
                List(
                    ValDef(//pass through field
                        Modifiers(MUTABLE | DEFAULTINIT), 
                        vname, 
                        Ident(iSymbol), 
                        EmptyTree),
                    DefDef(
                        Modifiers(OVERRIDE), 
                        "bindPart":TermName, 
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
                                    Select(This(tname), vname+"_$eq"), 
                                    TypeApply(
                                        Select(Ident("part"), "asInstanceOf"), 
                                        Ident(iSymbol))), 
                                    literalUnit), 
                            Apply(
                                Select(Super(This(tpnme.EMPTY), tpnme.EMPTY), "bindPart"), 
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
                                    Select(This(tname), vname)), 
                                Apply(
                                    Select(This(tname), vname+"_$eq"), 
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
                        "canAddPart":TermName, 
                        List(),
                        List(
                            List(
                                ValDef(Modifiers(PARAM), "part", PkgIdent("codechicken.multipart.TMultiPart"), EmptyTree)
                            )), 
                        Ident("Boolean":TypeName), 
                        Block(
                            If(//if(part.isInstanceOf[I] && impl != null)
                                Invoke(
                                    TypeApply(
                                        Select(Ident("part"), "isInstanceOf"), 
                                        Ident(iSymbol)),
                                    "$amp$amp", 
                                    Invoke(
                                        Select(This(tname), vname), 
                                        "$bang$eq", 
                                        const(null))), 
                                Return(const(false)),//return true
                                literalUnit),//else nothing
                            Invoke(//call super
                                Super(This(tpnme.EMPTY), tpnme.EMPTY), 
                                "canAddPart", 
                                Ident("part")
                            )
                        )
                    )
                )
                ++methods
            )
        
        return define(traitDef).fullName
    }
    
    def generateTile(types:Seq[String], client:Boolean) = SuperSet(types, client).generate
}
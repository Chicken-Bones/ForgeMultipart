package codechicken.microblock

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.FileReader
import scala.collection.mutable.{Map => MMap}
import net.minecraft.block.Block
import java.lang.Exception
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage
import net.minecraft.item.ItemStack
import BlockMicroMaterial.createAndRegister
import BlockMicroMaterial.materialKey

object ConfigContent
{
    private val nameMap = MMap[String, Seq[Int]]()
    private val idMap = new Array[Seq[Int]](Block.blocksList.length)
    
    def parse(cfgDir:File)
    {
        val cfgFile = new File(cfgDir, "microblocks.cfg")
        try
        {
            if(!cfgFile.exists())
                generateDefault(cfgFile)
            else
                loadLines(cfgFile)
        }
        catch
        {
            case e:IOException => e.printStackTrace()
        }
    }
    
    def generateDefault(cfgFile:File)
    {
        val writer = new PrintWriter(cfgFile)
        writer.println("#Configuration file for adding microblock materials for aesthetic blocks added by mods")
        writer.println("#Each line needs to be of the form <ID>:<meta>")
        writer.println("#<ID> may be a numerical id or the unlocalised name of the block/item enclosed in double quotes. NEI can help you find these")
        writer.println("#<meta> may be ommitted, in which case it defaults to 0, otherwise it can be a number, a comma separated list of numbers, or a dash separated range")
        writer.println("#Ex. 3. 5:3. 213:1,2,3,5. \"tile.blockIron\":0-15")
        writer.close()
    }
    
    def loadLine(line:String)
    {
        if(line.startsWith("#"))
            return
        
        var split1 = line.replace(" ", "").split(":")
        if(split1.length > 2)
            throw new IllegalArgumentException("Invalid number of ':' separators")
        if(split1.length == 1)
            split1 = Array(split1(0), "0")
            
        if(split1(0) == "")
            return
        
        var split2 = split1(1).split(",")
        val meta = split2.flatMap{s => 
            if(s.contains("-"))
            {
                val split2 = s.split("-")
                if(split2.length != 2)
                    throw new IllegalArgumentException("Invalid - separated range")
                split2(0).toInt to split2(1).toInt
            }
            else
            {
                Seq(s.toInt)
            }
        }
        
        val s_id = split1(0)
        if(s_id.charAt(0) == '\"')
        {
            if(!s_id.endsWith("\""))
                throw new IllegalArgumentException("Missing closing \" character")
            
            val name = s_id.substring(1, s_id.length-1)
            nameMap.put(name, meta)
        }
        else
        {
            idMap(s_id.toInt) = meta
        }
    }
    
    def loadLines(cfgFile:File)
    {
        val reader = new BufferedReader(new FileReader(cfgFile))
        var s:String = null
        do
        {
            s = reader.readLine
            if(s != null)
            {
                try
                {
                    loadLine(s)
                }
                catch
                {
                    case e:Exception => {
                        System.err.println("Invalid line in microblocks.cfg: "+s)
                        System.err.println(e.getMessage)
                    }
                }
            }
        }
        while(s != null)
        reader.close()
    }
    
    def load()
    {
        for(i <- 0 until idMap.length)
        {
            val block = Block.blocksList(i)
            if(idMap(i) != null)
            {
                if(block == null)
                    System.err.println("Warning: Unable to add micro material for block with ID "+i+" as it doesn't exist")
                else
                    createAndRegister(block, idMap(i))
            }
            else if(block != null)
            {
                val name = block.getUnlocalizedName()
                val value = nameMap.get(name)
                if(value.isDefined)
                {
                	nameMap.remove(name)
                	value.get.foreach{m => 
	                    try {
	                    	createAndRegister(block, m)
	                    }
	                    catch {
	                    	case e:IllegalStateException => System.err.println("Unable to register micro material: "+
	                    	        materialKey(block, m)+"\n\t"+e.getMessage)
	            	        case e:Exception => {
	            	            System.err.println("Unable to register micro material: "+materialKey(block, m))
	        	                e.printStackTrace()
	            	        }
	                    }
                	}
                }
            }
        }
        
        nameMap.foreach(e => System.err.println("Warning: Unable to add micro material for block with unlocalised name "+e._1+" as it doesn't exist"))
    }
    
    def handleIMC(messages:Seq[IMCMessage]) {
        messages.filter(_.key == "microMaterial").foreach{msg => 
            
            def error(s:String) {
                System.err.println("Invalid microblock IMC message from "+msg.getSender+": "+s)
            }
            
            if(msg.getMessageType != classOf[ItemStack])
                error("value is not an instanceof ItemStack")
            else {
                val stack = msg.getItemStackValue
                if(stack.itemID >= Block.blocksList.length || Block.blocksList(stack.itemID) == null)
                    System.err.println("Invalid blockID: "+stack.itemID)
                else if(stack.getItemDamage < 0 || stack.getItemDamage >= 16)
                    System.err.println("Invalid metadata: "+stack.getItemDamage)
                else {
                    try {
	                    createAndRegister(Block.blocksList(stack.itemID), stack.getItemDamage)
                    }
                    catch {
                    	case e:IllegalStateException => System.err.println("Unable to register micro material: "+
                    	        materialKey(Block.blocksList(stack.itemID), stack.getItemDamage)+"\n\t"+e.getMessage)
                    }
                }
            }
        }
    }
}
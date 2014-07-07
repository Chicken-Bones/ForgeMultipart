package codechicken.microblock

import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.io.BufferedReader
import java.io.FileReader
import scala.collection.mutable.{Map => MMap}
import net.minecraft.block.Block
import net.minecraft.item.Item
import java.lang.Exception
import cpw.mods.fml.common.event.FMLInterModComms.IMCMessage
import net.minecraft.item.ItemStack
import BlockMicroMaterial.createAndRegister
import BlockMicroMaterial.materialKey
import java.lang.{Iterable => JIterable}
import scala.collection.JavaConversions._

object ConfigContent
{
    private val nameMap = MMap[String, Seq[Int]]()

    def parse(cfgDir: File) {
        val cfgFile = new File(cfgDir, "microblocks.cfg")
        try {
            if (!cfgFile.exists())
                generateDefault(cfgFile)
            else
                loadLines(cfgFile)
        }
        catch {
            case e: IOException => logger.error("Error parsing config", e)
        }
    }

    def generateDefault(cfgFile: File) {
        val writer = new PrintWriter(cfgFile)
        writer.println("#Configuration file for adding microblock materials for aesthetic blocks added by mods")
        writer.println("#Each line needs to be of the form <name>:<meta>")
        writer.println("#<name> is the unlocalised name or registry key of the block/item enclosed in quotes. NEI can help you find these")
        writer.println("#<meta> may be ommitted, in which case it defaults to 0, otherwise it can be a number, a comma separated list of numbers, or a dash separated range")
        writer.println("#Ex. \"dirt\" \"minecraft:planks\":3 \"iron_ore\":1,2,3,5 \"ThermalFoundation:Storage\":0-15")
        writer.close()
    }

    def loadLine(line: String) {
        if (line.startsWith("#") || line.length < 3)
            return

        if(line.charAt(0) != '\"')
            throw new IllegalArgumentException("Line must begin with a quote")
        val q2 = line.indexOf('\"', 1)
        if(q2 < 0)
            throw new IllegalArgumentException("Unmatched quotes")

        var name = line.substring(1, q2)
        if(!name.contains('.') && !name.contains(':'))
            name = "minecraft:"+name

        var meta = Seq(0)
        if(line.length > q2+1) {
            if(line.charAt(q2+1) != ':')
                throw new IllegalArgumentException("Name must be followed by a colon separator")

            meta = line.substring(q2+2).split(",").flatMap{s =>
                if (s.contains("-")) {
                    val split2 = s.split("-")
                    if (split2.length != 2)
                        throw new IllegalArgumentException("Invalid - separated range")
                    split2(0).toInt to split2(1).toInt
                }
                else {
                    Seq(s.toInt)
                }
            }
        }

        nameMap.put(name, meta)
    }

    def loadLines(cfgFile: File) {
        val reader = new BufferedReader(new FileReader(cfgFile))
        var s: String = null
        do {
            s = reader.readLine
            if (s != null) {
                try {
                    loadLine(s)
                }
                catch {
                    case e: Exception =>
                        logger.error("Invalid line in microblocks.cfg: " + s)
                        logger.error(e.getMessage)
                }
            }
        }
        while (s != null)
        reader.close()
    }

    def load() {
        for(block <- Block.blockRegistry.asInstanceOf[JIterable[Block]]) {
            val metas = Seq(block.getUnlocalizedName, Block.blockRegistry.getNameForObject(block)).flatMap(nameMap.remove).flatten
            metas.foreach{m =>
                    try {
                        createAndRegister(block, m)
                    }
                    catch {
                        case e: IllegalStateException => logger.error("Unable to register micro material: " +
                            materialKey(block, m) + "\n\t" + e.getMessage)
                        case e: Exception =>
                            logger.error("Unable to register micro material: " + materialKey(block, m), e)
                    }
            }
        }

        nameMap.foreach(e => logger.warn("Unable to add micro material for block with unlocalised name " + e._1 + " as it doesn't exist"))
    }

    def handleIMC(messages: Seq[IMCMessage]) {
        messages.filter(_.key == "microMaterial").foreach {
            msg =>

                def error(s: String) {
                    logger.error("Invalid microblock IMC message from " + msg.getSender + ": " + s)
                }

                if (msg.getMessageType != classOf[ItemStack])
                    error("value is not an instanceof ItemStack")
                else {
                    val stack = msg.getItemStackValue
                    if (!Block.blockRegistry.containsId(Item.getIdFromItem(stack.getItem)))
                        error("Invalid Block: " + stack.getItem)
                    else if (stack.getItemDamage < 0 || stack.getItemDamage >= 16)
                        error("Invalid metadata: " + stack.getItemDamage)
                    else {
                        try {
                            createAndRegister(Block.getBlockFromItem(stack.getItem), stack.getItemDamage)
                        }
                        catch {
                            case e: IllegalStateException => error("Unable to register micro material: " +
                                materialKey(Block.getBlockFromItem(stack.getItem), stack.getItemDamage) + "\n\t" + e.getMessage)
                        }
                    }
                }
        }
    }
}
package codechicken.multipart

import codechicken.lib.data.MCDataOutput
import codechicken.lib.data.MCDataInput

trait IDWriter
{
    var write:(MCDataOutput, Int)=>Unit = _
    var read:(MCDataInput)=>Int = _
    
    def setMax(i:Int)
    {
        val l = i.toLong & 0xFFFFFFFF
        if(l > 0xFFFF)
        {
            write = (data, i) => data.writeInt(i)
            read = (data) => data.readInt()
        }
        else if(l > 0xFF)
        {
            write = (data, i) => data.writeShort(i)
            read = (data) => data.readUShort()
        }
        else
        {
            write = (data, i) => data.writeByte(i)
            read = (data) => data.readUByte()
        }
    }
}

object IDWriter
{
    def apply():IDWriter = new IDWriterImpl;
}

class IDWriterImpl extends IDWriter
{
}
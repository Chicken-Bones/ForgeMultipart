package codechicken.multipart.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetServerHandler;
import codechicken.core.packet.PacketCustom;
import codechicken.core.packet.PacketCustom.IServerPacketHandler;

public class McMultipartSPH implements IServerPacketHandler
{
    public static Object channel = MinecraftMultipartMod.instance;
    
    @Override
    public void handlePacket(PacketCustom packet, NetServerHandler nethandler, EntityPlayerMP sender)
    {
        switch(packet.getType())
        {
            case 1:
                EventHandler.place(sender, sender.worldObj);
            break;
        }
    }
}

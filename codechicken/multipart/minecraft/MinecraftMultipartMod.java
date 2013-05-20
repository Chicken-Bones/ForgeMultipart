package codechicken.multipart.minecraft;

import net.minecraftforge.common.MinecraftForge;
import codechicken.core.asm.CodeChickenCorePlugin;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(name="Minecraft Multipart", version="1.0.0.0", useMetadata = false, modid = "McMultipart", acceptedMinecraftVersions=CodeChickenCorePlugin.mcVersion)
public class MinecraftMultipartMod
{
    @PreInit
    public void preInit(FMLPreInitializationEvent event)
    {
        new Content().init();
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }
}

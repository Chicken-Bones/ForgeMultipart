package codechicken.multipart.nei;

/* Commented until NEI is setup with maven

import codechicken.microblock.MicroblockClass;
import codechicken.microblock.MicroblockClassRegistry;
import codechicken.nei.api.IConfigureNEI;

public class NEI_MicroblockConfig implements IConfigureNEI
{
    @Override
    public void loadConfig()
    {
        MicroblockClass[] microClasses = MicroblockClassRegistry.classes();
        for(int c = 0; c < microClasses.length; c++)
        {
            MicroblockClass mcrClass = microClasses[c];
            if(mcrClass == null)
                continue;

            addSubset(mcrClass, c<<8|1);
            addSubset(mcrClass, c<<8|2);
            addSubset(mcrClass, c<<8|4);
        }
    }

    private void addSubset(MicroblockClass mcrClass, int i)
    {
        API.addSetRange("Microblocks."+LangUtil.translateG(mcrClass.getName()+"."+(i&0xFF)+".subset"),
                new MultiItemRange().add(MicroblockProxy.itemMicro(), i, i));
    }

    @Override
    public String getName()
    {
        return "ForgeMultipart";
    }

    @Override
    public String getVersion()
    {
        return "1.0.0.0";
    }
}*/
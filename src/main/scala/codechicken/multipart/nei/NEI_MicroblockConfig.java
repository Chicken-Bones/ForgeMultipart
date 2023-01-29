package codechicken.multipart.nei;

import java.util.Arrays;

import net.minecraft.util.StatCollector;

import codechicken.lib.inventory.InventoryUtils;
import codechicken.microblock.CommonMicroClass;
import codechicken.microblock.MicroblockClass;
import codechicken.microblock.handler.MicroblockProxy;
import codechicken.nei.ItemStackMap;
import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

public class NEI_MicroblockConfig implements IConfigureNEI {

    @Override
    public void loadConfig() {
        MicroblockClass[] microClasses = CommonMicroClass.classes();
        for (int c = 0; c < microClasses.length; c++) {
            MicroblockClass mcrClass = microClasses[c];
            if (mcrClass == null) continue;

            addSubset(mcrClass, c << 8 | 1);
            addSubset(mcrClass, c << 8 | 2);
            addSubset(mcrClass, c << 8 | 4);
        }
    }

    private void addSubset(MicroblockClass mcrClass, int i) {
        API.addSubset(
                "Microblocks." + StatCollector.translateToLocal(mcrClass.getName() + "." + (i & 0xFF) + ".subset"),
                Arrays.asList(
                        InventoryUtils.newItemStack(MicroblockProxy.itemMicro(), 1, i, ItemStackMap.WILDCARD_TAG)));
    }

    @Override
    public String getName() {
        return "ForgeMultipart";
    }

    @Override
    public String getVersion() {
        return "1.0.0.0";
    }
}

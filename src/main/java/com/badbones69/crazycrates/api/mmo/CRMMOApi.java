package com.badbones69.crazycrates.api.mmo;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;

public class CRMMOApi {

    @Nullable
    public ItemStack getItemStack(String type, String id) {
        return MMOItems.plugin.getItem(type, id);
    }

}

package com.badbones69.crazycrates.api.objects;

import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.misc.Keys;
import com.badbones69.crazycrates.api.objects.Crate;
import java.util.List;
import libs.com.ryderbelserion.vital.common.utils.math.MathUtil;
import com.badbones69.crazycrates.utils.ColorUtils;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Tier {
    private final ItemBuilder item;
    private final String name;
    private final List<String> lore;
    private final String coloredName;
    private final double weight;
    private final int slot;

    public Tier(@NotNull String tier, @NotNull ConfigurationSection section) {
        this.name = tier;
        this.coloredName = ColorUtils.color(section.getString("Name", ""));
        this.lore = section.getStringList("Lore").stream().map(ColorUtils::color)
                .collect(Collectors.toList());
        this.item = (ItemBuilder) ((ItemBuilder) ((ItemBuilder) new ItemBuilder()
                .withType(section.getString("Item", "chest").toLowerCase()))
                .setHidingItemFlags(section.getBoolean("HideItemFlags", false)))
                .setCustomModelData(section.getInt("Custom-Model-Data", -1));
        this.weight = section.getDouble("Weight", -1.0);
        this.slot = section.getInt("Slot");
    }

    @NotNull
    public final String getName() {
        return this.name;
    }

    @NotNull
    public final String getColoredName() {
        return this.coloredName;
    }

    @NotNull
    public final ItemBuilder getItem() {
        return this.item;
    }

    public final double getWeight() {
        return this.weight;
    }

    public final int getSlot() {
        return this.slot;
    }

    @NotNull
    public final ItemStack getTierItem(@Nullable Player target, Crate crate) {
        if (target != null) {
            this.item.setPlayer(target);
        }
        return ((ItemBuilder) ((ItemBuilder) ((ItemBuilder) ((ItemBuilder) this.item.setDisplayName(this.coloredName))
                .setDisplayLore(this.lore))
                .addLorePlaceholder("%chance%", MathUtil.format(crate.getTierChance(this.getWeight()))))
                .setPersistentString(Keys.crate_tier.getNamespacedKey(), this.name)).asItemStack();
    }
}

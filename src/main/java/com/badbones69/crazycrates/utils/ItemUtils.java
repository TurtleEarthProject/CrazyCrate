package com.badbones69.crazycrates.utils;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.misc.Keys;
import com.badbones69.crazycrates.api.mmo.CRMMOApi;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import libs.com.ryderbelserion.vital.common.utils.StringUtil;
import libs.com.ryderbelserion.vital.paper.util.DyeUtil;
import libs.com.ryderbelserion.vital.paper.util.ItemUtil;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.PatternType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class ItemUtils {
    private static final CrazyCrates plugin = CrazyCrates.getPlugin();
    private static final CrateManager crateManager = plugin.getCrateManager();

    public static void removeItem(@NotNull ItemStack item, @NotNull Player player) {
        try {
            int amount = item.getAmount();
            if (amount <= 1) {
                player.getInventory().removeItem(item);
            } else {
                item.setAmount(amount - 1);
            }
        } catch (Exception ignored) {
        }
    }

    public static String getEnchant(String enchant) {
        if (enchant.isEmpty()) {
            return "";
        }
        switch (enchant) {
            case "PROTECTION_ENVIRONMENTAL": {
                return "protection";
            }
            case "PROTECTION_FIRE": {
                return "fire_protection";
            }
            case "PROTECTION_FALL": {
                return "feather_falling";
            }
            case "PROTECTION_EXPLOSIONS": {
                return "blast_protection";
            }
            case "PROTECTION_PROJECTILE": {
                return "projectile_protection";
            }
            case "OXYGEN": {
                return "respiration";
            }
            case "WATER_WORKER": {
                return "aqua_affinity";
            }
            case "DAMAGE_ALL": {
                return "sharpness";
            }
            case "DAMAGE_UNDEAD": {
                return "smite";
            }
            case "DAMAGE_ARTHROPODS": {
                return "bane_of_arthropods";
            }
            case "LOOT_BONUS_MOBS": {
                return "looting";
            }
            case "SWEEPING_EDGE": {
                return "sweeping";
            }
            case "DIG_SPEED": {
                return "efficiency";
            }
            case "DURABILITY": {
                return "unbreaking";
            }
            case "LOOT_BONUS_BLOCKS": {
                return "fortune";
            }
            case "ARROW_DAMAGE": {
                return "power";
            }
            case "ARROW_KNOCKBACK": {
                return "punch";
            }
            case "ARROW_FIRE": {
                return "flame";
            }
            case "ARROW_INFINITE": {
                return "infinity";
            }
            case "LUCK": {
                return "luck_of_the_sea";
            }
        }
        return enchant.toLowerCase();
    }

    public static String getPotion(String potion) {
        return potion.isEmpty() ? "" : potion.toLowerCase();
    }

    public static boolean isSimilar(@NotNull ItemStack itemStack, @NotNull Crate crate) {
        return crateManager.isKeyFromCrate(itemStack, crate);
    }

    public static String getKey(@NotNull PersistentDataContainer container) {
        return container.get(Keys.crate_key.getNamespacedKey(), PersistentDataType.STRING);
    }

    @NotNull
    public static ItemBuilder getItem(@NotNull ConfigurationSection section, @NotNull ItemBuilder builder, @NotNull Player player) {
        return ItemUtils.getItem(section, (ItemBuilder)builder.setPlayer(player));
    }

    @NotNull
    public static ItemBuilder getItem(@NotNull ConfigurationSection section, @NotNull ItemBuilder builder) {
        builder.setGlowing(section.contains("Glowing") ? section.getBoolean("Glowing") : null);
        builder.setDamage(section.getInt("DisplayDamage", 0));
        builder.setDisplayLore(ColorUtils.color(section.getStringList("Lore")));
        builder.addPatterns(section.getStringList("Patterns"));
        builder.setItemFlags(section.getStringList("Flags"));
        builder.setHidingItemFlags(section.getBoolean("HideItemFlags", false));
        builder.setUnbreakable(section.getBoolean("Unbreakable", false));
        if (section.contains("Skull") && plugin.getApi() != null) {
            builder.setSkull(section.getString("Skull", ""), plugin.getApi());
        }
        if (section.contains("Player") && builder.isPlayerHead()) {
            builder.setPlayer(section.getString("Player", ""));
        }
        builder.setCustomModelData(section.getInt("Custom-Model-Data", -1));
        if (section.contains("DisplayTrim.Pattern") && builder.isArmor()) {
            builder.applyTrimPattern(section.getString("DisplayTrim.Pattern", "sentry"));
        }
        if (section.contains("DisplayTrim.Material") && builder.isArmor()) {
            builder.applyTrimMaterial(section.getString("DisplayTrim.Material", "quartz"));
        }
        if (section.contains("DisplayEnchantments")) {
            for (String ench : section.getStringList("DisplayEnchantments")) {
                String[] value = ench.split(":");
                builder.addEnchantment(value[0], Integer.parseInt(value[1]), true);
            }
        }
        return builder;
    }

    public static ItemBuilder convertItemStack(Player player, ItemStack itemStack) {
        ItemBuilder itemBuilder = new ItemBuilder(itemStack.getType(), itemStack.getAmount());
        if (player != null) {
            itemBuilder.setPlayer(player);
        }
        return itemBuilder;
    }

    public static ItemBuilder convertItemStack(ItemStack itemStack) {
        return ItemUtils.convertItemStack(null, itemStack);
    }

    public static List<ItemBuilder> convertStringList(List<String> itemStrings) {
        return ItemUtils.convertStringList(itemStrings, null);
    }

    public static List<ItemBuilder> convertStringList(List<String> itemStrings, String section) {
        return itemStrings.stream().map(itemString -> ItemUtils.convertString(itemString, section)).collect(Collectors.toList());
    }

    public static ItemBuilder convertString(String itemString) {
        return ItemUtils.convertString(itemString, null);
    }

    public static ItemBuilder convertString(String itemString, String section) {
        ItemBuilder itemBuilder = new ItemBuilder();
        try {
            // Support MMOItems shortcut: "mmoitem:TYPE:ID"
            if (itemString != null) {
                String lower = itemString.toLowerCase();
                if (lower.startsWith("mmoitem:")) {
                    String[] parts = itemString.split(":", 3);
                    if (parts.length >= 3) {
                        String type = parts[1];
                        String id = parts[2];
                        CRMMOApi mmoApi = plugin.getMmoApi();
                        if (mmoApi != null) {
                            ItemStack mmoItem = mmoApi.getItemStack(type, id);
                            if (mmoItem != null) {
                                return new ItemBuilder(mmoItem);
                            }
                        }
                    }
                }
            }

            for (String optionString : itemString.split(", ")) {
                String option = optionString.split(":")[0];
                String value = optionString.replace(option + ":", "").replace(option, "");
                switch (option.toLowerCase()) {
                    case "item": {
                        itemBuilder.withType(value.toLowerCase());
                        continue;
                    }
                    case "data": {
                        itemBuilder = itemBuilder.fromBase64(value);
                        continue;
                    }
                    case "name": {
                        itemBuilder.setDisplayName(ColorUtils.color(value));
                        continue;
                    }
                    case "mob": {
                        EntityType type = ItemUtil.getEntity(value);
                        if (type == null) continue;
                        itemBuilder.setEntityType(type);
                        continue;
                    }
                    case "glowing": {
                        itemBuilder.setGlowing(Boolean.valueOf(value));
                        continue;
                    }
                    case "amount": {
                        Optional<Number> amount = StringUtil.tryParseInt(value);
                        itemBuilder.setAmount(amount.map(Number::intValue).orElse(1));
                        continue;
                    }
                    case "damage": {
                        Optional<Number> amount = StringUtil.tryParseInt(value);
                        itemBuilder.setDamage(amount.map(Number::intValue).orElse(1));
                        continue;
                    }
                    case "lore": {
                        itemBuilder.setDisplayLore(ColorUtils.color(List.of(value.split(","))));
                        continue;
                    }
                    case "player": {
                        itemBuilder.setPlayer(value);
                        continue;
                    }
                    case "skull": {
                        itemBuilder.setSkull(value, plugin.getApi());
                        continue;
                    }
                    case "custom-model-data": {
                        itemBuilder.setCustomModelData(StringUtil.tryParseInt(value).orElse(-1).intValue());
                        continue;
                    }
                    case "unbreakable-item": {
                        itemBuilder.setUnbreakable(value.isEmpty() || value.equalsIgnoreCase("true"));
                        continue;
                    }
                    case "trim-pattern": {
                        itemBuilder.applyTrimPattern(value);
                        continue;
                    }
                    case "trim-material": {
                        itemBuilder.applyTrimMaterial(value);
                        continue;
                    }
                    default: {
                        if (ItemUtil.getEnchantment(option.toLowerCase()) != null) {
                            Optional<Number> amount = StringUtil.tryParseInt(value);
                            itemBuilder.addEnchantment(option.toLowerCase(), amount.map(Number::intValue).orElse(1), true);
                            continue;
                        }
                        for (ItemFlag itemFlag : ItemFlag.values()) {
                            if (!itemFlag.name().equalsIgnoreCase(option)) continue;
                            itemBuilder.addItemFlag(itemFlag);
                            break;
                        }
                        try {
                            DyeColor color = DyeUtil.getDyeColor(value);
                            PatternType patternType = ItemUtil.getPatternType(option.toLowerCase());
                            if (patternType == null) continue;
                            itemBuilder.addPattern(patternType, color);
                            continue;
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception exception) {
            itemBuilder.withType(Material.RED_TERRACOTTA).setDisplayName("<red>Error found!, Prize Name: " + section);
            if (MiscUtils.isLogging()) {
                plugin.getLogger().warning("An error has occurred with the item builder: " + exception.getMessage());
            }
        }
        return itemBuilder;
    }
}



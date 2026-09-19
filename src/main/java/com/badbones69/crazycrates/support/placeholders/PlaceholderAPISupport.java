package com.badbones69.crazycrates.support.placeholders;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.utils.MiscUtils;
import java.text.NumberFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPISupport extends PlaceholderExpansion {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final BukkitUserManager userManager = this.plugin.getUserManager();
    private final CrateManager crateManager = this.plugin.getCrateManager();

    @NotNull
    public final String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null || identifier.isEmpty()) {
            return "0";
        }
        Player human = (Player) player;
        for (Crate crate : this.crateManager.getUsableCrates()) {
            String fileName = crate.getFileName();
            if (identifier.equalsIgnoreCase(fileName)) {
                return NumberFormat.getNumberInstance().format(this.userManager.getVirtualKeys(human.getUniqueId(), fileName));
            }
            if (identifier.equalsIgnoreCase(fileName + "_physical")) {
                return NumberFormat.getNumberInstance().format(this.userManager.getPhysicalKeys(human.getUniqueId(), fileName));
            }
            if (identifier.equalsIgnoreCase(fileName + "_total")) {
                return NumberFormat.getNumberInstance().format(this.userManager.getTotalKeys(human.getUniqueId(), fileName));
            }
            if (identifier.equalsIgnoreCase(fileName + "_opened")) {
                return NumberFormat.getNumberInstance().format(this.userManager.getCrateOpened(human.getUniqueId(), fileName));
            }
            if (identifier.equalsIgnoreCase("crates_opened")) {
                return NumberFormat.getNumberInstance().format(this.userManager.getTotalCratesOpened(human.getUniqueId()));
            }
        }
        
        int index = identifier.lastIndexOf("_");
        if (index == -1) return "0";
        
        String value = PlaceholderAPI.setPlaceholders(human, "%" + StringUtils.substringBetween(identifier.substring(0, index), "{", "}") + "%");
        Player target = this.plugin.getServer().getPlayer(value);
        if (target != null) {
            String[] parts = identifier.split("_");
            if (parts.length >= 3) {
                String crateName = parts[2];
                return this.getKeys(target.getUniqueId(), identifier, crateName, value);
            }
        }
        return "0";
    }

    @NotNull
    private String getKeys(@NotNull UUID uuid, @NotNull String identifier, @NotNull String crateName, @NotNull String value) {
        if (crateName.isEmpty() || value.isEmpty()) {
            return "0";
        }
        Crate crate = this.crateManager.getCrateFromName(crateName);
        if (crate == null) {
            if (identifier.endsWith("opened")) {
                return NumberFormat.getNumberInstance().format(this.userManager.getTotalCratesOpened(uuid));
            }
            return "0";
        }
        String fileName = crate.getFileName();
        if (identifier.endsWith("total")) {
            return NumberFormat.getNumberInstance().format(this.userManager.getTotalKeys(uuid, fileName));
        }
        if (identifier.endsWith("physical")) {
            return NumberFormat.getNumberInstance().format(this.userManager.getPhysicalKeys(uuid, fileName));
        }
        if (identifier.endsWith("virtual")) {
            return NumberFormat.getNumberInstance().format(this.userManager.getVirtualKeys(uuid, fileName));
        }
        if (identifier.endsWith("opened")) {
            return NumberFormat.getNumberInstance().format(this.userManager.getCrateOpened(uuid, fileName));
        }
        return "0";
    }

    public final boolean persist() {
        return true;
    }

    public final boolean canRegister() {
        return true;
    }

    @NotNull
    public final String getIdentifier() {
        return this.plugin.getName().toLowerCase();
    }

    @NotNull
    public final String getAuthor() {
        return "BadBones69";
    }

    @NotNull
    public final String getVersion() {
        return this.plugin.getPluginMeta().getVersion();
    }
}

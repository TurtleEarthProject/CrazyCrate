/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.PlaceholderAPI
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Event
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.permissions.PermissionAttachmentInfo
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.badbones69.crazycrates.api;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.builders.ItemBuilder;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.enums.misc.Files;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.api.objects.Tier;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.utils.MiscUtils;
import com.badbones69.crazycrates.utils.MsgUtils;
import com.badbones69.crazycrates.utils.ColorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import libs.com.ryderbelserion.vital.paper.util.scheduler.FoliaRunnable;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrizeManager {
    private static final CrazyCrates plugin = CrazyCrates.getPlugin();
    private static final BukkitUserManager userManager = plugin.getUserManager();
    private static ScheduledTask pendingDataSave;

    public static int getCap(Crate crate, Player player) {
        String format = "crazycrates.respin." + crate.getFileName() + ".";
        int cap = 0;
        for (PermissionAttachmentInfo permission : player.getEffectivePermissions()) {
            int number;
            String node = permission.getPermission();
            if (!node.startsWith(format.toLowerCase()) || (number = Integer.parseInt(node = node.replace(format.toLowerCase(), ""))) <= cap || cap >= crate.getCyclePermissionCap()) continue;
            cap = number;
        }
        return cap;
    }

    public static boolean isCapped(Crate crate, Player player) {
        boolean isCapped = false;
        if (!crate.isCyclePermissionToggle() || crate.getCyclePermissionCap() < 1 || player.isOp()) {
            return false;
        }
        int wins = userManager.getCrateRespin(player.getUniqueId(), crate.getFileName());
        int cap = PrizeManager.getCap(crate, player);
        String format = "crazycrates.respin." + crate.getFileName() + ".";
        String node = format + cap;
        if (player.hasPermission(node)) {
            if (wins >= cap) {
                isCapped = true;
            }
        } else {
            isCapped = true;
        }
        return isCapped;
    }

    public static void givePrize(@NotNull Player player, @Nullable Prize prize, @NotNull Crate crate) {
        int pulls;
        if (prize == null) {
            if (MiscUtils.isLogging()) {
                plugin.getComponentLogger().warn("No prize was found when giving {} a prize.", (Object)player.getName());
            }
            return;
        }
        Prize prize2 = prize = prize.hasPermission(player) ? prize.getAlternativePrize() : prize;
        if (!player.isOp() && (pulls = PrizeManager.getCurrentPulls(prize, crate)) != -1 && pulls < prize.getMaxPulls()) {
            YamlConfiguration yamlConfiguration = Files.data.getConfiguration();
            yamlConfiguration.set("Prizes." + crate.getFileName() + "." + prize.getSectionName() + ".Pulls", (Object)(pulls + 1));
            PrizeManager.queueDataSave();
        }
        for (ItemStack itemStack : prize.getEditorItems()) {
            if (!MiscUtils.isInventoryFull(player)) {
                player.getInventory().addItem(new ItemStack[]{itemStack});
                continue;
            }
            player.getWorld().dropItemNaturally(player.getLocation(), itemStack);
        }
        if (!prize.getItemBuilders().isEmpty()) {
            boolean isPlaceholderAPIEnabled = Support.placeholder_api.isEnabled();
            for (ItemBuilder item : prize.getItemBuilders()) {
                if (isPlaceholderAPIEnabled) {
                    List<String> displayLore;
                    String displayName = item.getDisplayName();
                    if (!displayName.isEmpty()) {
                        item.setDisplayName(ColorUtils.color(PlaceholderAPI.setPlaceholders(player, displayName)));
                    }
                    if (!(displayLore = item.getDisplayLore()).isEmpty()) {
                        ArrayList<String> lore = new ArrayList<String>();
                        displayLore.forEach(line -> lore.add(ColorUtils.color(PlaceholderAPI.setPlaceholders(player, line))));
                        item.setDisplayLore(lore);
                    }
                }
                if (!MiscUtils.isInventoryFull(player)) {
                    MiscUtils.addItem(player, ((ItemBuilder)item.setPlayer(player)).asItemStack());
                    continue;
                }
                player.getWorld().dropItemNaturally(player.getLocation(), ((ItemBuilder)item.setPlayer(player)).asItemStack());
            }
        }
        for (String string : crate.getPrizeCommands()) {
            PrizeManager.runCommands(player, prize, crate, string);
        }
        for (String string : prize.getCommands()) {
            PrizeManager.runCommands(player, prize, crate, string);
        }
        prize.broadcast(player, crate);
        if (!crate.getPrizeMessage().isEmpty() && prize.getMessages().isEmpty()) {
            for (String string : crate.getPrizeMessage()) {
                PrizeManager.sendMessage(player, prize, crate, string);
            }
            return;
        }
        for (String string : prize.getMessages()) {
            PrizeManager.sendMessage(player, prize, crate, string);
        }
    }

    private static void runCommands(@NotNull Player player, @NotNull Prize prize, @NotNull Crate crate, @NotNull String command) {
        String cmd = command;
        if (cmd.contains("%random%:")) {
            StringBuilder commandBuilder = new StringBuilder();
            for (String word : cmd.split(" ")) {
                if (word.startsWith("%random%:")) {
                    word = word.replace("%random%:", "");
                    try {
                        long min = Long.parseLong(word.split("-")[0]);
                        long max = Long.parseLong(word.split("-")[1]);
                        commandBuilder.append(MiscUtils.pickNumber(min, max)).append(" ");
                    }
                    catch (Exception e) {
                        commandBuilder.append("1 ");
                        if (!MiscUtils.isLogging()) continue;
                        plugin.getComponentLogger().warn("The prize {} in the {} crate has caused an error when trying to run a command.", (Object)prize.getPrizeName(), (Object)prize.getCrateName());
                        plugin.getComponentLogger().warn("Command: {}", (Object)cmd);
                    }
                    continue;
                }
                commandBuilder.append(word).append(" ");
            }
            cmd = commandBuilder.toString();
            cmd = cmd.substring(0, cmd.length() - 1);
        }
        if (Support.placeholder_api.isEnabled()) {
            cmd = PlaceholderAPI.setPlaceholders((Player)player, (String)cmd);
        }
        String maxPulls = String.valueOf(prize.getMaxPulls());
        String pulls = String.valueOf(PrizeManager.getCurrentPulls(prize, crate));
        String prizeName = prize.getPrizeName().replaceAll("%maxpulls%", maxPulls).replaceAll("%pulls%", pulls);
        MiscUtils.sendCommand(cmd.replaceAll("%player%", Matcher.quoteReplacement(player.getName())).replaceAll("%reward%", Matcher.quoteReplacement(prizeName)).replaceAll("%reward_stripped%", Matcher.quoteReplacement(prize.getStrippedName())).replaceAll("%crate_fancy%", Matcher.quoteReplacement(crate.getCrateName())).replaceAll("%crate%", Matcher.quoteReplacement(crate.getFileName())).replaceAll("%maxpulls%", maxPulls).replaceAll("%pulls%", pulls));
    }

    private static void sendMessage(@NotNull Player player, @NotNull Prize prize, @NotNull Crate crate, String message) {
        if (message.isEmpty()) {
            return;
        }
        String maxPulls = String.valueOf(prize.getMaxPulls());
        String pulls = String.valueOf(PrizeManager.getCurrentPulls(prize, crate));
        String prizeName = prize.getPrizeName().replaceAll("%maxpulls%", maxPulls).replaceAll("%pulls%", pulls);
        String defaultMessage = message.replaceAll("%player%", Matcher.quoteReplacement(player.getName())).replaceAll("%reward%", Matcher.quoteReplacement(prizeName)).replaceAll("%reward_stripped%", Matcher.quoteReplacement(prize.getStrippedName())).replaceAll("%crate%", Matcher.quoteReplacement(crate.getCrateName())).replaceAll("%maxpulls%", maxPulls).replaceAll("%pulls%", pulls);
        MsgUtils.sendMessage((CommandSender)player, Support.placeholder_api.isEnabled() ? PlaceholderAPI.setPlaceholders((Player)player, (String)defaultMessage) : defaultMessage, false);
    }

    public static int getCurrentPulls(Prize prize, Crate crate) {
        if (prize.getMaxPulls() == -1) {
            return 0;
        }
        YamlConfiguration configuration = Files.data.getConfiguration();
        ConfigurationSection section = configuration.getConfigurationSection("Prizes." + crate.getFileName() + "." + prize.getSectionName());
        if (section == null) {
            return 0;
        }
        return section.getInt("Pulls", 0);
    }

    public static void queueDataSave() {
        if (pendingDataSave != null && !pendingDataSave.isCancelled()) {
            return;
        }
        pendingDataSave = new FoliaRunnable(plugin.getServer().getGlobalRegionScheduler()) {
            @Override
            public void run() {
                PrizeManager.flushDataSave();
            }
        }.runDelayed(plugin, 40L);
    }

    public static void flushDataSave() {
        if (pendingDataSave != null) {
            pendingDataSave.cancel();
            pendingDataSave = null;
        }
        Files.data.save();
    }

    public static void givePrize(@NotNull Player player, final @NotNull Crate crate, @Nullable Prize prize) {
        if (prize != null) {
            PrizeManager.givePrize(player, prize, crate);
            if (prize.useFireworks()) {
                MiscUtils.spawnFirework(player.getLocation().add(0.0, 1.0, 0.0), null);
            }
            plugin.getServer().getPluginManager().callEvent((Event)new PlayerPrizeEvent(player, crate, prize));
        } else {
            Messages.prize_error.sendMessage((CommandSender)player, (Map<String, String>)new HashMap<String, String>(){
                {
                    this.put("{crate}", crate.getCrateName());
                }
            });
        }
    }

    public static void getPrize(@NotNull Crate crate, @NotNull Inventory inventory, int slot, @NotNull Player player) {
        ItemStack item = inventory.getItem(slot);
        if (item == null) {
            return;
        }
        Prize prize = crate.getPrize(item);
        PrizeManager.givePrize(player, prize, crate);
    }

    @Nullable
    public static Tier getTier(@NotNull Crate crate) {
        int index;
        if (crate.getTiers().isEmpty()) {
            return null;
        }
        Random random = MiscUtils.getRandom();
        double weight = 0.0;
        List<Tier> tiers = crate.getTiers();
        for (Tier tier : tiers) {
            weight += tier.getWeight();
        }
        double value = random.nextDouble() * weight;
        for (index = 0; index < tiers.size() - 1 && !((value -= tiers.get(index).getWeight()) < 0.0); ++index) {
        }
        return tiers.get(index);
    }
}


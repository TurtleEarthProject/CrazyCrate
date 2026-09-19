package com.badbones69.crazycrates.listeners.crates;

import ch.jalu.configme.SettingsManager;
import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.events.CrateOpenEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.ConfigKeys;
import com.badbones69.crazycrates.common.utils.Methods;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.managers.events.EventManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;

import java.util.List;

import libs.com.ryderbelserion.vital.paper.api.enums.Support;
import me.clip.placeholderapi.PlaceholderAPI;
import com.badbones69.crazycrates.utils.ColorUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import us.crazycrew.crazycrates.api.enums.types.CrateType;

public class CrateOpenListener
        implements Listener {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final CrateManager crateManager = this.plugin.getCrateManager();
    private final BukkitUserManager userManager = this.plugin.getUserManager();
    private final SettingsManager config = ConfigManager.getConfig();

    @EventHandler
    public void onCrateOpen(CrateOpenEvent event) {
        List commands;
        boolean commandToggle;
        Player player = event.getPlayer();
        Crate crate = event.getCrate();
        String fileName = crate.getFileName();
        String fancyName = crate.getCrateName();
        if (crate.getCrateType() != CrateType.menu && (crate.getPrizes().isEmpty() || !crate.canWinPrizes(player))) {
            Messages.no_prizes_found.sendMessage(player, "{crate}", fancyName);
            this.crateManager.removePlayerFromOpeningList(player);
            this.crateManager.removePlayerKeyType(player);
            event.setCancelled(true);
            return;
        }
        if (this.config.getProperty(ConfigKeys.use_new_permission_system).booleanValue()) {
            if (player.hasPermission("crazycrates.deny.open." + fileName)) {
                Messages.no_crate_permission.sendMessage(player, "{crate}", fancyName);
                this.crateManager.removePlayerFromOpeningList(player);
                this.crateManager.removeCrateInUse(player);
                event.setCancelled(true);
                return;
            }
        } else if (!player.hasPermission("crazycrates.open." + fileName)) {
            Messages.no_crate_permission.sendMessage((CommandSender) player, "{crate}", fancyName);
            this.crateManager.removePlayerFromOpeningList(player);
            this.crateManager.removeCrateInUse(player);
            event.setCancelled(true);
            return;
        }
        this.crateManager.addPlayerToOpeningList(player, crate);
        if (crate.getCrateType() != CrateType.cosmic) {
            this.userManager.addOpenedCrate(player.getUniqueId(), fileName);
            com.badbones69.crazycrates.support.TurtleTopSupport.addPoint(player.getName(), "crazy-crates-" + fileName, 1);
        }
        YamlConfiguration configuration = event.getConfiguration();
        String broadcastMessage = configuration.getString("Crate.BroadCast", "");
        boolean broadcastToggle = configuration.getBoolean("Crate.OpeningBroadCast", false);
        if (broadcastToggle && crate.getCrateType() != CrateType.cosmic && !event.isSilent()
                && !broadcastMessage.trim().isEmpty()) {
            String builder = Support.placeholder_api.isEnabled()
                    ? PlaceholderAPI.setPlaceholders((Player) player, (String) broadcastMessage)
                    : broadcastMessage;
            String message = builder.replaceAll("%crate%", fancyName)
                    .replaceAll("%prefix%", Methods.getPrefix())
                    .replaceAll("%player%", player.getName());
            this.plugin.getServer().broadcast(ColorUtils.toComponent(message));
        }
        boolean bl = commandToggle = configuration.contains("Crate.opening-command")
                && configuration.getBoolean("Crate.opening-command.toggle");
        if (commandToggle && !(commands = configuration.getStringList("Crate.opening-command.commands")).isEmpty()) {
            commands.forEach(line -> {
                String builder = Support.placeholder_api.isEnabled()
                        ? PlaceholderAPI.setPlaceholders(player, ((String) line)
                                .replaceAll("%crate%", fileName)
                                .replaceAll("%prefix%", Methods.getPrefix())
                                .replaceAll("%player%", player.getName()))
                        : ((String) line)
                                .replaceAll("%crate%", fileName)
                                .replaceAll("%prefix%", Methods.getPrefix())
                                .replaceAll("%player%", player.getName());
                this.plugin.getServer().dispatchCommand(this.plugin.getServer().getConsoleSender(),
                        ColorUtils.plainText(builder));
            });
        }
        EventManager.logEvent(event.getEventType(), player, player, crate, event.getKeyType(), 1);
    }
}

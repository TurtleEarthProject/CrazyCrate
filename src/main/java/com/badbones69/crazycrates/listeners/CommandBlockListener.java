package com.badbones69.crazycrates.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandBlockListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String message = e.getMessage().toLowerCase();
        if (message.startsWith("/key") || message.startsWith("/keys")) {
            if (!e.getPlayer().hasPermission("crazycrates.admin")) {
                e.setCancelled(true);
                // No message to avoid any spam, just block it as requested
            }
        }
    }
}

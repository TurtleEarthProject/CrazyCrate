/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package com.badbones69.crazycrates.tasks.crates.other.quadcrates;

import com.badbones69.crazycrates.tasks.crates.other.quadcrates.QuadCrateManager;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SessionManager {
    public final boolean inSession(@NotNull Player player) {
        if (QuadCrateManager.getCrateSessions().isEmpty()) {
            return false;
        }
        UUID uuid = player.getUniqueId();
        for (QuadCrateManager quadCrateManager : QuadCrateManager.getCrateSessions()) {
            if (!quadCrateManager.getPlayer().getUniqueId().equals(uuid)) continue;
            return true;
        }
        return false;
    }

    @Nullable
    public final QuadCrateManager getSession(@NotNull Player player) {
        UUID uuid = player.getUniqueId();
        for (QuadCrateManager quadCrateManager : QuadCrateManager.getCrateSessions()) {
            if (!quadCrateManager.getPlayer().getUniqueId().equals(uuid)) continue;
            return quadCrateManager;
        }
        return null;
    }

    public static void endCrates() {
        if (!QuadCrateManager.getCrateSessions().isEmpty()) {
            QuadCrateManager.getCrateSessions().forEach(session -> session.endCrate(true));
            QuadCrateManager.getCrateSessions().clear();
        }
    }
}


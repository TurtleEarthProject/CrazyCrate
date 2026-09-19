/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.sound.Sound$Source
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Server
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockState
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.jetbrains.annotations.NotNull
 */
package com.badbones69.crazycrates.tasks.crates.other.quadcrates;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.ChestManager;
import com.badbones69.crazycrates.api.SpiralManager;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.crates.CrateLocation;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.ConfigKeys;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.support.holograms.HologramManager;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import libs.com.ryderbelserion.vital.paper.util.scheduler.FoliaRunnable;
import libs.com.ryderbelserion.vital.paper.util.structures.StructureManager;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

public class QuadCrateManager {
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final CrateManager crateManager = this.plugin.getCrateManager();
    private final BukkitUserManager userManager = this.plugin.getUserManager();
    private static final List<QuadCrateManager> crateSessions = new ArrayList<QuadCrateManager>();
    private final QuadCrateManager instance;
    private final Player player;
    private final boolean checkHand;
    private final Crate crate;
    private final KeyType keyType;
    private final List<Entity> displayedRewards = new ArrayList<Entity>();
    private final Location spawnLocation;
    private final Location lastLocation;
    private final List<Location> crateLocations = new ArrayList<Location>();
    private final Map<Location, Boolean> cratesOpened = new HashMap<Location, Boolean>();
    private final Map<Location, BlockState> quadCrateChests = new HashMap<Location, BlockState>();
    private final Map<Location, BlockState> oldBlocks = new HashMap<Location, BlockState>();
    private final Color particleColor;
    private final Particle particle;
    private final StructureManager handler;

    public QuadCrateManager(@NotNull Player player, @NotNull Crate crate, @NotNull KeyType keyType, @NotNull Location spawnLocation, boolean inHand, @NotNull StructureManager handler) {
        this.instance = this;
        this.player = player;
        this.crate = crate;
        this.keyType = keyType;
        this.checkHand = inHand;
        this.spawnLocation = spawnLocation;
        this.lastLocation = player.getLocation();
        this.lastLocation.setPitch(0.0f);
        this.handler = handler;
        this.particle = crate.getParticle();
        this.particleColor = crate.getColor();
        crateSessions.add(this.instance);
    }

    public void startCrate() {
        CrateLocation crateLocation;
        if (this.spawnLocation.clone().subtract(0.0, 1.0, 0.0).getBlock().getType() == Material.AIR) {
            Messages.not_on_block.sendMessage((CommandSender)this.player);
            this.crateManager.removePlayerFromOpeningList(this.player);
            crateSessions.remove(this.instance);
            return;
        }
        if (this.crateManager.getCrateSchematics().isEmpty()) {
            Messages.no_schematics_found.sendMessage((CommandSender)this.player);
            this.crateManager.removePlayerFromOpeningList(this.player);
            crateSessions.remove(this.instance);
            return;
        }
        Set<Location> structureLocations = this.handler.getBlocks(this.spawnLocation.clone());
        for (Location location : structureLocations) {
            Block block = location.getBlock();
            Material type = block.getType();
            if (this.handler.getBlockBlacklist().contains(type)) {
                Messages.needs_more_room.sendMessage((CommandSender)this.player);
                this.crateManager.removePlayerFromOpeningList(this.player);
                crateSessions.remove(this.instance);
                return;
            }
            if (type == Material.AIR) continue;
            this.oldBlocks.put(block.getLocation(), block.getState());
        }
        ArrayList<Entity> shovePlayers = new ArrayList<Entity>();
        for (Entity entity2 : this.player.getNearbyEntities(3.0, 3.0, 3.0)) {
            if (!(entity2 instanceof Player)) continue;
            Player entityPlayer = (Player)entity2;
            for (QuadCrateManager ongoingCrate : crateSessions) {
                if (entityPlayer.getUniqueId() != ongoingCrate.player.getUniqueId()) continue;
                Messages.too_close_to_another_player.sendMessage((CommandSender)this.player, "{player}", entityPlayer.getName());
                this.crateManager.removePlayerFromOpeningList(this.player);
                crateSessions.remove(this.instance);
                return;
            }
            shovePlayers.add(entity2);
        }
        if (!this.userManager.takeKeys(this.player.getUniqueId(), this.crate.getFileName(), this.keyType, this.crate.useRequiredKeys() ? this.crate.getRequiredKeys() : 1, this.checkHand)) {
            this.crateManager.removePlayerFromOpeningList(this.player);
            crateSessions.remove(this.instance);
            return;
        }
        HologramManager hologramManager = this.crateManager.getHolograms();
        if (hologramManager != null && this.crate.getHologram().isEnabled() && (crateLocation = this.crateManager.getCrateLocation(this.spawnLocation)) != null) {
            hologramManager.removeHologram(crateLocation.getID());
        }
        shovePlayers.forEach(entity -> entity.getLocation().toVector().subtract(this.spawnLocation.clone().toVector()).normalize().setY(1));
        this.addCrateLocations(2, 1, 0);
        this.addCrateLocations(0, 1, 2);
        this.addCrateLocations(-2, 1, 0);
        this.addCrateLocations(0, 1, -2);
        this.crateLocations.forEach(loc -> this.cratesOpened.put((Location)loc, false));
        for (Location loc3 : this.crateLocations) {
            if (!this.crateLocations.contains(loc3)) continue;
            this.quadCrateChests.put(loc3.clone(), loc3.getBlock().getState());
        }
        this.handler.pasteStructure(this.spawnLocation, true);
        this.player.teleportAsync(this.spawnLocation.clone().toCenterLocation().add(0.0, 1.0, 0.0));
        this.crateManager.addQuadCrateTask(this.player, new FoliaRunnable(this.player.getScheduler(), null){
            double radius;
            int crateNumber;
            int tickTillSpawn;
            Location particleLocation;
            List<Location> spiralLocationsClockwise;
            List<Location> spiralLocationsCounterClockwise;
            {
                this.radius = 0.0;
                this.crateNumber = 0;
                this.tickTillSpawn = 0;
                this.particleLocation = QuadCrateManager.this.crateLocations.get(this.crateNumber).clone().add(0.5, 3.0, 0.5);
                this.spiralLocationsClockwise = SpiralManager.getSpiralLocationClockwise(this.particleLocation);
                this.spiralLocationsCounterClockwise = SpiralManager.getSpiralLocationCounterClockwise(this.particleLocation);
            }

            @Override
            public void run() {
                if (this.tickTillSpawn < 60) {
                    QuadCrateManager.this.spawnParticles(QuadCrateManager.this.particleColor, this.spiralLocationsClockwise.get(this.tickTillSpawn), this.spiralLocationsCounterClockwise.get(this.tickTillSpawn));
                    ++this.tickTillSpawn;
                } else {
                    QuadCrateManager.this.crate.playSound(QuadCrateManager.this.player, QuadCrateManager.this.player.getLocation(), "cycle-sound", "block.stone.step", Sound.Source.PLAYER);
                    Block chest = QuadCrateManager.this.crateLocations.get(this.crateNumber).getBlock();
                    chest.setType(Material.CHEST);
                    ChestManager.rotateChest(chest, this.crateNumber);
                    if (this.crateNumber == 3) {
                        QuadCrateManager.this.crateManager.endQuadCrate(QuadCrateManager.this.player);
                    } else {
                        this.tickTillSpawn = 0;
                        ++this.crateNumber;
                        this.radius = 0.0;
                        this.particleLocation = QuadCrateManager.this.crateLocations.get(this.crateNumber).clone().add(0.5, 3.0, 0.5);
                        this.spiralLocationsClockwise = SpiralManager.getSpiralLocationClockwise(this.particleLocation);
                        this.spiralLocationsCounterClockwise = SpiralManager.getSpiralLocationCounterClockwise(this.particleLocation);
                    }
                }
            }
        }.runAtFixedRate(this.plugin, 0L, 1L));
        this.crateManager.addCrateTask(this.player, new FoliaRunnable(this.player.getScheduler(), null){

            @Override
            public void run() {
                QuadCrateManager.this.endCrate(true);
                Messages.out_of_time.sendMessage((CommandSender)QuadCrateManager.this.player, "{crate}", QuadCrateManager.this.crate.getCrateName());
                QuadCrateManager.this.crate.playSound(QuadCrateManager.this.player, QuadCrateManager.this.player.getLocation(), "stop-sound", "entity.player.levelup", Sound.Source.PLAYER);
            }
        }.runDelayed(this.plugin, ConfigManager.getConfig().getProperty(ConfigKeys.quad_crate_timer) * 20));
    }

    public void endCrate(boolean immediately) {
        final Server server = this.plugin.getServer();
        new FoliaRunnable(server.getGlobalRegionScheduler()){

            @Override
            public void run() {
                CrateLocation crateLocation;
                QuadCrateManager.this.crateLocations.forEach(location -> server.getRegionScheduler().run((Plugin)QuadCrateManager.this.plugin, location, schedulerTask -> QuadCrateManager.this.quadCrateChests.get(location).update(true, false)));
                for (Entity displayedReward : QuadCrateManager.this.displayedRewards) {
                    displayedReward.getScheduler().run((Plugin)QuadCrateManager.this.plugin, scheduledTask -> displayedReward.remove(), null);
                }
                QuadCrateManager.this.player.teleportAsync(QuadCrateManager.this.lastLocation);
                QuadCrateManager.this.handler.removeStructure();
                QuadCrateManager.this.oldBlocks.keySet().forEach(location -> server.getRegionScheduler().run((Plugin)QuadCrateManager.this.plugin, location, schedulerTask -> QuadCrateManager.this.oldBlocks.get(location).update(true, false)));
                HologramManager manager = QuadCrateManager.this.crateManager.getHolograms();
                if (manager != null && QuadCrateManager.this.crate.getHologram().isEnabled() && (crateLocation = QuadCrateManager.this.crateManager.getCrateLocation(QuadCrateManager.this.spawnLocation)) != null) {
                    manager.createHologram(QuadCrateManager.this.spawnLocation, QuadCrateManager.this.crate, crateLocation.getID());
                }
                QuadCrateManager.this.crateManager.endCrate(QuadCrateManager.this.player);
                QuadCrateManager.this.crateManager.removePlayerFromOpeningList(QuadCrateManager.this.player);
                crateSessions.remove(QuadCrateManager.this.instance);
            }
        }.runDelayed(this.plugin, immediately ? 0L : 5L);
    }

    public void endCrateForce(boolean removeForce) {
        this.oldBlocks.keySet().forEach(location -> this.oldBlocks.get(location).update(true, false));
        this.crateLocations.forEach(location -> this.quadCrateChests.get(location).update(true, false));
        this.displayedRewards.forEach(Entity::remove);
        this.player.teleportAsync(this.lastLocation);
        if (removeForce) {
            this.crateManager.removePlayerFromOpeningList(this.player);
            crateSessions.remove(this.instance);
        }
        this.handler.removeStructure();
    }

    public void addCrateLocations(int x, int y, int z) {
        this.crateLocations.add(this.spawnLocation.clone().add((double)x, (double)y, (double)z));
    }

    private void spawnParticles(@NotNull Color particleColor, @NotNull Location location1, @NotNull Location location2) {
        if (this.particle == Particle.DUST) {
            location1.getWorld().spawnParticle(this.particle, location1, 0, (Object)new Particle.DustOptions(particleColor, 1.0f));
            location2.getWorld().spawnParticle(this.particle, location2, 0, (Object)new Particle.DustOptions(particleColor, 1.0f));
            return;
        }
        location1.getWorld().spawnParticle(this.particle, location1, 0);
        location2.getWorld().spawnParticle(this.particle, location2, 0);
    }

    public static List<QuadCrateManager> getCrateSessions() {
        return crateSessions;
    }

    @NotNull
    public final Player getPlayer() {
        return this.player;
    }

    @NotNull
    public final List<Location> getCrateLocations() {
        return this.crateLocations;
    }

    @NotNull
    public final Map<Location, Boolean> getCratesOpened() {
        return this.cratesOpened;
    }

    @NotNull
    public final Crate getCrate() {
        return this.crate;
    }

    @NotNull
    public final List<Entity> getDisplayedRewards() {
        return this.displayedRewards;
    }

    public final boolean allCratesOpened() {
        for (Map.Entry<Location, Boolean> location : this.cratesOpened.entrySet()) {
            if (location.getValue().booleanValue()) continue;
            return false;
        }
        return true;
    }
}


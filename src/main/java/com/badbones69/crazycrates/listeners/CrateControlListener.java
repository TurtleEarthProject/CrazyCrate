package com.badbones69.crazycrates.listeners;

import com.badbones69.crazycrates.api.PrizeManager;
import com.badbones69.crazycrates.api.events.PlayerPrizeEvent;
import com.badbones69.crazycrates.api.objects.Prize;
import com.badbones69.crazycrates.managers.events.EventManager;

import ch.jalu.configme.SettingsManager;
import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.enums.Messages;
import com.badbones69.crazycrates.api.events.KeyCheckEvent;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.api.objects.crates.CrateLocation;
import com.badbones69.crazycrates.common.config.ConfigManager;
import com.badbones69.crazycrates.common.config.impl.ConfigKeys;
import com.badbones69.crazycrates.managers.BukkitUserManager;
import com.badbones69.crazycrates.managers.InventoryManager;
import com.badbones69.crazycrates.managers.events.enums.EventType;
import com.badbones69.crazycrates.tasks.crates.CrateManager;
import com.badbones69.crazycrates.tasks.menus.CrateMainMenu;
import com.badbones69.crazycrates.utils.ItemUtils;
import com.badbones69.crazycrates.utils.MiscUtils;
import libs.com.ryderbelserion.vital.paper.util.scheduler.FoliaRunnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import us.crazycrew.crazycrates.api.enums.types.CrateType;
import us.crazycrew.crazycrates.api.enums.types.KeyType;

public class CrateControlListener
implements Listener {
    private static final long MASS_OPEN_COOLDOWN_MILLIS = 3000L;
    private final CrazyCrates plugin = CrazyCrates.getPlugin();
    private final InventoryManager inventoryManager = this.plugin.getInventoryManager();
    private final SettingsManager config = ConfigManager.getConfig();
    private final CrateManager crateManager = this.plugin.getCrateManager();
    private final BukkitUserManager userManager = this.plugin.getUserManager();
    private final Map<UUID, Long> massOpenCooldowns = new ConcurrentHashMap<>();

    @EventHandler
    public void onGroundClick(PlayerInteractEvent event) {
        boolean isKey;
        Player player = event.getPlayer();
        if (!event.getAction().isRightClick()) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        boolean bl = isKey = event.getHand() == EquipmentSlot.OFF_HAND ? this.crateManager.isKey(player.getInventory().getItemInOffHand()) : this.crateManager.isKey(player.getInventory().getItemInMainHand());
        if (isKey) {
            event.setUseItemInHand(Event.Result.DENY);
        }
    }

    @EventHandler
    public void onCrateInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isLeftClick()) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Location location = block.getLocation();
        CrateLocation crateLocation = this.crateManager.getCrateLocation(location);
        if (crateLocation == null) {
            return;
        }
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (player.getGameMode() == GameMode.CREATIVE && player.isSneaking() && player.hasPermission("crazycrates.admin")) {
            String arg2;
            String arg1 = MiscUtils.location(location, true);
            if (arg1.equals(arg2 = MiscUtils.location(crateLocation.getLocation(), true))) {
                this.crateManager.removeCrateLocation(crateLocation.getID());
                Messages.removed_physical_crate.sendMessage((CommandSender)player, "{id}", crateLocation.getID());
            }
            return;
        }
        this.preview(player, crateLocation.getCrate(), false);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onRightClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!event.getAction().isRightClick() || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }
        Location location = clickedBlock.getLocation();
        CrateLocation crateLocation = this.crateManager.getCrateLocation(location);
        if (crateLocation == null) {
            return;
        }
        Crate crate = crateLocation.getCrate();
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (crate.getCrateType() == CrateType.menu) {
            this.preview(player, crate, true);
            return;
        }
        KeyCheckEvent key = new KeyCheckEvent(player, crateLocation);
        player.getServer().getPluginManager().callEvent((Event)key);
        if (key.isCancelled()) {
            return;
        }
        boolean hasKey = false;
        boolean isPhysical = false;
        boolean useQuickCrateAgain = false;
        int requiredKeys = crate.getRequiredKeys();
        String fileName = crate.getFileName();
        int totalKeys = this.userManager.getTotalKeys(player.getUniqueId(), fileName);
        String fancyName = crate.getCrateName();
        if (requiredKeys > 0 && totalKeys < requiredKeys) {
            HashMap<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("{required_amount}", String.valueOf(requiredKeys));
            placeholders.put("{key_amount}", String.valueOf(requiredKeys));
            placeholders.put("{amount}", String.valueOf(totalKeys));
            placeholders.put("{crate}", fancyName);
            placeholders.put("{key}", crate.getKeyName());
            Messages.not_enough_keys.sendMessage((CommandSender)player, placeholders);
            this.lackingKey(player, crate, location, false);
            key.setCancelled(true);
            return;
        }
        ItemStack itemStack = player.getInventory().getItemInMainHand();
        if (this.config.getProperty(ConfigKeys.physical_accepts_physical_keys).booleanValue() && crate.getCrateType() != CrateType.crate_on_the_go && ItemUtils.isSimilar(itemStack, crate)) {
            hasKey = true;
            isPhysical = true;
        } else if (this.config.getProperty(ConfigKeys.physical_accepts_virtual_keys).booleanValue() && this.userManager.getVirtualKeys(player.getUniqueId(), fileName) >= 1) {
            hasKey = true;
        }
        if (hasKey) {
            KeyType keyType;
            if (this.crateManager.isInOpeningList(player) && this.crateManager.getOpeningCrate(player).getCrateType() == CrateType.quick_crate && this.crateManager.isCrateInUse(player) && this.crateManager.getCrateInUseLocation(player).equals((Object)crateLocation.getLocation())) {
                useQuickCrateAgain = true;
            }
            if (!useQuickCrateAgain) {
                if (this.crateManager.isInOpeningList(player)) {
                    Messages.already_opening_crate.sendMessage((CommandSender)player, "{crate}", fancyName);
                    return;
                }
                if (this.crateManager.getCratesInUse().containsValue(crateLocation.getLocation())) {
                    Messages.crate_in_use.sendMessage((CommandSender)player, "{crate}", fancyName);
                    return;
                }
            }
            if (MiscUtils.isInventoryFull(player)) {
                Messages.inventory_not_empty.sendMessage((CommandSender)player, "{crate}", fancyName);
                return;
            }
            if (useQuickCrateAgain) {
                this.crateManager.endQuickCrate(player, crateLocation.getLocation(), crate, true);
            }
            if (player.isSneaking()) {
                UUID playerId = player.getUniqueId();
                long now = System.currentTimeMillis();
                long cooldownUntil = this.massOpenCooldowns.getOrDefault(playerId, 0L);
                if (cooldownUntil > now) {
                    player.sendMessage("Bạn phải đợi thêm 3 giây để có thể hành động");
                    return;
                }
                this.massOpenCooldowns.remove(playerId, cooldownUntil);

                int amount = 10;
                int keys = this.userManager.getVirtualKeys(player.getUniqueId(), fileName);
                if (keys < amount) {
                    HashMap<String, String> placeholders = new HashMap<>();
                    placeholders.put("{required_amount}", String.valueOf(amount));
                    placeholders.put("{key_amount}", String.valueOf(amount));
                    placeholders.put("{amount}", String.valueOf(keys));
                    placeholders.put("{crate}", fancyName);
                    placeholders.put("{key}", crate.getKeyName());
                    Messages.not_enough_keys.sendMessage((CommandSender) player, placeholders);
                    return;
                }

                this.crateManager.addPlayerToOpeningList(player, crate);
                new FoliaRunnable(player.getScheduler(), null) {
                    private int used;

                    @Override
                    public void run() {
                        if (!player.isOnline() || this.used >= amount || MiscUtils.isInventoryFull(player)) {
                            this.finish();
                            return;
                        }
                        Prize prize = crate.pickPrize(player);
                        PrizeManager.givePrize(player, prize, crate);
                        plugin.getServer().getPluginManager().callEvent(new PlayerPrizeEvent(player, crate, prize));
                        com.badbones69.crazycrates.support.TurtleTopSupport.addPoint(player.getName(), "crazy-crates-" + fileName, 1);
                        if (prize.useFireworks()) {
                            MiscUtils.spawnFirework(player.getLocation().clone().add(0.5, 1.0, 0.5), null);
                        }
                        this.used++;
                    }

                    private void finish() {
                        if (this.used > 0) {
                            userManager.addOpenedCrate(player.getUniqueId(), fileName, this.used);
                            EventManager.logEvent(EventType.event_crate_opened, player, (CommandSender) player, crate, KeyType.virtual_key, this.used);
                            userManager.takeKeys(player.getUniqueId(), fileName, KeyType.virtual_key, this.used, false);
                            massOpenCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + MASS_OPEN_COOLDOWN_MILLIS);
                        }
                        crateManager.removePlayerFromOpeningList(player);
                        this.cancel();
                    }
                }.runAtFixedRate(this.plugin, 1L, 1L);
                return;
            }

            KeyType keyType2 = keyType = isPhysical ? KeyType.physical_key : KeyType.virtual_key;
            if (crate.getCrateType() == CrateType.cosmic) {
                this.crateManager.addPlayerKeyType(player, keyType);
            }
            this.crateManager.addPlayerToOpeningList(player, crate);
            this.crateManager.openCrate(player, crate, keyType, crateLocation.getLocation(), false, true, EventType.event_crate_opened);
            return;
        }
        this.lackingKey(player, crate, location, true);
        key.setCancelled(true);
    }

    @EventHandler
    public void onPistonPushCrate(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Location location = block.getLocation();
            Crate crate = this.crateManager.getCrateFromLocation(location);
            if (crate == null) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPistonPullCrate(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Location location = block.getLocation();
            Crate crate = this.crateManager.getCrateFromLocation(location);
            if (crate == null) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.crateManager.hasCrateTask(player)) {
            this.crateManager.endCrate(player);
        }
        if (this.crateManager.hasQuadCrateTask(player)) {
            this.crateManager.endQuadCrate(player);
        }
        if (this.crateManager.isInOpeningList(player)) {
            this.crateManager.removePlayerFromOpeningList(player);
        }
    }

    private void lackingKey(Player player, final Crate crate, Location location, boolean sendMessage) {
        final String keyName = crate.getKeyName();
        HashMap<String, String> placeholders = new HashMap<String, String>(){
            {
                this.put("{crate}", crate.getCrateName());
                this.put("{key}", keyName);
            }
        };
        if (crate.getCrateType() != CrateType.crate_on_the_go) {
            if (this.config.getProperty(ConfigKeys.knock_back).booleanValue()) {
                this.knockback(player, location);
            }
            if (this.config.getProperty(ConfigKeys.need_key_sound_toggle).booleanValue()) {
                Sound sound = Sound.sound((Key)Key.key((String)this.config.getProperty(ConfigKeys.need_key_sound)), (Sound.Source)Sound.Source.PLAYER, (float)1.0f, (float)1.0f);
                player.playSound(sound);
            }
            if (sendMessage) {
                Messages.no_keys.sendMessage((CommandSender)player, (Map<String, String>)placeholders);
            }
        }
    }

    private void knockback(Player player, Location location) {
        Vector vector = player.getLocation().toVector().subtract(location.toVector()).normalize().multiply(1).setY(0.1);
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().setVelocity(vector);
            return;
        }
        player.setVelocity(vector);
    }

    private void preview(Player player, Crate crate, boolean skipTypeCheck) {
        if (skipTypeCheck || crate.getCrateType() == CrateType.menu) {
            if (!this.crateManager.isInOpeningList(player) && this.config.getProperty(ConfigKeys.enable_crate_menu).booleanValue()) {
                new CrateMainMenu(player, this.config.getProperty(ConfigKeys.inventory_name), this.config.getProperty(ConfigKeys.inventory_rows)).open();
            } else {
                Messages.feature_disabled.sendMessage((CommandSender)player);
            }
        } else if (crate.isPreviewEnabled()) {
            this.inventoryManager.openNewCratePreview(player, crate);
        } else {
            Messages.preview_disabled.sendMessage((CommandSender)player, "{crate}", crate.getCrateName());
        }
    }
}


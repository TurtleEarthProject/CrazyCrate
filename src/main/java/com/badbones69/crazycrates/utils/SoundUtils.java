package com.badbones69.crazycrates.utils;

import com.badbones69.crazycrates.api.objects.Crate;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SoundUtils {

    public static void playSound(@NotNull Crate crate, @NotNull Location location, @NotNull String type,
            @NotNull String fallback) {
        if (!type.isEmpty() || !fallback.isEmpty()) {
            ConfigurationSection section = crate.getFile().getConfigurationSection("Crate.sound");
            if (section != null) {
                SoundEffect sound = new SoundEffect(section, type, fallback);
                sound.play(location);
            }
        }
    }

    /**
     * Resolves a sound key to a Sound using the Bukkit Registry with NamespacedKey.
     * Handles both enum-style names (ENTITY_PLAYER_LEVELUP) and namespaced keys
     * (entity.player.levelup).
     */
    private static @Nullable Sound resolveSound(@Nullable String key) {
        if (key == null || key.isEmpty())
            return null;

        // Normalize: convert ENTITY_PLAYER_LEVELUP → entity.player.levelup
        String normalized = key.toLowerCase().replace("_", ".");

        // Try as a minecraft namespaced key
        Sound sound = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
        if (sound != null)
            return sound;

        // Try the original key as-is (e.g., already dot-separated)
        sound = Registry.SOUNDS.get(NamespacedKey.minecraft(key.toLowerCase()));
        if (sound != null)
            return sound;

        return null;
    }

    public static class SoundEffect {
        private final boolean isEnabled;
        private Sound sound;
        private float volume;
        private float pitch;

        public SoundEffect(@NotNull ConfigurationSection section, @NotNull String type, @NotNull String fallback) {
            this.isEnabled = section.getBoolean(type + ".toggle", false);
            if (this.isEnabled) {
                String key = section.getString(type + ".value", fallback);
                this.sound = resolveSound(key);
                if (this.sound == null) {
                    this.sound = resolveSound(fallback);
                }
                this.volume = (float) section.getDouble(type + ".volume", 1.0);
                this.pitch = (float) section.getDouble(type + ".pitch", 1.0);
            }
        }

        public void play(@NotNull Location location) {
            if (this.isEnabled && this.sound != null) {
                World world = location.getWorld();
                if (world != null) {
                    world.playSound(location, this.sound, this.volume, this.pitch);
                }
            }
        }
    }
}

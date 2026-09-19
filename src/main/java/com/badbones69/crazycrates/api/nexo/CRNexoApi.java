package com.badbones69.crazycrates.api.nexo;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

/**
 * Small adapter around Nexo's public item API. Nexo remains an optional
 * server dependency; callers must first check {@link #isEnabled()}.
 */
public final class CRNexoApi {

    public static boolean isEnabled() {
        return Bukkit.getPluginManager().isPluginEnabled("Nexo");
    }

    @Nullable
    public ItemStack getItemStack(String id) {
        if (!isEnabled() || id == null || id.isBlank()) {
            return null;
        }

        try {
            Class<?> nexoItems = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method exists = nexoItems.getMethod("exists", String.class);
            if (!Boolean.TRUE.equals(exists.invoke(null, id))) {
                return null;
            }

            Method itemFromId = nexoItems.getMethod("itemFromId", String.class);
            Object builder = itemFromId.invoke(null, id);
            if (builder == null) {
                return null;
            }

            Object item = builder.getClass().getMethod("build").invoke(builder);
            return item instanceof ItemStack ? (ItemStack) item : null;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Nexo loads item definitions asynchronously. A later display
            // request can retry after Nexo has finished loading them.
            return null;
        }
    }
}

package com.badbones69.crazycrates.support.holograms;

import com.badbones69.crazycrates.CrazyCrates;
import com.badbones69.crazycrates.api.objects.crates.CrateHologram;
import com.badbones69.crazycrates.api.objects.Crate;
import com.badbones69.crazycrates.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class HologramManager {

    protected CrazyCrates plugin = CrazyCrates.getPlugin();

    public abstract void createHologram(@NotNull final Location location, @NotNull final Crate crate,
            @NotNull final String id);

    public abstract void removeHologram(@NotNull final String id);

    public abstract boolean exists(@NotNull final String id);

    public abstract void purge(final boolean isShutdown);

    public abstract String getName();

    protected @NotNull final String name() {
        return this.plugin.getName().toLowerCase() + "-" + UUID.randomUUID();
    }

    protected @NotNull final String name(final String id) {
        return this.plugin.getName().toLowerCase() + "-" + id;
    }

    protected @NotNull final Vector getVector(@NotNull final Crate crate) {
        return new Vector(0.5, crate.getHologram().getHeight(), 0.5);
    }

    protected @Nullable final String color(@NotNull final String message) {
        return ColorUtils.color(message);
    }

    protected @NotNull final List<String> lines(@NotNull final CrateHologram crateHologram) {
        if (crateHologram.getMessages().isEmpty())
            return Collections.emptyList();

        final List<String> lines = new ArrayList<>();

        crateHologram.getMessages().forEach(line -> lines.add(color(line)));

        return lines;
    }
}
/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.jetbrains.annotations.NotNull
 */
package com.badbones69.crazycrates.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

public class SpiralManager {
    @NotNull
    private static List<Location> getLocations(@NotNull Location center, boolean clockWise) {
        World world = center.getWorld();
        double downWardsDistance = 0.05;
        double expandingRadius = 0.08;
        double centerY = center.getY();
        double radius = 0.0;
        int particleAmount = 10;
        int radiusIncrease = 0;
        int nextLocation = 0;
        double increment = Math.PI * 2 / (double)particleAmount;
        ArrayList<Location> locations = new ArrayList<Location>();
        for (int i = 0; i < 60; ++i) {
            double z;
            double x;
            double angle = (double)nextLocation * increment;
            if (clockWise) {
                x = center.getX() + radius * Math.cos(angle);
                z = center.getZ() + radius * Math.sin(angle);
            } else {
                x = center.getX() - radius * Math.cos(angle);
                z = center.getZ() - radius * Math.sin(angle);
            }
            locations.add(new Location(world, x, centerY, z));
            centerY -= downWardsDistance;
            ++nextLocation;
            if (++radiusIncrease == 6) {
                radiusIncrease = 0;
                radius += expandingRadius;
            }
            if (nextLocation != 10) continue;
            nextLocation = 0;
        }
        return Collections.unmodifiableList(locations);
    }

    @NotNull
    public static List<Location> getSpiralLocationClockwise(@NotNull Location center) {
        return SpiralManager.getLocations(center, true);
    }

    @NotNull
    public static List<Location> getSpiralLocationCounterClockwise(@NotNull Location center) {
        return SpiralManager.getLocations(center, false);
    }
}


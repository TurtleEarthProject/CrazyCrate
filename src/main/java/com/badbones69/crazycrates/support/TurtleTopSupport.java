package com.badbones69.crazycrates.support;

import com.badbones69.crazycrates.CrazyCrates;
import com.turtle.turtletop.core.TurtleTopApi;
import org.bukkit.Bukkit;

public class TurtleTopSupport {

    public static void addPoint(String playerName, String pointName, double amount) {
        if (!Bukkit.getPluginManager().isPluginEnabled("TurtleTop")) return;
        try {
            TurtleTopApi.getInstance().getPlayerDataList().addData(playerName).addPoint(pointName, amount);
            TurtleTopApi.getInstance().getPlayerDataList().markRankDirty();
        } catch (Exception e) {
            CrazyCrates.getPlugin().getLogger().warning("Failed to add TurtleTop point for " + playerName + " to " + pointName + ": " + e.getMessage());
        }
    }
}

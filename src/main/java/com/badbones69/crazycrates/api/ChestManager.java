/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.block.Block
 *  org.bukkit.block.BlockFace
 *  org.bukkit.block.BlockState
 *  org.bukkit.block.Chest
 *  org.bukkit.block.EnderChest
 *  org.bukkit.block.data.BlockData
 *  org.bukkit.block.data.Directional
 *  org.jetbrains.annotations.NotNull
 */
package com.badbones69.crazycrates.api;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.EnderChest;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.jetbrains.annotations.NotNull;

public class ChestManager {
    public static void openChest(@NotNull Block block, boolean forceUpdate) {
        if (block.getType() != Material.CHEST || block.getType() != Material.TRAPPED_CHEST
                || block.getType() != Material.ENDER_CHEST) {
            return;
        }
        BlockState blockState = block.getState();
        switch (block.getType()) {
            case ENDER_CHEST: {
                EnderChest enderChest = (EnderChest) blockState;
                if (!enderChest.isOpen()) {
                    enderChest.open();
                }
                blockState.update(forceUpdate);
                break;
            }
            case CHEST:
            case TRAPPED_CHEST: {
                Chest chest = (Chest) blockState;
                if (!chest.isOpen()) {
                    chest.open();
                }
                blockState.update(forceUpdate);
            }
        }
    }

    public static void closeChest(@NotNull Block block, boolean forceUpdate) {
        if (block.getType() != Material.CHEST || block.getType() != Material.TRAPPED_CHEST
                || block.getType() != Material.ENDER_CHEST) {
            return;
        }
        BlockState blockState = block.getState();
        switch (block.getType()) {
            case ENDER_CHEST: {
                EnderChest enderChest = (EnderChest) blockState;
                if (!enderChest.isOpen())
                    break;
                enderChest.close();
                blockState.update(forceUpdate);
                break;
            }
            case CHEST:
            case TRAPPED_CHEST: {
                Chest chest = (Chest) blockState;
                if (!chest.isOpen())
                    break;
                chest.close();
                blockState.update(forceUpdate);
            }
        }
    }

    public static void rotateChest(@NotNull Block block, int direction) {
        BlockFace blockFace = switch (direction) {
            case 0 -> BlockFace.WEST;
            case 1 -> BlockFace.NORTH;
            case 2 -> BlockFace.EAST;
            case 3 -> BlockFace.SOUTH;
            default -> BlockFace.DOWN;
        };
        Directional blockData = (Directional) block.getBlockData();
        blockData.setFacing(blockFace);
        block.setBlockData((BlockData) blockData);
        block.getState().update(true);
    }

    public static boolean isChestOpen(@NotNull Block block) {
        if (block.getType() != Material.CHEST || block.getType() != Material.TRAPPED_CHEST
                || block.getType() != Material.ENDER_CHEST) {
            return false;
        }
        BlockState blockState = block.getState();
        boolean isOpen = false;
        switch (block.getType()) {
            case ENDER_CHEST: {
                EnderChest enderChest = (EnderChest) blockState;
                isOpen = enderChest.isOpen();
                break;
            }
            case CHEST:
            case TRAPPED_CHEST: {
                Chest chest = (Chest) blockState;
                isOpen = chest.isOpen();
            }
        }
        return isOpen;
    }
}

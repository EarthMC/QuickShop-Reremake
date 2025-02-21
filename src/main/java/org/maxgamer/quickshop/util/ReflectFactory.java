/*
 * This file is a part of project QuickShop, the name is ReflectFactory.java
 *  Copyright (C) PotatoCraft Studio and contributors
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the
 *  Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.maxgamer.quickshop.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandMap;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ItemType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;

/**
 * ReflectFactory is library builtin QuickShop to get/execute stuff that cannot be access with BukkitAPI with reflect way.
 *
 * @author Ghost_chu
 */
public class ReflectFactory {
    /**
     * Get MinecraftServer's TPS
     *
     * @return TPS (e.g 19.92)
     */
    @NotNull
    public static Double getTPS() {
        return QuickShop.getInstance().getTpsWatcher().getAverageTPS();
    }

    @NotNull
    public static String getServerVersion() {
        return Bukkit.getServer().getMinecraftVersion();
    }

    /**
     * Save ItemStack to Json through the NMS.
     *
     * @param bStack ItemStack
     * @return The json for ItemStack.
     */
    @Nullable
    @SuppressWarnings("deprecation")
    public static String convertBukkitItemStackToJson(@NotNull ItemStack bStack) {
        return Bukkit.getUnsafe().serializeItemAsJson(bStack).toString();
    }

    public static CommandMap getCommandMap() {
        return Bukkit.getServer().getCommandMap();
    }

    @Nullable
    public static String getMaterialMinecraftNamespacedKey(Material material) {
        final ItemType itemType = material.asItemType();
        if (itemType == null) {
            return null;
        }

        return itemType.getKey().asString();
    }
}

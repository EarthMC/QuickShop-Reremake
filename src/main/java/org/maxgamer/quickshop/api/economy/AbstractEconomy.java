/*
 * This file is a part of project QuickShop, the name is AbstractEconomy.java
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

package org.maxgamer.quickshop.api.economy;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.util.reload.ReloadResult;
import org.maxgamer.quickshop.util.reload.ReloadStatus;
import org.maxgamer.quickshop.util.reload.Reloadable;

import java.util.UUID;

/**
 * Abstract Economy Core
 */
public abstract class AbstractEconomy implements EconomyCore, Reloadable {

    public AbstractEconomy() {
    }

    /**
     * Getting QuickShop now using type of Economy
     *
     * @return Economy type that QuickShop now using
     */
    public static EconomyType getNowUsing() {
        return EconomyType.fromID(QuickShop.getInstance().getConfig().getInt("economy-type"));
    }

    @Override
    public abstract String toString();

    /**
     * Transfer specific amount of currency from A to B
     * (Developer: This is low layer of Economy System, use EconomyTransaction if possible)
     *
     * @param from     The player who is paying money
     * @param to       The player who is receiving money
     * @param amount   The amount to transfer
     * @param world    The transaction world
     * @param currency The currency name
     * @return successed
     */
    @Override
    public boolean transfer(@NotNull UUID from, @NotNull UUID to, double amount, @NotNull World world, @Nullable String currency) {
        if (!isValid()) {
            return false;
        }
        if (this.getBalance(from, world, currency) >= amount) {
            if (this.withdraw(from, amount, world, currency)) {
                if (this.deposit(to, amount, world, currency)) {
                    this.deposit(from, amount, world, currency);
                    return false;
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public @NotNull String getName() {
        return "BuiltIn-Economy Processor";
    }

    /**
     * Callback for reloading
     *
     * @return Reloading success
     */
    @Override
    public ReloadResult reloadModule() {
        return ReloadResult.builder().status(ReloadStatus.SUCCESS).build();
    }
}

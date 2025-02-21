/*
 * This file is a part of project QuickShop, the name is Trader.java
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

package org.maxgamer.quickshop.economy;

import io.papermc.paper.persistence.PersistentDataContainerView;
import lombok.AllArgsConstructor;
import org.bukkit.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.util.PlayerFinder;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
public class Trader implements OfflinePlayer {


    @Nullable
    private String lastKnownName;
    @NotNull
    private final OfflinePlayer offlinePlayer;

    public static Trader adapt(OfflinePlayer offlinePlayer) {
        return new Trader(offlinePlayer.getName(), offlinePlayer);
    }

    public static Trader adapt(PlayerFinder.PlayerProfile profile) {
        return new Trader(profile.getName(), Bukkit.getOfflinePlayer(profile.getUuid()));
    }

    @Override
    public boolean isOnline() {
        return offlinePlayer.isOnline();
    }

    @Override
    public boolean isConnected() {
        return offlinePlayer.isConnected();
    }

    @Override
    public @Nullable String getName() {
        return lastKnownName == null && offlinePlayer.isOnline() ? lastKnownName = offlinePlayer.getName() : lastKnownName;
    }

    @Override
    public @NotNull UUID getUniqueId() {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public com.destroystokyo.paper.profile.@NotNull PlayerProfile getPlayerProfile() {
        return offlinePlayer.getPlayerProfile();
    }

    @Override
    public boolean isBanned() {
        return offlinePlayer.isBanned();
    }

    @Nullable
    @Override
    public BanEntry<PlayerProfile> ban(@Nullable String s, @Nullable Date date, @Nullable String s1) {
        return offlinePlayer.ban(s, date, s1);
    }

    @Nullable
    @Override
    public BanEntry<PlayerProfile> ban(@Nullable String s, @Nullable Instant instant, @Nullable String s1) {
        return offlinePlayer.ban(s, instant, s1);
    }

    @Nullable
    @Override
    public BanEntry<PlayerProfile> ban(@Nullable String s, @Nullable Duration duration, @Nullable String s1) {
        return offlinePlayer.ban(s, duration, s1);
    }

    @Override
    public boolean isWhitelisted() {
        return offlinePlayer.isWhitelisted();
    }

    @Override
    public void setWhitelisted(boolean value) {
        offlinePlayer.setWhitelisted(value);
    }

    @Override
    public @Nullable Player getPlayer() {
        return offlinePlayer.getPlayer();
    }

    @Override
    public long getFirstPlayed() {
        return offlinePlayer.getFirstPlayed();
    }

    @Override
    public long getLastPlayed() {
        return offlinePlayer.getLastPlayed();
    }

    @Override
    public boolean hasPlayedBefore() {
        return offlinePlayer.hasPlayedBefore();
    }

    @Override
    public @Nullable Location getBedSpawnLocation() {
        return offlinePlayer.getBedSpawnLocation();
    }

    @Override
    public long getLastLogin() {
        return offlinePlayer.getLastLogin();
    }

    @Override
    public long getLastSeen() {
        return offlinePlayer.getLastSeen();
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        offlinePlayer.decrementStatistic(statistic);
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {
        offlinePlayer.decrementStatistic(statistic);
    }

    @Override
    public void setStatistic(@NotNull Statistic statistic, int newValue) throws IllegalArgumentException {
        offlinePlayer.setStatistic(statistic, newValue);
    }

    @Override
    public int getStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
        return offlinePlayer.getStatistic(statistic);
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic, material);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        offlinePlayer.decrementStatistic(statistic, material);
    }

    @Override
    public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
        return offlinePlayer.getStatistic(statistic, material);
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic, material, amount);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {
        offlinePlayer.decrementStatistic(statistic, material, amount);
    }

    @Override
    public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) throws IllegalArgumentException {
        offlinePlayer.setStatistic(statistic, material, newValue);
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic, entityType);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        offlinePlayer.decrementStatistic(statistic, entityType);
    }

    @Override
    public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
        return offlinePlayer.getStatistic(statistic, entityType);
    }

    @Override
    public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) throws IllegalArgumentException {
        offlinePlayer.incrementStatistic(statistic, entityType, amount);
    }

    @Override
    public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {
        offlinePlayer.decrementStatistic(statistic, entityType, amount);
    }

    @Override
    public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) {
        offlinePlayer.setStatistic(statistic, entityType, newValue);
    }

    @Nullable
    @Override
    public Location getLastDeathLocation() {
        return offlinePlayer.getLastDeathLocation();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        return offlinePlayer.serialize();
    }

    @Override
    public boolean isOp() {
        return offlinePlayer.isOp();
    }

    @Override
    public void setOp(boolean value) {
        offlinePlayer.setOp(value);
    }

    @Nullable
    @Override
    public Location getLocation() {
        return offlinePlayer.getLocation();
    }

    @Override
    public PersistentDataContainerView getPersistentDataContainer() {
        return offlinePlayer.getPersistentDataContainer();
    }

    @Nullable
    @Override
    public Location getRespawnLocation() {
        return offlinePlayer.getRespawnLocation();
    }
}

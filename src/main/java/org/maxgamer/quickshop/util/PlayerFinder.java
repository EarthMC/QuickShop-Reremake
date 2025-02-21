/*
 * This file is a part of project QuickShop, the name is PlayerFinder.java
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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.economy.Trader;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * A player finder for finding player by name
 *
 * @author sandtechnology
 * @since 5.1.0.3
 */
public final class PlayerFinder {

    //Hold store for large server
    private static final Map<String, UUID> name2UUIDStash = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Cache<String, UUID> name2UUIDCache = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();
    private static final Cache<UUID, String> uuid2StringCache = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.MINUTES).build();

    private static final Set<UUID> emptyNameUUIDSet = new ConcurrentSkipListSet<>();

    private static volatile boolean useOfflineStash;

    @Nullable
    private static PlayerProfile findProfileByName(String name, java.util.Collection<? extends org.bukkit.OfflinePlayer> players, boolean isOfflinePlayer) {
        for (OfflinePlayer player : players) {
            UUID uuid = player.getUniqueId();
            if (isOfflinePlayer && emptyNameUUIDSet.contains(uuid)) {
                continue;
            }
            String playerName = player.getName();
            if (playerName != null) {
                if (playerName.equalsIgnoreCase(name)) {
                    return new PlayerProfile(playerName, player.getUniqueId());
                }
            } else {
                emptyNameUUIDSet.add(uuid);
            }
        }
        return null;
    }

    private PlayerFinder() {
    }

    /**
     * Get the unmodified list of cached offline player names
     * will return empty when useOfflineStash is false
     */
    public static Set<String> getCachedOfflinePlayerNames() {
        return Collections.unmodifiableSet(name2UUIDStash.keySet());
    }

    /**
     * Update the cache when needed
     *
     * @param player player instance needed to be updated
     */
    public static void updateIfNeeded(Player player) {
        UUID uuid = player.getUniqueId();
        emptyNameUUIDSet.remove(uuid);
        if (useOfflineStash) {
            name2UUIDStash.put(player.getName().toLowerCase(Locale.ROOT), uuid);
        }
    }

    public static void doLargeOfflineCachingWork(QuickShop quickShop, OfflinePlayer[] offlinePlayers) {
        quickShop.getLogger().log(Level.INFO, "Large server detected (offline player > 2000), start offline player caching...");
        useOfflineStash = true;
        int amount = 0;
        int errorAmount = 0;
        int doneAmount = 0;
        int reportingInterval = Math.max(1000, offlinePlayers.length / 20); // Reduce the amount of log spam from caching offline players
        for (OfflinePlayer offlinePlayer : offlinePlayers) { // use the existing offlinePlayers array
            try {
                String name = offlinePlayer.getName();
                if (name != null) {
                    name2UUIDStash.put(name.toLowerCase(Locale.ROOT), offlinePlayer.getUniqueId());
                } else {
                    emptyNameUUIDSet.add(offlinePlayer.getUniqueId());
                }
            } catch (Throwable ignored) {
                errorAmount++;
            }
            amount++;
            if (amount == reportingInterval) {
                doneAmount += reportingInterval;
                amount = 0;
                quickShop.getLogger().log(Level.INFO, "Caching Offline player...cached " + doneAmount + "/" + offlinePlayers.length + " players, " + errorAmount + " errors got when caching players.");
            }
        }
        quickShop.getLogger().log(Level.INFO, "Done! cached " + offlinePlayers.length + " players, " + errorAmount + " errors got when caching players.");
    }

    /**
     * Getting built-in PlayerProfile from PlayerFinder by name
     *
     * @param name            the player name, case ignored
     * @param forceWebRequest whether to use web request, may result blocking server thread
     * @return the uuid of this player name, null when [forceWebRequest] is false
     */
    public static PlayerProfile findPlayerProfileByName(String name, boolean forceWebRequest, boolean checkOfflinePlayer) {
        UUID uuid = findUUIDByName(name, forceWebRequest, checkOfflinePlayer);
        if (uuid != null) {
            return new PlayerProfile(findNameByUUID(uuid), uuid);
        } else {
            return null;
        }
    }

    private static void puttingToCache(String realPlayerName, UUID uuid) {
        if (realPlayerName == null) {
            emptyNameUUIDSet.add(uuid);
        } else {
            uuid2StringCache.put(uuid, realPlayerName);
            name2UUIDCache.put(realPlayerName.toLowerCase(Locale.ROOT), uuid);
        }
    }

    /**
     * Get player uuid from its name
     *
     * @param name                    the player name, case ignored
     * @param includingBlockingWebReq whether to use web request, may result blocking server thread
     * @param checkOfflinePlayer      whether to check offline player, beware that should be a large cost for some times
     * @return the uuid of this player name, null when [includingBlockingWebReq] is false
     */
    @Nullable
    public static UUID findUUIDByName(String name, boolean includingBlockingWebReq, boolean checkOfflinePlayer) {

        OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(name);
        if (offline != null) {
            return offline.getUniqueId();
        }

        UUID uuid = name2UUIDCache.getIfPresent(name.toLowerCase(Locale.ROOT));

        if (uuid == null && useOfflineStash) {
            uuid = name2UUIDStash.get(name.toLowerCase(Locale.ROOT));
        }

        if (uuid != null) {
            return uuid;
        } else {
            Server server = Bukkit.getServer();
            //Online
            PlayerProfile profile = findProfileByName(name, server.getOnlinePlayers(), false);
            //Offline
            if (!useOfflineStash && checkOfflinePlayer && profile == null) {
                profile = findProfileByName(name, Arrays.asList(server.getOfflinePlayers()), true);
            }
            //Blocking web request/querying user cache
            //TODO:Adding timeout
            if (profile == null && includingBlockingWebReq) {
                OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(name);
                profile = new PlayerProfile(player.getName(), player.getUniqueId());
            }

            if (profile == null) {
                return null;
            }
            puttingToCache(profile.getName(), profile.getUuid());
            return profile.getUuid();
        }
    }

    /**
     * Get player name from its uuid
     *
     * @param uuid the player uuid
     * @return the player name, null when it does not exist in local
     */
    @Nullable
    public static String findNameByUUID(UUID uuid) {
        if (emptyNameUUIDSet.contains(uuid)) {
            return null;
        }
        String result = uuid2StringCache.getIfPresent(uuid);
        if (result == null) {
            OfflinePlayer player = findOfflinePlayerByUUID(uuid);
            result = player.getName();
            puttingToCache(result, uuid);
        }
        return result;
    }

    @Data
    @AllArgsConstructor
    public static class PlayerProfile {
        @Nullable
        private String name;
        @NotNull
        private UUID uuid;

        public Trader getTrader() {
            return Trader.adapt(this);
        }

        public OfflinePlayer getOfflinePlayer() {
            return Bukkit.getOfflinePlayer(uuid);
        }
    }

    public static OfflinePlayer findOfflinePlayerByUUID(UUID uuid) {
        return Bukkit.getOfflinePlayer(uuid);
    }
}

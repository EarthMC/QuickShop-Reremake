/*
 * This file is a part of project QuickShop, the name is MojangGameLanguageImpl.java
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

package org.maxgamer.quickshop.localization.game.game;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.codec.digest.DigestUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.util.JsonUtil;
import org.maxgamer.quickshop.util.MsgUtil;
import org.maxgamer.quickshop.util.ReflectFactory;
import org.maxgamer.quickshop.util.Util;
import org.maxgamer.quickshop.util.mojangapi.MojangAPI;
import org.maxgamer.quickshop.util.mojangapi.MojangApiBmclApiMirror;
import org.maxgamer.quickshop.util.mojangapi.MojangApiMcbbsApiMirror;
import org.maxgamer.quickshop.util.mojangapi.MojangApiMirror;
import org.maxgamer.quickshop.util.mojangapi.MojangApiOfficialMirror;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * MojangGameLanguageImpl - A simple GameLanguage impl
 *
 * @author Ghost_chu and sandtechnology
 */
public class MojangGameLanguageImpl extends BukkitGameLanguageImpl implements GameLanguage {
    private final QuickShop plugin;
    private final String languageCode;
    private final AtomicReference<Optional<JsonObject>> lang = new AtomicReference<>(Optional.empty());
    private MojangApiMirror mirror;
    private static final AtomicBoolean isTaskRunning = new AtomicBoolean(false);

    @SneakyThrows
    public MojangGameLanguageImpl(@NotNull QuickShop plugin, @NotNull String languageCode) {
        super(plugin);
        this.plugin = plugin;
        this.languageCode = MsgUtil.getGameLanguageCode(languageCode);
        switch (plugin.getConfig().getInt("mojangapi-mirror", 0)) {
            case 0:
                mirror = new MojangApiOfficialMirror();
                plugin.getLogger().info("Game assets server selected: Mojang API");
                break;
            case 1:
                mirror = new MojangApiBmclApiMirror();
                plugin.getLogger().info("Game assets server selected: BMCLAPI");
                plugin.getLogger().info("===Mirror description===");
                plugin.getLogger().info("BMCLAPI is a non-profit mirror service made by @bangbang93 to speed up download in China mainland region.");
                plugin.getLogger().info("Donate BMCLAPI or get details about BMCLAPI, check here: https://bmclapidoc.bangbang93.com");
                plugin.getLogger().info("You should only use this mirror if your server in China mainland or have connection trouble with Mojang server, otherwise use Mojang Official server");
                plugin.getLogger().warning("You're selected unofficial game assets server, use at your own risk.");
                break;
            case 2:
                mirror = new MojangApiMcbbsApiMirror();
                plugin.getLogger().info("Game assets server selected: BMCLAPI");
                plugin.getLogger().info("===Mirror description===");
                plugin.getLogger().info("MCBBSAPI is a special server of OpenBMCLAPI made by @bangbang93 but managed by MCBBS, same with BMCLAPI, MCBBSAPI is target speed up download in China mainland region.");
                plugin.getLogger().info("Donate BMCLAPI or get details about BMCLAPI (includes MCBBSAPI), check here: https://bmclapidoc.bangbang93.com");
                plugin.getLogger().info("You should only use this mirror if your server in China mainland or have connection trouble with Mojang server, otherwise use Mojang Official server");
                plugin.getLogger().warning("You're selected unofficial game assets server, use at your own risk.");
                break;
        }
    }

    public void load() {
        if (isTaskRunning.get()) {
            plugin.getLogger().info("There are already have a i18n task downloading in background, please reset itemi18n.yml, potioni18n.yml and enchi18n.yml after download completed.");
            return;
        }
        FutureTask<Optional<JsonObject>> futureTask = new FutureTask<>(new GameLanguageLoadTask(plugin, languageCode, mirror));
        try {
            new Thread(futureTask, "QuickShop I18n file update task").start();
            this.lang.set(futureTask.get(20, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            plugin.getLogger().log(Level.WARNING, "Failed to wait game language thread loading", exception);
        } catch (ExecutionException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get game language file!", e);
        } catch (TimeoutException e) {
            Util.debugLog("No longer waiting file downloading because it now timed out, now downloading in background.");
            plugin.getLogger().info("No longer waiting file downloading because it now timed out, now downloading in background, please reset itemi18n.yml, potioni18n.yml and enchi18n.yml after download completed.");
        }
    }

    @Override
    public @NotNull String getName() {
        return "Mojang API";
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }

    @Override
    public @NotNull String getItem(@NotNull ItemStack itemStack) {
        return getItem(itemStack.getType());
    }

    @Override
    public @NotNull String getItem(@NotNull Material material) {
        Optional<JsonObject> lang = this.lang.get();
        if (!lang.isPresent()) {
            return super.getItem(material);
        }
        JsonElement element;
        String name = material.name().toLowerCase();
        boolean isSmithingTemplate = false;
        boolean hasDesc = false;
        if (name.endsWith("_banner_pattern") || name.startsWith("music_disc")) {
            hasDesc = true;
            element = lang.get().get("item.minecraft." + name);
        } else if (name.endsWith("_armor_trim_smithing_template")) {
            isSmithingTemplate = true;
            element = lang.get().get("trim_pattern.minecraft." + name.substring(0, name.length() - "_armor_trim_smithing_template".length()));
        } else if (name.endsWith("_upgrade_smithing_template")) {
            isSmithingTemplate = true;
            element = lang.get().get("upgrade.minecraft." + name.substring(0, name.length() - "_smithing_template".length()));
        } else {
            element = lang.get().get("item.minecraft." + name);
        }
        if (element == null) {
            return getBlock(material);
        } else {
            if (isSmithingTemplate) {
                JsonElement prefix = lang.get().get("item.minecraft.smithing_template");
                if (prefix != null) {
                    return prefix.getAsString() + ":" + element.getAsString();
                }
            }
            if (hasDesc) {
                JsonElement desc = lang.get().get("item.minecraft." + name + ".desc");
                if (desc != null) {
                    return element.getAsString() + ":" + desc.getAsString();
                }
            }

            return element.getAsString();
        }
    }

    /**
     * Get block only translations, if not found, it WON'T call getItem()
     *
     * @param material The material
     * @return The translations for material
     */
    @NotNull
    public String getBlock(@NotNull Material material) {
        Optional<JsonObject> lang = this.lang.get();
        if (!lang.isPresent()) {
            return super.getItem(material);
        }

        JsonElement jsonElement = lang.get().get("block.minecraft." + material.name().toLowerCase());
        if (jsonElement == null) {
            return super.getItem(material);
        }
        return jsonElement.getAsString();
    }

    private static final boolean isPotionSupportMinecraftKey = Util.isMethodAvailable("org.bukkit.potion.PotionEffectType", "getKey");
    @Override
    public @NotNull String getPotion(@NotNull PotionEffectType potionEffectType) {
        Optional<JsonObject> lang = this.lang.get();
        if (!lang.isPresent()) {
            return super.getPotion(potionEffectType);
        }
        JsonElement jsonElement;
        if (isPotionSupportMinecraftKey) {
            jsonElement = lang.get().get("effect.minecraft." + potionEffectType.getKey().getKey().toLowerCase());
        } else {
            jsonElement = lang.get().get("effect.minecraft." + potionEffectType.getName().toLowerCase());
        }
        if (jsonElement == null) {
            return super.getPotion(potionEffectType);
        }
        return jsonElement.getAsString();
    }

    @Override
    public @NotNull String getEnchantment(@NotNull Enchantment enchantment) {
        Optional<JsonObject> lang = this.lang.get();
        if (!lang.isPresent()) {
            return super.getEnchantment(enchantment);
        }
        JsonElement jsonElement = lang.get().get("enchantment.minecraft." + enchantment.getKey().getKey().toLowerCase());
        if (jsonElement == null) {
            return super.getEnchantment(enchantment);
        }
        return jsonElement.getAsString();

    }

    @Override
    public @NotNull String getEntity(@NotNull EntityType entityType) {
        Optional<JsonObject> lang = this.lang.get();
        if (!lang.isPresent()) {
            return super.getEntity(entityType);
        }
        JsonElement jsonElement = lang.get().get("entity.minecraft." + entityType.name().toLowerCase());
        if (jsonElement == null) {
            return super.getEntity(entityType);
        }
        return jsonElement.getAsString();
    }

    @Getter
    static class GameLanguageLoadTask implements Callable<Optional<JsonObject>> {
        private final String languageCode;
        private final QuickShop plugin;
        private final MojangApiMirror mirror;
        private boolean isLatest = false; //Does assets is latest?
        private boolean isUpdated = false; //Did we tried update assets?

        public GameLanguageLoadTask(@NotNull QuickShop plugin, @NotNull String languageCode, @NotNull MojangApiMirror mirror) {
            this.plugin = plugin;
            this.languageCode = languageCode;
            this.mirror = mirror;

        }

        @Override
        public Optional<JsonObject> call() throws Exception {
            isTaskRunning.set(true);
            try {
                File cacheFile = new File(Util.getCacheFolder(), "mojanglang.cache"); // Load cache file
                if (!cacheFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    cacheFile.createNewFile();
                }
                YamlConfiguration yamlConfiguration = new YamlConfiguration();
                yamlConfiguration.load(cacheFile);
                /* The cache data, if it all matches, we doesn't need connect to internet to download files again. */
                String cacheVersion = yamlConfiguration.getString("ver", "ERROR");
                String cacheSha1 = yamlConfiguration.getString("sha1", "ERROR");
                String cacheCode = yamlConfiguration.getString("lang");
                /* If language name is default, use computer language */
                if ("en_us".equalsIgnoreCase(languageCode)) {
                    isLatest = true;
                    return Optional.empty(); //Ignore english language
                }
                File cachedFile = new File(Util.getCacheFolder(), cacheSha1);
                if (languageCode.equals(cacheCode)) { //Language same
                    if (cachedFile.exists()) { //File exists
                        try (FileInputStream cacheFileInputSteam = new FileInputStream(cachedFile)) {
                            if (DigestUtils.sha1Hex(cacheFileInputSteam).equals(cacheSha1)) { //Check if file broken
                                Util.debugLog("MojangAPI in-game translation digest check passed.");
                                if (cacheVersion.equals(ReflectFactory.getServerVersion())) {
                                    isLatest = true;
                                    try (FileReader reader = new FileReader(cachedFile)) {
                                        return Optional.of(JsonUtil.readObject(reader)); //We doesn't need to update it
                                    } catch (Exception e) {
                                        //Keep it empty so continue to update files
                                    }
                                }
                            }
                        }
                    }
                }

                //UPDATE
                isUpdated = true;

                plugin.getLogger().info("Loading required files from Mojang API, Please allow up to 20 secs.");

                //Download new things from Mojang launcher meta site
                MojangAPI mojangAPI = new MojangAPI(mirror);
                MojangAPI.AssetsAPI assetsAPI = mojangAPI.getAssetsAPI(ReflectFactory.getServerVersion());
                if (!assetsAPI.isAvailable()) { //This version no meta can be found, bug?
                    Util.debugLog("AssetsAPI returns not available, This may caused by Mojang servers down or connection issue.");
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server, This may caused by Mojang servers down, connection issue or invalid language code.");
                    return Optional.empty();
                }
                //Download AssetsIndex
                Optional<MojangAPI.AssetsFileData> assetsFileData = assetsAPI.getGameAssetsFile();
                if (!assetsFileData.isPresent()) {
                    Util.debugLog("AssetsAPI returns nothing about required game asset file, This may caused by Mojang servers down or connection issue.");
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server, This may caused by Mojang servers down, connection issue or invalid language code.");
                    return Optional.empty();
                }
                Util.debugLog(MsgUtil.fillArgs("Assets file loaded! id:[{0}], sha1:[{1}], Content Length:[{2}]",
                        assetsFileData.get().getId(),
                        assetsFileData.get().getSha1(),
                        String.valueOf(assetsFileData.get().getContent().length())));


                String indexSha1Hex = DigestUtils.sha1Hex(assetsFileData.get().getContent());

                if (!assetsFileData.get().getSha1().equals(indexSha1Hex)) {
                    Util.debugLog(MsgUtil.fillArgs("File hashing equals failed! excepted:[{0}], file:[{1}]",
                            assetsFileData.get().getSha1(),
                            indexSha1Hex));
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server because the file seems invalid, please try again later.");
                    return Optional.empty();
                }

                try {
                    Files.write(new File(Util.getCacheFolder(), indexSha1Hex).toPath(), assetsFileData.get().getContent().getBytes(StandardCharsets.UTF_8));
                } catch (IOException ioException) {
                    plugin.getLogger().log(Level.WARNING, "Failed save file to local drive, game language system caches will stop work, we will try download again in next reboot. skipping...", ioException);
                }

                //Download language json

                JsonElement indexJson = JsonUtil.readObject(assetsFileData.get().getContent());
                if (!indexJson.isJsonObject()) {
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server because the json structure seems invalid, please try again later.");
                    return Optional.empty();
                }
                if (!indexJson.getAsJsonObject().get("objects").isJsonObject()) {
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server because the json structure about objects seems invalid, please try again later.");
                    return Optional.empty();
                }
                JsonElement langElement = indexJson.getAsJsonObject().get("objects").getAsJsonObject().get("minecraft/lang/" + languageCode + ".json");
                if (langElement == null) {
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server because the language code " + languageCode + " not supported by Minecraft.");
                    return Optional.empty();
                }

                String langHash = langElement.getAsJsonObject().get("hash").getAsString();
                Optional<String> langContent = mojangAPI.getResourcesAPI().get(langHash);
                if (!langContent.isPresent()) {
                    plugin.getLogger().warning("Failed to update game assets from MojangAPI server because network connection issue.");
                    return Optional.empty();
                }

                try {
                    Files.write(new File(Util.getCacheFolder(), langHash).toPath(), langContent.get().getBytes(StandardCharsets.UTF_8));
                } catch (IOException ioException) {
                    plugin.getLogger().log(Level.WARNING, "Failed save file to local drive, game language system caches will stop work, we will try download again in next reboot. skipping...", ioException);
                }

                //Save the caches
                yamlConfiguration.set("ver", ReflectFactory.getServerVersion());
                yamlConfiguration.set("sha1", langHash);
                yamlConfiguration.set("lang", languageCode);
                yamlConfiguration.save(cacheFile);
                isLatest = true;
                Util.debugLog("Successfully update game assets.");
                plugin.getLogger().info("Success! The game assets now up-to-date :)");
                plugin.getLogger().info("Now you can execute [/qs reset lang] command to regenerate files with localized.");
                return Optional.of(JsonUtil.readObject(langContent.get()));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Something going wrong when loading game translation assets", e);
            } finally {
                isTaskRunning.set(false);
            }
            return Optional.empty();
        }
    }
}



/*
 * This file is a part of project QuickShop, the name is MojangAPI.java
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

package org.maxgamer.quickshop.util.mojangapi;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.util.HttpUtil;
import org.maxgamer.quickshop.util.JsonUtil;
import org.maxgamer.quickshop.util.Util;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MojangAPI {
    private final MojangApiMirror mirror;

    public MojangAPI(MojangApiMirror mirror) {
        this.mirror = mirror;
    }


    @NotNull
    public AssetsAPI getAssetsAPI(@NotNull String serverVersion) {
        return new AssetsAPI(mirror, serverVersion);
    }

    @NotNull
    public GameInfoAPI getGameInfoAPI(@NotNull String gameVersionJson) {
        return new GameInfoAPI(gameVersionJson);
    }

    @NotNull
    public MetaAPI getMetaAPI(@NotNull String serverVersion) {
        return new MetaAPI(mirror, serverVersion);
    }

    public ResourcesAPI getResourcesAPI() {
        return new ResourcesAPI(mirror);
    }


    @Data
    @AllArgsConstructor
    public static class AssetsFileData {
        private String content;
        private String sha1;
        private String id;
    }

    @Data
    public static class ResourcesAPI {
        protected final Cache<String, String> requestCachePool = CacheBuilder.newBuilder()
                .expireAfterWrite(7, TimeUnit.DAYS)
                .build();
        private final MojangApiMirror apiMirror;

        public ResourcesAPI(MojangApiMirror mirror) {
            this.apiMirror = mirror;
        }

        public Optional<String> get(@NotNull String hash) {
            String url = apiMirror.getResourcesDownloadRoot() + "/" + hash.substring(0, 2) + "/" + hash;
            return Optional.ofNullable(HttpUtil.createGet(url));
        }
    }


    public static class AssetsAPI {
        private final MetaAPI metaAPI;

        AssetsAPI(@NotNull MojangApiMirror apiMirror, @NotNull String version) {
            this.metaAPI = new MetaAPI(apiMirror, version);
        }

        public boolean isAvailable() {
            return this.metaAPI.get().isPresent();
        }

        /**
         * Gets the GameAsset file content
         *
         * @return The file content
         */
        public Optional<AssetsFileData> getGameAssetsFile() {
            Optional<GameInfoAPI.DataBean> bean = getAssetsJson();
            if (!bean.isPresent()) {
                return Optional.empty();
            }
            GameInfoAPI.DataBean.AssetIndexBean assetIndexBean = bean.get().getAssetIndex();
            if (assetIndexBean == null || assetIndexBean.getUrl() == null || assetIndexBean.getId() == null) {
                return Optional.empty();
            }
            String data = HttpUtil.createGet(assetIndexBean.getUrl());
            return Optional.of(new AssetsFileData(data, assetIndexBean.getSha1(), assetIndexBean.getId()));
        }


        private Optional<GameInfoAPI.DataBean> getAssetsJson() {
            if (!isAvailable()) {
                return Optional.empty();
            }
            Optional<String> content = this.metaAPI.get();
            if (!content.isPresent()) {
                return Optional.empty();
            }
            GameInfoAPI gameInfoAPI = new GameInfoAPI(content.get());
            return Optional.of(gameInfoAPI.get());
        }


    }

    @Data
    public static class GameInfoAPI {
        protected final Cache<String, String> requestCachePool = CacheBuilder.newBuilder()
                .expireAfterWrite(7, TimeUnit.DAYS)
                .build();
        private final String json;
        private final Gson gson = JsonUtil.getGson();

        public GameInfoAPI(@NotNull String json) {
            this.json = json;
        }

        @NotNull
        public DataBean get() {
            return gson.fromJson(json, DataBean.class);
        }

        @Data
        static class DataBean {
            @Nullable
            private AssetIndexBean assetIndex;
            @Nullable
            private String assets;

            @Data
            public static class AssetIndexBean {
                /**
                 * id : 1.16
                 * sha1 : 3a5d110a6ab102c7083bae4296d2de4b8fcf92eb
                 * size : 295421
                 * totalSize : 330604420
                 * url : https://launchermeta.mojang.com/v1/packages/3a5d110a6ab102c7083bae4296d2de4b8fcf92eb/1.16.json
                 */
                @Nullable
                private String id;
                @Nullable
                private String sha1;
                @Nullable
                private String url;
            }

        }


    }

    public static class MetaAPI {
        private final String metaEndpoint;
        private final String version;

        public MetaAPI(@NotNull MojangApiMirror mirror, @NotNull String version) {
            this.version = version;
            this.metaEndpoint = mirror.getLauncherMetaRoot() + "/mc/game/version_manifest.json";
        }

        /**
         * Gets the available status and the Game Version Meta Json File content.
         *
         * @return The meta data
         */
        @SneakyThrows
        public Optional<String> get() {

            String result = HttpUtil.createGet(this.metaEndpoint);
            if (result == null) {
                Util.debugLog("Request Meta Endpoint failed.");
                return Optional.empty();
            }
            try {
                JsonElement index = JsonUtil.readObject(result);
                if (!index.isJsonObject()) {
                    return Optional.empty();
                }
                JsonElement availableVersions = index.getAsJsonObject().get("versions");
                if (!availableVersions.isJsonArray()) {
                    return Optional.empty();
                }
                for (JsonElement gameVersionData : availableVersions.getAsJsonArray()) {
                    if (gameVersionData.isJsonObject()) {
                        JsonElement gameId = gameVersionData.getAsJsonObject().get("id");
                        JsonElement gameIndexUrl = gameVersionData.getAsJsonObject().get("url");
                        if (Objects.equals(gameId.getAsString(), version)) {
                            return Optional.ofNullable(HttpUtil.createGet(gameIndexUrl.getAsString()));
                        }
                    }
                }
                return Optional.empty();
            } catch (RuntimeException exception) {
                return Optional.empty();
            }
        }
    }


}

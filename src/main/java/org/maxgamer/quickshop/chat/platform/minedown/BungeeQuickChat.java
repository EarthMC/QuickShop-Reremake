/*
 * This file is a part of project QuickShop, the name is BungeeQuickChat.java
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

package org.maxgamer.quickshop.chat.platform.minedown;

import lombok.AllArgsConstructor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.maxgamer.quickshop.QuickShop;
import org.maxgamer.quickshop.api.chat.QuickChat;
import org.maxgamer.quickshop.api.chat.QuickComponent;
import org.maxgamer.quickshop.api.shop.Shop;
import org.maxgamer.quickshop.chat.QuickComponentImpl;
import org.maxgamer.quickshop.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * BungeeChat module to send complex chats and impl QuickChat
 *
 * @author Ghost_chu
 */
@AllArgsConstructor
public class BungeeQuickChat implements QuickChat {
    private final QuickShop plugin;
    private final String[] indexStrCache = {"{0}", "{1}", "{2}", "{3}", "{4}", "{5}", "{6}", "{7}", "{8}", "{9}"};

    public static BaseComponent[] fromLegacyText(String text) {
        return TextComponent.fromLegacyText(text, net.md_5.bungee.api.ChatColor.RESET);
    }

    public static String toLegacyText(BaseComponent[] components) {
        StringBuilder builder = new StringBuilder();
        BaseComponent lastComponent = null;
        for (BaseComponent component : components) {
            net.md_5.bungee.api.ChatColor color = component.getColorRaw();
            String legacyText = component.toLegacyText();
            if (color == null && legacyText.startsWith("§f")) {
                //Remove redundant §f added by toLegacyText
                legacyText = legacyText.substring(2);
            }
            if (lastComponent != null && (
                    lastComponent.isBold() != component.isBold() ||
                            lastComponent.isItalic() != component.isItalic() ||
                            lastComponent.isObfuscated() != component.isObfuscated() ||
                            lastComponent.isStrikethrough() != component.isStrikethrough() ||
                            lastComponent.isUnderlined() != lastComponent.isUnderlined()
            )) {
                builder.append("§r");
            }
            builder.append(legacyText);
            lastComponent = component;
        }
        return builder.toString();
    }

    @Override
    public void send(@NotNull CommandSender receiver, @Nullable QuickComponent component) {
        if (component == null) {
            return;
        }
        if (component.get() instanceof BaseComponent[]) {
            receiver.spigot().sendMessage((BaseComponent[]) component.get());
            return;
        }
        if (component.get() instanceof BaseComponent) {
            receiver.spigot().sendMessage((BaseComponent) component.get());
            return;
        }
        if (component.get() instanceof Component adventureComponent) {
            receiver.sendMessage(adventureComponent);
            return;
        }
        Util.debugLog("Illegal component " + component.get().getClass().getName() + " sending to " + this.getClass().getName() + " processor, trying force sending.");

    }

    @Override
    public void send(@NotNull CommandSender receiver, @Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        receiver.spigot().sendMessage(fromLegacyText(message));
    }

    @Override
    public void sendItemHologramChat(@NotNull Player player, @NotNull String text, @NotNull ItemStack itemStack) {
        player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(text).hoverEvent(itemStack));
    }

    @Override
    public @NotNull QuickComponent getItemHologramChat(@NotNull Shop shop, @NotNull ItemStack itemStack, @NotNull Player player, @NotNull String message) {
        try {
            final net.kyori.adventure.text.TextComponent.Builder builder = LegacyComponentSerializer.legacySection().deserialize(message).hoverEvent(itemStack).toBuilder();
            if (QuickShop.getPermissionManager().hasPermission(player, "quickshop.preview")) {
                //Skip the previous component, avoid it was applied with click event
                builder.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/qs silentpreview " + shop.getRuntimeRandomUniqueId()));
                builder.append(LegacyComponentSerializer.legacySection().deserialize(plugin.text().of(player, "menu.preview").forLocale()));

            }
            return new QuickComponentImpl(builder.build());
        } catch (Exception t) {
            plugin.getLogger().log(Level.WARNING, "Failed to process chat component", t);
            return new QuickComponentImpl(new TextComponent(plugin.text().of(player, "menu.item-holochat-error").forLocale()));
        }
    }

    @Override
    public @NotNull QuickComponent getItemTextComponent(@NotNull Player player, @NotNull ItemStack itemStack, @NotNull String normalText) {
        final Component component = LegacyComponentSerializer.legacySection().deserialize(normalText + " " + plugin.text().of(player, "menu.preview").forLocale())
                .hoverEvent(itemStack);

        return new QuickComponentImpl(component);

    }

    public String getIndexStr(int index) {
        return index <= 9 ? indexStrCache[index] : "{" + index + "}";
    }

    @Override
    public void sendExecutableChat(@NotNull CommandSender receiver, @NotNull String message, Map.Entry<String, String>... textToCommandMapping) {
        List<BaseComponent> components = new ArrayList<>(Arrays.asList(fromLegacyText(message)));
        Iterator<Map.Entry<String, String>> iterator = Arrays.asList(textToCommandMapping).iterator();

        int index = 0;
        replace:
        while (iterator.hasNext()) {
            Map.Entry<String, String> replacement = iterator.next();
            for (int i = 0; i < components.size(); i++) {
                BaseComponent component = components.get(i);
                if (component instanceof TextComponent) {
                    String text = ((TextComponent) component).getText();
                    if (text.contains(getIndexStr(index))) {
                        String[] strings = text.split(getIndexStr(index).replace("{", "\\{").replace("}", "\\}"), 2);
                        TextComponent component1 = new TextComponent(strings[0]);
                        TextComponent component2 = new TextComponent(strings[1]);
                        component1.copyFormatting(component);
                        component2.copyFormatting(component);
                        BaseComponent[] replacementComponents = fromLegacyText(replacement.getKey());
                        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, replacement.getValue());
                        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, fromLegacyText(replacement.getKey()));
                        for (BaseComponent baseComponent : replacementComponents) {
                            baseComponent.setClickEvent(clickEvent);
                            baseComponent.setHoverEvent(hoverEvent);
                            component1.addExtra(baseComponent);
                        }
                        components.remove(i);
                        components.add(i, component2);
                        components.add(i, component1);
                        index++;
                        continue replace;
                    }
                }
            }
        }
        receiver.spigot().sendMessage(components.toArray(new BaseComponent[0]));
    }

    @Override
    public void sendExecutableChat(@NotNull CommandSender receiver, @NotNull String message, @NotNull String hoverText, @NotNull String command) {
        BaseComponent[] components =
                fromLegacyText(ChatColor.DARK_PURPLE + plugin.text().of(receiver, "tableformat.left_begin").forLocale() + message);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, command);
        //FIXME: Update this when drop 1.15 supports
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, fromLegacyText(hoverText));
        for (BaseComponent component : components) {
            component.setClickEvent(clickEvent);
            component.setHoverEvent(
                    hoverEvent); //FIXME: Update this when drop 1.15 supports
        }
        receiver.spigot().sendMessage(components);
    }

    @Override
    public void sendSuggestedChat(@NotNull CommandSender receiver, @NotNull String message, @NotNull String hoverText, @NotNull String command) {
        BaseComponent[] components =
                fromLegacyText(ChatColor.DARK_PURPLE + plugin.text().of(receiver, "tableformat.left_begin").forLocale() + message);
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command);
        //FIXME: Update this when drop 1.15 supports
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, fromLegacyText(hoverText));
        for (BaseComponent component : components) {
            component.setClickEvent(clickEvent);
            component.setHoverEvent(hoverEvent);
        }
        receiver.spigot().sendMessage(components);
    }

    public static class BungeeComponentBuilder {
        private static final boolean isUsingResetCommitVersion = Util
                .isMethodAvailable(BaseComponent.class, "isReset");
        private final ComponentBuilder builder;

        public BungeeComponentBuilder() {
            builder = new ComponentBuilder("");
            builder.removeComponent(0);
        }

        public BungeeComponentBuilder append(BaseComponent component) {
            if (builder.getCursor() == -1) {
                append(component, ComponentBuilder.FormatRetention.EVENTS);
            } else {
                append(component, ComponentBuilder.FormatRetention.ALL);
            }
            return this;
        }


        public BungeeComponentBuilder append(BaseComponent[] components) {
            for (BaseComponent component : components) {
                append(component);
            }
            return this;
        }

        public BungeeComponentBuilder append(String text) {
            if (builder.getCursor() == -1) {
                append(text, ComponentBuilder.FormatRetention.EVENTS);
            } else {
                append(text, ComponentBuilder.FormatRetention.ALL);
            }
            return this;
        }

        public BungeeComponentBuilder appendLegacy(String... text) {
            if (text == null || text.length == 0) {
                return this;
            }
            StringBuilder stringBuilder = new StringBuilder(text[0]);
            for (int i = 1; i < text.length; i++) {
                stringBuilder.append(text[i]);
            }
            append(fromLegacyText(stringBuilder.toString()), ComponentBuilder.FormatRetention.EVENTS);
            return this;
        }

        public BungeeComponentBuilder appendLegacyAndItem(String left, BaseComponent[] itemsComponent, String right) {
            String uuidStr = UUID.randomUUID().toString();
            BaseComponent[] components = fromLegacyText(left + uuidStr + right);
            boolean centerFound = false;
            for (BaseComponent component : components) {
                //Find center value
                if (!centerFound && component.toPlainText().contains(uuidStr)) {
                    centerFound = true;
                    String[] text = ((TextComponent) component).getText().split(uuidStr, 2);
                    TextComponent leftComponent = new TextComponent(text[0]);
                    leftComponent.copyFormatting(component);
                    TextComponent rightComponent = new TextComponent(text[1]);
                    rightComponent.copyFormatting(component);
                    for (BaseComponent baseComponent : itemsComponent) {
                        leftComponent.addExtra(baseComponent);
                    }
                    append(leftComponent, ComponentBuilder.FormatRetention.EVENTS);
                    append(rightComponent, ComponentBuilder.FormatRetention.EVENTS);
                } else {
                    append(component, ComponentBuilder.FormatRetention.EVENTS);
                }
            }
            return this;
        }


        public BungeeComponentBuilder appendLegacy(String text) {
            append(fromLegacyText(text), ComponentBuilder.FormatRetention.EVENTS);
            return this;
        }

        public BungeeComponentBuilder event(ClickEvent clickEvent) {
            builder.event(clickEvent);
            return this;
        }

        public BungeeComponentBuilder event(HoverEvent hoverEvent) {
            builder.event(hoverEvent);
            return this;
        }

        public BungeeComponentBuilder reset() {
            builder.reset();
            return this;
        }

        public BaseComponent[] create() {
            return builder.create();
        }

        public ComponentBuilder color(net.md_5.bungee.api.ChatColor color) {
            return builder.color(color);
        }

        /**
         * The dump method for fixing append always care about reset
         */
        //dump start
        private ComponentBuilder append(BaseComponent component, ComponentBuilder.FormatRetention retention) {
            if (!isUsingResetCommitVersion) {
                builder.append(component, retention);
                return builder;
            }

            List<BaseComponent> parts = builder.getParts();
            BaseComponent previous = (parts.isEmpty()) ? null : parts.get(parts.size() - 1);

            if (component.isReset() && retention == ComponentBuilder.FormatRetention.ALL) {
                retention = ComponentBuilder.FormatRetention.EVENTS;
            }

            //Hacky method to replace dummy components
            if (previous == null && builder.getCursor() == -1) {
                previous = new TextComponent("");
                builder.append(previous);
            }

            if (previous != null && (!component.isReset() || retention == ComponentBuilder.FormatRetention.EVENTS)) {
                component.copyFormatting(previous, retention, false);
                parts.add(component);
                builder.resetCursor();
            } else {
                //follow the original logic
                builder.append(component, retention);
            }
            return builder;
        }

        private ComponentBuilder append(String text, ComponentBuilder.FormatRetention retention) {
            return append(new TextComponent(text), retention);
        }

        private ComponentBuilder append(BaseComponent[] components, ComponentBuilder.FormatRetention retention) {
            for (BaseComponent component : components) {
                append(component, retention);
            }
            return builder;
        }
        //dump end
    }
}

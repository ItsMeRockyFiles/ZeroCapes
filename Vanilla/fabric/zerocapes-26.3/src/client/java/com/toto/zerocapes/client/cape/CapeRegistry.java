package com.toto.zerocapes.client.cape;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CapeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZeroCapes");
    private static final List<CapeDefinition> CAPES = new ArrayList<>();
    private static final Map<String, CapeDefinition> BY_ID = new LinkedHashMap<>();
    private static final Set<String> CATEGORIES = new LinkedHashSet<>();
    private static CapeDefinition DEFAULT_CAPE;

    public static void init() {
        CAPES.clear();
        BY_ID.clear();
        CATEGORIES.clear();

        // Always have default 'none' option
        DEFAULT_CAPE = new CapeDefinition(
            "none",
            "None (Vanilla / Default)",
            "Default",
            "Keep your regular Minecraft cape (if any).",
            null
        );
        register(DEFAULT_CAPE);

        try (InputStream is = CapeRegistry.class.getResourceAsStream("/assets/zerocapes/capes_catalog.json")) {
            if (is == null) {
                LOGGER.warn("[ZeroCapes] Could not locate /assets/zerocapes/capes_catalog.json in classpath!");
                return;
            }

            JsonElement root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement item : array) {
                    JsonObject obj = item.getAsJsonObject();
                    String id = obj.get("id").getAsString();
                    if ("none".equalsIgnoreCase(id)) {
                        continue;
                    }
                    String name = obj.get("name").getAsString();
                    String category = obj.has("category") ? obj.get("category").getAsString() : "Events";
                    String desc = obj.has("description") ? obj.get("description").getAsString() : "Official Minecraft Cape";
                    Identifier texture = null;
                    if (obj.has("texture") && !obj.get("texture").isJsonNull()) {
                        String texStr = obj.get("texture").getAsString();
                        if (texStr != null && !texStr.isBlank()) {
                            texture = Identifier.parse(texStr);
                        }
                    }

                    CapeDefinition def = new CapeDefinition(id, name, category, desc, texture);
                    register(def);
                }
            }
            LOGGER.info("[ZeroCapes] Successfully registered {} official capes!", CAPES.size());
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to load cape catalog!", e);
        }
    }

    private static void register(CapeDefinition def) {
        CAPES.add(def);
        BY_ID.put(def.id().toLowerCase(Locale.ROOT), def);
        CATEGORIES.add(def.category());
    }

    public static List<CapeDefinition> getAllCapes() {
        return Collections.unmodifiableList(CAPES);
    }

    public static CapeDefinition getById(String id) {
        if (id == null) return DEFAULT_CAPE;
        return BY_ID.getOrDefault(id.toLowerCase(Locale.ROOT), DEFAULT_CAPE);
    }

    public static CapeDefinition getDefaultCape() {
        return DEFAULT_CAPE;
    }

    public static Set<String> getCategories() {
        return Collections.unmodifiableSet(CATEGORIES);
    }
}

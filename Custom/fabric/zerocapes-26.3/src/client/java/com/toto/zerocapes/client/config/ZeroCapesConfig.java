package com.toto.zerocapes.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toto.zerocapes.client.cape.CapeDefinition;
import com.toto.zerocapes.client.cape.CapeRegistry;
import com.toto.zerocapes.client.cape.CustomCapeManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ZeroCapesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ZeroCapes");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("zerocapes.json");
    private static ZeroCapesConfig INSTANCE;

    private String selectedCapeId = "none";
    private String customCapePath = "";

    private boolean customCapeRestoreAttempted = false;

    public static ZeroCapesConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZeroCapesConfig();
            INSTANCE.load();
        }
        return INSTANCE;
    }

    public void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            if (json.has("selectedCapeId")) {
                this.selectedCapeId = json.get("selectedCapeId").getAsString();
            }
            if (json.has("customCapePath")) {
                this.customCapePath = json.get("customCapePath").getAsString();
            }
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to read configuration from {}", CONFIG_PATH, e);
        }
    }

    private void ensureCustomCapeRestored() {
        if (customCapeRestoreAttempted) return;
        customCapeRestoreAttempted = true;

        if (this.customCapePath == null || this.customCapePath.isBlank()) {
            return;
        }

        Identifier loaded = CustomCapeManager.loadFromFile(this.customCapePath);
        if (loaded == null) {
            LOGGER.warn("[ZeroCapes] Custom cape file no longer found: {}", this.customCapePath);
            this.customCapePath = "";
            if ("custom_cape".equalsIgnoreCase(this.selectedCapeId)) {
                this.selectedCapeId = "none";
            }
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                JsonObject json = new JsonObject();
                json.addProperty("selectedCapeId", this.selectedCapeId != null ? this.selectedCapeId : "none");
                json.addProperty("customCapePath", this.customCapePath != null ? this.customCapePath : "");
                GSON.toJson(json, writer);
            }
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to save configuration to {}", CONFIG_PATH, e);
        }
    }

    public String getSelectedCapeId() {
        return this.selectedCapeId != null ? this.selectedCapeId : "none";
    }

    public void setSelectedCapeId(String id) {
        this.selectedCapeId = (id != null && !id.isBlank()) ? id : "none";
        save();
    }

    public String getCustomCapePath() {
        return this.customCapePath != null ? this.customCapePath : "";
    }

    public void setCustomCapePath(String path) {
        this.customCapePath = (path != null) ? path : "";
        if (!this.customCapePath.isBlank()) {
            this.selectedCapeId = "custom_cape";
        }
        save();
    }

    public void clearCustomCape() {
        this.customCapePath = "";
        if ("custom_cape".equalsIgnoreCase(this.selectedCapeId)) {
            this.selectedCapeId = "none";
        }
        save();
    }

    @Nullable
    public Identifier getCurrentCapeTexture() {
        // Lazy restore: safe because this is called during rendering
        ensureCustomCapeRestored();

        if ("none".equalsIgnoreCase(this.selectedCapeId)) {
            return null;
        }

        if ("custom_cape".equalsIgnoreCase(this.selectedCapeId)) {
            return CustomCapeManager.isLoaded()
                    ? CustomCapeManager.CUSTOM_CAPE_TEXTURE_ID
                    : null;
        }

        CapeDefinition def = CapeRegistry.getById(this.selectedCapeId);
        return (def != null && !def.isDefault()) ? def.texture() : null;
    }

    public CapeDefinition getCurrentCapeDefinition() {
        return CapeRegistry.getById(this.selectedCapeId);
    }
}
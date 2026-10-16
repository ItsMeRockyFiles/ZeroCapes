package com.toto.zerocapes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toto.zerocapes.cape.CapeDefinition;
import com.toto.zerocapes.cape.CapeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class ZeroCapesConfig {
    private static final Logger LOGGER = LogManager.getLogger("ZeroCapes");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ZeroCapesConfig INSTANCE;

    private File configFile;
    private String selectedCapeId = "none";

    public static ZeroCapesConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ZeroCapesConfig();
            INSTANCE.init();
        }
        return INSTANCE;
    }

    private void init() {
        File configDir = new File(Minecraft.getMinecraft().mcDataDir, "config");
        this.configFile = new File(configDir, "zerocapes.json");
        load();
    }

    public void load() {
        if (configFile == null || !configFile.exists()) {
            save();
            return;
        }

        Reader reader = null;
        try {
            reader = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
            JsonObject json = new JsonParser().parse(reader).getAsJsonObject();
            if (json.has("selectedCapeId")) {
                this.selectedCapeId = json.get("selectedCapeId").getAsString();
            }
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to read configuration from " + configFile, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public void save() {
        if (configFile == null) return;
        Writer writer = null;
        try {
            if (configFile.getParentFile() != null) {
                configFile.getParentFile().mkdirs();
            }
            writer = new OutputStreamWriter(new FileOutputStream(configFile), "UTF-8");
            JsonObject json = new JsonObject();
            json.addProperty("selectedCapeId", this.selectedCapeId != null ? this.selectedCapeId : "none");
            GSON.toJson(json, writer);
        } catch (Exception e) {
            LOGGER.error("[ZeroCapes] Failed to save configuration to " + configFile, e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public String getSelectedCapeId() {
        return this.selectedCapeId != null ? this.selectedCapeId : "none";
    }

    public void setSelectedCapeId(String id) {
        this.selectedCapeId = (id != null && !id.trim().isEmpty()) ? id : "none";
        save();
    }

    public ResourceLocation getCurrentCapeTexture() {
        if ("none".equalsIgnoreCase(this.selectedCapeId)) {
            return null;
        }
        CapeDefinition def = CapeRegistry.getById(this.selectedCapeId);
        return (def != null && !def.isDefault()) ? def.getTexture() : null;
    }

    public CapeDefinition getCurrentCapeDefinition() {
        return CapeRegistry.getById(this.selectedCapeId);
    }
}

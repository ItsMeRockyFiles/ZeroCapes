package com.toto.zerocapes.cape;

import net.minecraft.util.ResourceLocation;

public class CapeDefinition {
    private final String id;
    private final String name;
    private final String category;
    private final String description;
    private final ResourceLocation texture;

    public CapeDefinition(String id, String name, String category, String description, ResourceLocation texture) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.description = description;
        this.texture = texture;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public boolean isDefault() {
        return "none".equalsIgnoreCase(this.id) || this.texture == null;
    }
}

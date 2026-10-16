package com.toto.zerocapes.client.cape;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public record CapeDefinition(
    String id,
    String name,
    String category,
    String description,
    @Nullable Identifier texture
) {
    public boolean isDefault() {
        return "none".equalsIgnoreCase(this.id) || this.texture == null;
    }
}

package com.hexvane.orbisorigins.species;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Represents a single attachment option (e.g., a specific hair style, outfit, etc.)
 */
public class AttachmentOption {
    private final String model;
    private final String texture;
    private final String displayName;

    public AttachmentOption(@Nonnull String model, @Nonnull String texture) {
        this(model, texture, null);
    }

    public AttachmentOption(@Nonnull String model, @Nonnull String texture, @Nullable String displayName) {
        this.model = model;
        this.texture = texture;
        this.displayName = displayName;
    }

    @Nonnull
    public String getModel() {
        return model;
    }

    @Nonnull
    public String getTexture() {
        return texture;
    }

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the display name for this option, falling back to a default if not set.
     */
    @Nonnull
    public String getDisplayNameOrDefault(@Nonnull String defaultName) {
        return displayName != null ? displayName : defaultName;
    }
}

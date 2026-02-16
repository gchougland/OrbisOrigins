package com.hexvane.orbisorigins.species;

import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data for a single species variant in v2 format.
 * Contains model path, parent model, textures, and per-variant attachment options.
 */
public class SpeciesVariantData {
    private final String variantName;
    private final String parentModel;
    private final String model;
    private final List<String> textures;
    private final Float eyeHeight;
    private final Float crouchOffset;
    private final Box hitBox;
    private final List<DefaultAttachmentDef> defaultAttachments;
    private final Map<String, AttachmentSlotDef> attachments;

    public SpeciesVariantData(
            @Nonnull String variantName,
            @Nonnull String parentModel,
            @Nonnull String model,
            @Nonnull List<String> textures,
            @Nullable Float eyeHeight,
            @Nullable Float crouchOffset,
            @Nullable Box hitBox,
            @Nonnull List<DefaultAttachmentDef> defaultAttachments,
            @Nonnull Map<String, AttachmentSlotDef> attachments
    ) {
        this.variantName = variantName;
        this.parentModel = parentModel;
        this.model = model;
        this.textures = new ArrayList<>(textures);
        this.eyeHeight = eyeHeight;
        this.crouchOffset = crouchOffset;
        this.hitBox = hitBox;
        this.defaultAttachments = new ArrayList<>(defaultAttachments);
        this.attachments = new HashMap<>(attachments);
    }

    @Nonnull
    public String getVariantName() {
        return variantName;
    }

    @Nonnull
    public String getParentModel() {
        return parentModel;
    }

    @Nonnull
    public String getModel() {
        return model;
    }

    @Nonnull
    public List<String> getTextures() {
        return Collections.unmodifiableList(textures);
    }

    @Nullable
    public Float getEyeHeight() {
        return eyeHeight;
    }

    @Nullable
    public Float getCrouchOffset() {
        return crouchOffset;
    }

    @Nullable
    public Box getHitBox() {
        return hitBox;
    }

    @Nonnull
    public List<DefaultAttachmentDef> getDefaultAttachments() {
        return Collections.unmodifiableList(defaultAttachments);
    }

    @Nonnull
    public Map<String, AttachmentSlotDef> getAttachments() {
        return Collections.unmodifiableMap(attachments);
    }

    /**
     * Default attachment definition (always applied, no selector).
     */
    public static class DefaultAttachmentDef {
        private final String model;
        private final String texture;

        public DefaultAttachmentDef(@Nonnull String model, @Nonnull String texture) {
            this.model = model;
            this.texture = texture;
        }

        @Nonnull
        public String getModel() {
            return model;
        }

        @Nonnull
        public String getTexture() {
            return texture;
        }
    }

    /**
     * Attachment slot definition with selectable options.
     */
    public static class AttachmentSlotDef {
        private final boolean allowsNone;
        private final List<AttachmentOption> options;

        public AttachmentSlotDef(boolean allowsNone, @Nonnull List<AttachmentOption> options) {
            this.allowsNone = allowsNone;
            this.options = new ArrayList<>(options);
        }

        public boolean isAllowsNone() {
            return allowsNone;
        }

        @Nonnull
        public List<AttachmentOption> getOptions() {
            return Collections.unmodifiableList(options);
        }
    }
}

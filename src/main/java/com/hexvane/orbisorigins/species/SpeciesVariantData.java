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
    /** Optional scale for this variant; null = use species modelScale. */
    private final Float scale;
    /** Optional sitting offset; null = use parent model. */
    private final Float sittingOffset;
    /** Optional sleeping offset; null = use parent model. */
    private final Float sleepingOffset;
    /** Optional blocks to raise player when sleeping; null = use species sleepingRaiseHeight. */
    private final Float sleepingRaiseHeight;

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
        this(variantName, parentModel, model, textures, eyeHeight, crouchOffset, hitBox, defaultAttachments, attachments, null, null, null, null);
    }

    public SpeciesVariantData(
            @Nonnull String variantName,
            @Nonnull String parentModel,
            @Nonnull String model,
            @Nonnull List<String> textures,
            @Nullable Float eyeHeight,
            @Nullable Float crouchOffset,
            @Nullable Box hitBox,
            @Nonnull List<DefaultAttachmentDef> defaultAttachments,
            @Nonnull Map<String, AttachmentSlotDef> attachments,
            @Nullable Float scale
    ) {
        this(variantName, parentModel, model, textures, eyeHeight, crouchOffset, hitBox, defaultAttachments, attachments, scale, null, null, null);
    }

    public SpeciesVariantData(
            @Nonnull String variantName,
            @Nonnull String parentModel,
            @Nonnull String model,
            @Nonnull List<String> textures,
            @Nullable Float eyeHeight,
            @Nullable Float crouchOffset,
            @Nullable Box hitBox,
            @Nonnull List<DefaultAttachmentDef> defaultAttachments,
            @Nonnull Map<String, AttachmentSlotDef> attachments,
            @Nullable Float scale,
            @Nullable Float sittingOffset,
            @Nullable Float sleepingOffset
    ) {
        this(variantName, parentModel, model, textures, eyeHeight, crouchOffset, hitBox, defaultAttachments, attachments, scale, sittingOffset, sleepingOffset, null);
    }

    public SpeciesVariantData(
            @Nonnull String variantName,
            @Nonnull String parentModel,
            @Nonnull String model,
            @Nonnull List<String> textures,
            @Nullable Float eyeHeight,
            @Nullable Float crouchOffset,
            @Nullable Box hitBox,
            @Nonnull List<DefaultAttachmentDef> defaultAttachments,
            @Nonnull Map<String, AttachmentSlotDef> attachments,
            @Nullable Float scale,
            @Nullable Float sittingOffset,
            @Nullable Float sleepingOffset,
            @Nullable Float sleepingRaiseHeight
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
        this.scale = scale;
        this.sittingOffset = sittingOffset;
        this.sleepingOffset = sleepingOffset;
        this.sleepingRaiseHeight = sleepingRaiseHeight;
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

    /**
     * Gets the optional model scale for this variant.
     * When null, the species-level modelScale is used.
     */
    @Nullable
    public Float getScale() {
        return scale;
    }

    /**
     * Gets the optional sitting offset for this variant.
     * When null, the parent model's value is used.
     */
    @Nullable
    public Float getSittingOffset() {
        return sittingOffset;
    }

    /**
     * Gets the optional sleeping offset for this variant.
     * When null, the parent model's value is used.
     */
    @Nullable
    public Float getSleepingOffset() {
        return sleepingOffset;
    }

    /**
     * Gets the optional sleeping raise height (blocks to raise position when sleeping).
     * When null, the species sleepingRaiseHeight is used.
     */
    @Nullable
    public Float getSleepingRaiseHeight() {
        return sleepingRaiseHeight;
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

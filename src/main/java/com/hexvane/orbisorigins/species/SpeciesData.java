package com.hexvane.orbisorigins.species;

import com.hypixel.hytale.math.shape.Box;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data class representing a playable species with its properties.
 * Supports version 1 (legacy) and version 2 (new variant/attachment format).
 */
public class SpeciesData {
    private final int version;
    private final String id;
    private final String displayName;
    private final String displayNameKey; // Optional language key override
    private final String modelBaseName;
    private final List<String> variants;
    private final List<SpeciesVariantData> variantsV2;
    private final String description;
    private final String descriptionKey; // Optional language key override
    private final int healthModifier;
    private final int staminaModifier;
    private final int manaModifier;
    private final boolean enabled;
    /** When true, species has no custom model (uses default player appearance like Orbian). */
    private final boolean usePlayerModel;
    private final boolean enableAttachmentDiscovery;
    private final Map<String, Map<String, AttachmentOption>> manualAttachments; // attachment type -> option name -> AttachmentOption
    private final Map<String, Float> eyeHeightModifiers; // Per-variant eye height modifiers (model name -> modifier in blocks)
    private final Map<String, Float> hitboxHeightModifiers; // Per-variant hitbox height modifiers (model name -> modifier in blocks)
    private final List<String> starterItems;
    private final Map<String, Float> damageResistances; // damage type -> resistance multiplier (0.0 = immune, 1.0 = no resistance, 0.5 = 50% reduction)

    public SpeciesData(
            @Nonnull String id,
            @Nonnull String displayName,
            @Nonnull String modelBaseName,
            @Nonnull List<String> variants,
            @Nonnull String description,
            int healthModifier,
            int staminaModifier,
            @Nonnull List<String> starterItems,
            @Nonnull Map<String, Float> damageResistances
    ) {
        this(1, id, displayName, null, modelBaseName, variants, null, description, null, healthModifier, staminaModifier, 0, true, false, false, new HashMap<>(), new HashMap<>(), new HashMap<>(), starterItems, damageResistances);
    }

    public SpeciesData(
            @Nonnull String id,
            @Nonnull String displayName,
            @Nullable String displayNameKey,
            @Nonnull String modelBaseName,
            @Nonnull List<String> variants,
            @Nonnull String description,
            @Nullable String descriptionKey,
            int healthModifier,
            int staminaModifier,
            int manaModifier,
            boolean enabled,
            boolean enableAttachmentDiscovery,
            @Nonnull Map<String, Map<String, AttachmentOption>> manualAttachments,
            @Nonnull Map<String, Float> eyeHeightModifiers,
            @Nonnull Map<String, Float> hitboxHeightModifiers,
            @Nonnull List<String> starterItems,
            @Nonnull Map<String, Float> damageResistances
    ) {
        this(1, id, displayName, displayNameKey, modelBaseName, variants, null, description, descriptionKey, healthModifier, staminaModifier, manaModifier, enabled, false, enableAttachmentDiscovery, manualAttachments, eyeHeightModifiers, hitboxHeightModifiers, starterItems, damageResistances);
    }

    public SpeciesData(
            int version,
            @Nonnull String id,
            @Nonnull String displayName,
            @Nullable String displayNameKey,
            @Nonnull String modelBaseName,
            @Nonnull List<String> variants,
            @Nullable List<SpeciesVariantData> variantsV2,
            @Nonnull String description,
            @Nullable String descriptionKey,
            int healthModifier,
            int staminaModifier,
            int manaModifier,
            boolean enabled,
            boolean usePlayerModel,
            boolean enableAttachmentDiscovery,
            @Nonnull Map<String, Map<String, AttachmentOption>> manualAttachments,
            @Nonnull Map<String, Float> eyeHeightModifiers,
            @Nonnull Map<String, Float> hitboxHeightModifiers,
            @Nonnull List<String> starterItems,
            @Nonnull Map<String, Float> damageResistances
    ) {
        this.version = version;
        this.id = id;
        this.displayName = displayName;
        this.displayNameKey = displayNameKey;
        this.modelBaseName = modelBaseName;
        this.variants = variants != null ? new ArrayList<>(variants) : new ArrayList<>();
        this.variantsV2 = variantsV2 != null ? new ArrayList<>(variantsV2) : new ArrayList<>();
        this.description = description;
        this.descriptionKey = descriptionKey;
        this.healthModifier = healthModifier;
        this.staminaModifier = staminaModifier;
        this.manaModifier = manaModifier;
        this.enabled = enabled;
        this.usePlayerModel = usePlayerModel;
        this.enableAttachmentDiscovery = enableAttachmentDiscovery;
        // Deep copy manual attachments
        this.manualAttachments = new HashMap<>();
        for (Map.Entry<String, Map<String, AttachmentOption>> entry : manualAttachments.entrySet()) {
            this.manualAttachments.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        this.eyeHeightModifiers = new HashMap<>(eyeHeightModifiers);
        this.hitboxHeightModifiers = new HashMap<>(hitboxHeightModifiers);
        this.starterItems = new ArrayList<>(starterItems);
        this.damageResistances = new HashMap<>(damageResistances);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    @Nullable
    public String getDisplayNameKey() {
        return displayNameKey;
    }

    @Nonnull
    public String getModelBaseName() {
        return modelBaseName;
    }

    @Nonnull
    public List<String> getVariants() {
        return new ArrayList<>(variants);
    }

    @Nonnull
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getDescriptionKey() {
        return descriptionKey;
    }

    public int getHealthModifier() {
        return healthModifier;
    }

    public int getStaminaModifier() {
        return staminaModifier;
    }

    public int getManaModifier() {
        return manaModifier;
    }

    /**
     * Returns whether this species is enabled and should appear in the selection list.
     * @return true if enabled, false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Returns whether this species uses the default player model (no custom model/variants).
     * When true, the player keeps their normal appearance; stats and damage resistances still apply.
     */
    public boolean usesPlayerModel() {
        return usePlayerModel;
    }

    /**
     * Returns whether attachment discovery is enabled for this species.
     * @return true if discovery is enabled, false otherwise
     */
    public boolean isAttachmentDiscoveryEnabled() {
        return enableAttachmentDiscovery;
    }

    /**
     * Gets manually defined attachments for this species.
     * @return Map of attachment type -> option name -> AttachmentOption
     */
    @Nonnull
    public Map<String, Map<String, AttachmentOption>> getManualAttachments() {
        Map<String, Map<String, AttachmentOption>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, AttachmentOption>> entry : manualAttachments.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return result;
    }

    /**
     * Gets the eye height modifier for a specific model/variant.
     * Returns 0.0 if no modifier is specified for the given model.
     * @param modelName The model name (variant name) to get the modifier for
     * @return The eye height modifier in blocks (additive)
     */
    public float getEyeHeightModifier(@Nonnull String modelName) {
        return eyeHeightModifiers.getOrDefault(modelName, 0.0f);
    }

    /**
     * Gets the hitbox height modifier for a specific model/variant.
     * Returns 0.0 if no modifier is specified for the given model.
     * @param modelName The model name (variant name) to get the modifier for
     * @return The hitbox height modifier in blocks (additive, modifies max.y)
     */
    public float getHitboxHeightModifier(@Nonnull String modelName) {
        return hitboxHeightModifiers.getOrDefault(modelName, 0.0f);
    }

    @Nonnull
    public List<String> getStarterItems() {
        return new ArrayList<>(starterItems);
    }

    @Nonnull
    public Map<String, Float> getDamageResistances() {
        return new HashMap<>(damageResistances);
    }

    /**
     * Gets the resistance multiplier for a specific damage type.
     * Returns 1.0 (no resistance) if the damage type is not in the map.
     */
    public float getDamageResistance(@Nonnull String damageType) {
        return damageResistances.getOrDefault(damageType, 1.0f);
    }

    /**
     * Gets the model name for a specific variant index.
     * For v1: returns the variant model asset ID.
     * For v2: returns the variant's parent model (for maintenance system comparison).
     * Returns the base model name if variant index is invalid.
     */
    @Nonnull
    public String getModelName(int variantIndex) {
        if (version == 2) {
            SpeciesVariantData v = getVariantData(variantIndex);
            return v != null ? v.getParentModel() : modelBaseName;
        }
        if (variants.isEmpty() || variantIndex < 0 || variantIndex >= variants.size()) {
            return modelBaseName;
        }
        return variants.get(variantIndex);
    }

    /**
     * Returns true if this species uses version 2 format.
     */
    public boolean isVersion2() {
        return version == 2;
    }

    /**
     * Gets the version (1 or 2).
     */
    public int getVersion() {
        return version;
    }

    /**
     * Gets variant data for v2 species.
     */
    @Nullable
    public SpeciesVariantData getVariantData(int variantIndex) {
        if (variantsV2.isEmpty() || variantIndex < 0 || variantIndex >= variantsV2.size()) {
            return null;
        }
        return variantsV2.get(variantIndex);
    }

    /**
     * Gets the number of variants (works for both v1 and v2).
     */
    public int getVariantCount() {
        return version == 2 ? variantsV2.size() : variants.size();
    }

    /**
     * Gets attachment options for a v2 variant slot.
     * Returns map of option key (Name or "option_N") -> AttachmentOption.
     */
    @Nonnull
    public Map<String, AttachmentOption> getAttachmentOptions(int variantIndex, @Nonnull String slot) {
        SpeciesVariantData v = getVariantData(variantIndex);
        if (v == null) return Collections.emptyMap();
        SpeciesVariantData.AttachmentSlotDef slotDef = v.getAttachments().get(slot);
        if (slotDef == null) return Collections.emptyMap();
        Map<String, AttachmentOption> result = new LinkedHashMap<>();
        int i = 0;
        for (AttachmentOption opt : slotDef.getOptions()) {
            String key = opt.getDisplayNameOrDefault(null);
            if (key == null || key.isEmpty()) key = "option_" + i;
            result.put(key, opt);
            i++;
        }
        return result;
    }

    /**
     * Gets all attachment slot names for a v2 variant.
     */
    @Nonnull
    public List<String> getAttachmentSlotNames(int variantIndex) {
        SpeciesVariantData v = getVariantData(variantIndex);
        if (v == null) return Collections.emptyList();
        return new ArrayList<>(v.getAttachments().keySet());
    }

    /**
     * Checks if an attachment slot allows "None" for v2.
     */
    public boolean attachmentAllowsNone(int variantIndex, @Nonnull String slot) {
        SpeciesVariantData v = getVariantData(variantIndex);
        if (v == null) return false;
        SpeciesVariantData.AttachmentSlotDef slotDef = v.getAttachments().get(slot);
        return slotDef != null && slotDef.isAllowsNone();
    }
}

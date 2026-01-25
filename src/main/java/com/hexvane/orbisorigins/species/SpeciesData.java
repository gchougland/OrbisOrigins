package com.hexvane.orbisorigins.species;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Data class representing a playable species with its properties.
 */
public class SpeciesData {
    private final String id;
    private final String displayName;
    private final String displayNameKey; // Optional language key override
    private final String modelBaseName;
    private final List<String> variants;
    private final String description;
    private final String descriptionKey; // Optional language key override
    private final int healthModifier;
    private final int staminaModifier;
    private final int manaModifier;
    private final boolean enabled;
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
        this(id, displayName, null, modelBaseName, variants, description, null, healthModifier, staminaModifier, 0, true, new HashMap<>(), new HashMap<>(), starterItems, damageResistances);
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
            @Nonnull Map<String, Float> eyeHeightModifiers,
            @Nonnull Map<String, Float> hitboxHeightModifiers,
            @Nonnull List<String> starterItems,
            @Nonnull Map<String, Float> damageResistances
    ) {
        this.id = id;
        this.displayName = displayName;
        this.displayNameKey = displayNameKey;
        this.modelBaseName = modelBaseName;
        this.variants = new ArrayList<>(variants);
        this.description = description;
        this.descriptionKey = descriptionKey;
        this.healthModifier = healthModifier;
        this.staminaModifier = staminaModifier;
        this.manaModifier = manaModifier;
        this.enabled = enabled;
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
     * Returns the base model name if variant index is invalid.
     */
    @Nonnull
    public String getModelName(int variantIndex) {
        if (variants.isEmpty() || variantIndex < 0 || variantIndex >= variants.size()) {
            return modelBaseName;
        }
        return variants.get(variantIndex);
    }
}

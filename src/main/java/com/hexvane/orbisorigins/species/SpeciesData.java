package com.hexvane.orbisorigins.species;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Data class representing a playable species with its properties.
 */
public class SpeciesData {
    private final String id;
    private final String displayName;
    private final String modelBaseName;
    private final List<String> variants;
    private final String description;
    private final int healthModifier;
    private final int staminaModifier;
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
        this.id = id;
        this.displayName = displayName;
        this.modelBaseName = modelBaseName;
        this.variants = new ArrayList<>(variants);
        this.description = description;
        this.healthModifier = healthModifier;
        this.staminaModifier = staminaModifier;
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

    public int getHealthModifier() {
        return healthModifier;
    }

    public int getStaminaModifier() {
        return staminaModifier;
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

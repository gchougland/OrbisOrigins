package com.hexvane.orbisorigins.species;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Configuration for a single AbilityAPI-backed ability granted by a species.
 * Parsed from the species JSON {@code abilities} array.
 */
public class SpeciesAbilityConfig {
    @Nonnull
    private final String id;
    @Nullable
    private final Float value;
    @Nullable
    private final String condition;
    @Nonnull
    private final Map<String, Object> metadata;
    @Nullable
    private final String name;
    @Nullable
    private final String description;

    public SpeciesAbilityConfig(
            @Nonnull String id,
            @Nullable Float value,
            @Nullable String condition,
            @Nonnull Map<String, Object> metadata,
            @Nullable String name,
            @Nullable String description
    ) {
        this.id = id;
        this.value = value;
        this.condition = condition;
        this.metadata = new HashMap<>(metadata);
        this.name = name;
        this.description = description;
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nullable
    public Float getValue() {
        return value;
    }

    @Nullable
    public String getCondition() {
        return condition;
    }

    @Nonnull
    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }
}


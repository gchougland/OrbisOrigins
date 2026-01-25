package com.hexvane.orbisorigins.species;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Registry for all playable species.
 * Loads species from JSON files in resources and data directory.
 */
public class SpeciesRegistry {
    private static final Logger LOGGER = Logger.getLogger(SpeciesRegistry.class.getName());
    private static final Map<String, SpeciesData> SPECIES_MAP = new HashMap<>();
    private static final List<SpeciesData> SPECIES_LIST = new ArrayList<>();

    /**
     * Initialize the species registry by loading from JSON files.
     * @param dataDirectory Plugin data directory (can be null)
     */
    public static void initialize(@Nullable Path dataDirectory) {
        SPECIES_MAP.clear();
        SPECIES_LIST.clear();

        // Load all species from JSON files
        List<SpeciesData> loadedSpecies = SpeciesLoader.loadAll(dataDirectory);
        
        for (SpeciesData species : loadedSpecies) {
            registerSpecies(species);
        }

        LOGGER.info("SpeciesRegistry initialized with " + SPECIES_LIST.size() + " species");
    }

    /**
     * Legacy initialization method (for backward compatibility).
     * Loads from resources only.
     */
    public static void initialize() {
        initialize(null);
    }


    /**
     * Register a species programmatically (for backward compatibility).
     * Note: Species loaded from JSON files are automatically registered.
     */
    public static void registerSpecies(@Nonnull SpeciesData species) {
        // Remove existing species with same ID if present
        SPECIES_LIST.removeIf(s -> s.getId().equals(species.getId()));
        SPECIES_MAP.remove(species.getId());
        
        SPECIES_MAP.put(species.getId(), species);
        SPECIES_LIST.add(species);
        LOGGER.info("Registered species: " + species.getId());
    }

    @Nullable
    public static SpeciesData getSpecies(@Nonnull String id) {
        return SPECIES_MAP.get(id);
    }

    /**
     * Gets all enabled species (species that should appear in the selection list).
     * @return List of enabled species
     */
    @Nonnull
    public static List<SpeciesData> getAllSpecies() {
        List<SpeciesData> enabledSpecies = new ArrayList<>();
        for (SpeciesData species : SPECIES_LIST) {
            if (species.isEnabled()) {
                enabledSpecies.add(species);
            }
        }
        return enabledSpecies;
    }

    /**
     * Gets all species including disabled ones (for internal use).
     * @return List of all species regardless of enabled status
     */
    @Nonnull
    public static List<SpeciesData> getAllSpeciesIncludingDisabled() {
        return new ArrayList<>(SPECIES_LIST);
    }

    @Nullable
    public static SpeciesData getDefaultSpecies() {
        return getSpecies("orbian");
    }
}

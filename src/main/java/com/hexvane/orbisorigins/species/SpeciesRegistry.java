package com.hexvane.orbisorigins.species;

import com.hexvane.orbisorigins.util.AttachmentDiscoveryUtil;
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
     * If attachment discovery is enabled, attachments will be discovered for all variants.
     */
    public static void registerSpecies(@Nonnull SpeciesData species) {
        // Remove existing species with same ID if present
        SPECIES_LIST.removeIf(s -> s.getId().equals(species.getId()));
        SPECIES_MAP.remove(species.getId());
        
        // If attachment discovery is enabled, discover attachments for all variants
        if (species.isAttachmentDiscoveryEnabled()) {
            LOGGER.info("SpeciesRegistry: Attachment discovery enabled for " + species.getId() + ", discovering attachments...");
            // Attachments are discovered on-demand when needed, not cached here
            // This keeps the registry simple and allows lazy loading
        }
        
        SPECIES_MAP.put(species.getId(), species);
        SPECIES_LIST.add(species);
        LOGGER.info("Registered species: " + species.getId());
    }

    /**
     * Gets available attachments for a species variant.
     * Combines discovered attachments (if discovery is enabled) with manual attachments.
     */
    @Nonnull
    public static Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> getAvailableAttachments(
            @Nonnull SpeciesData species,
            @Nonnull String modelName
    ) {
        Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> result = new HashMap<>();
        
        LOGGER.info("SpeciesRegistry.getAvailableAttachments: Getting attachments for species " + species.getId() + ", model " + modelName + ", discovery enabled: " + species.isAttachmentDiscoveryEnabled());
        
        // Start with manual attachments
        Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> manualAttachments = species.getManualAttachments();
        LOGGER.info("SpeciesRegistry.getAvailableAttachments: Manual attachments: " + manualAttachments.size() + " types");
        for (Map.Entry<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> entry : manualAttachments.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        
        // If discovery is enabled, discover and merge attachments
        if (species.isAttachmentDiscoveryEnabled()) {
            LOGGER.info("SpeciesRegistry.getAvailableAttachments: Discovering attachments for model: " + modelName);
            Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> discovered = 
                AttachmentDiscoveryUtil.discoverAttachments(modelName);
            if (discovered != null) {
                LOGGER.info("SpeciesRegistry.getAvailableAttachments: Discovered " + discovered.size() + " attachment types");
                // Merge discovered attachments (discovered can override manual if same type)
                for (Map.Entry<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> entry : discovered.entrySet()) {
                    Map<String, com.hexvane.orbisorigins.species.AttachmentOption> existing = result.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
                    existing.putAll(entry.getValue());
                }
            } else {
                LOGGER.info("SpeciesRegistry.getAvailableAttachments: No attachments discovered for model: " + modelName);
            }
        }
        
        LOGGER.info("SpeciesRegistry.getAvailableAttachments: Total attachment types: " + result.size());
        return result;
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

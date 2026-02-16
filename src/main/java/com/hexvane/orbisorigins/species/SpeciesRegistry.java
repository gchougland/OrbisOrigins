package com.hexvane.orbisorigins.species;

import com.hexvane.orbisorigins.util.AttachmentDiscoveryUtil;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
     * For v2: returns attachment options from variant config.
     * For v1: combines discovered attachments (if discovery is enabled) with manual attachments.
     */
    @Nonnull
    public static Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> getAvailableAttachments(
            @Nonnull SpeciesData species,
            int variantIndex,
            @Nullable String modelName
    ) {
        Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> result = new HashMap<>();
        
        // v2: get from variant config (no discovery); preserve JSON order via LinkedHashMap
        if (species.isVersion2()) {
            for (String slot : species.getAttachmentSlotNames(variantIndex)) {
                Map<String, com.hexvane.orbisorigins.species.AttachmentOption> options = species.getAttachmentOptions(variantIndex, slot);
                if (!options.isEmpty()) {
                    Map<String, com.hexvane.orbisorigins.species.AttachmentOption> optionMap = new LinkedHashMap<>(options);
                    if (species.attachmentAllowsNone(variantIndex, slot)) {
                        Map<String, com.hexvane.orbisorigins.species.AttachmentOption> withNone = new LinkedHashMap<>();
                        withNone.put("null", new com.hexvane.orbisorigins.species.AttachmentOption("", "", "None"));
                        withNone.putAll(optionMap);
                        result.put(slot, withNone);
                    } else {
                        result.put(slot, optionMap);
                    }
                }
            }
            return result;
        }
        
        // v1: manual + discovered
        LOGGER.info("SpeciesRegistry.getAvailableAttachments: Getting attachments for species " + species.getId() + ", model " + modelName + ", discovery enabled: " + species.isAttachmentDiscoveryEnabled());
        Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> manualAttachments = species.getManualAttachments();
        for (Map.Entry<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> entry : manualAttachments.entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        if (species.isAttachmentDiscoveryEnabled() && modelName != null) {
            Map<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> discovered = AttachmentDiscoveryUtil.discoverAttachments(modelName);
            if (discovered != null) {
                for (Map.Entry<String, Map<String, com.hexvane.orbisorigins.species.AttachmentOption>> entry : discovered.entrySet()) {
                    Map<String, com.hexvane.orbisorigins.species.AttachmentOption> existing = result.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
                    existing.putAll(entry.getValue());
                }
            }
        }
        return result;
    }

    @Nullable
    public static SpeciesData getSpecies(@Nonnull String id) {
        return SPECIES_MAP.get(id);
    }

    /**
     * Returns the species for the given id if it exists, otherwise the default species (orbian).
     * Use this when applying species to a player so that removed/unavailable species fall back safely.
     * @return The species, or the default species, or null if neither exists (e.g. no species loaded)
     */
    @Nullable
    public static SpeciesData getSpeciesOrDefault(@Nonnull String id) {
        SpeciesData species = SPECIES_MAP.get(id);
        return species != null ? species : getDefaultSpecies();
    }

    /**
     * Returns true if the given species id is currently registered and enabled.
     */
    public static boolean isSpeciesAvailable(@Nonnull String id) {
        SpeciesData species = SPECIES_MAP.get(id);
        return species != null && species.isEnabled();
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

    /**
     * Default species used when a player's stored species has been removed from the mod.
     * Prefers "orbian"; if orbian is not loaded, returns the first enabled species if any.
     */
    @Nullable
    public static SpeciesData getDefaultSpecies() {
        SpeciesData orbian = getSpecies("orbian");
        if (orbian != null) {
            return orbian;
        }
        List<SpeciesData> enabled = getAllSpecies();
        return enabled.isEmpty() ? null : enabled.get(0);
    }
}

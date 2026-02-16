package com.hexvane.orbisorigins.species;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loads species definitions from JSON files.
 * Supports loading from both resources (built-in) and data directory (user-added).
 */
public class SpeciesLoader {
    private static final Logger LOGGER = Logger.getLogger(SpeciesLoader.class.getName());
    private static final String RESOURCES_PATH = "/Species/";
    private static final String DATA_DIR_SUBDIR = "Species";

    /**
     * Loads all species from resources and data directory.
     * Resources are loaded first, then data directory (data directory entries override resources).
     */
    @Nonnull
    public static List<SpeciesData> loadAll(@Nullable Path dataDirectory) {
        List<SpeciesData> speciesList = new ArrayList<>();

        // Load from resources first
        loadFromResources(speciesList);

        // Load from data directory (overrides resources)
        if (dataDirectory != null) {
            loadFromDataDirectory(dataDirectory, speciesList);
        }

        LOGGER.info("Loaded " + speciesList.size() + " species total");
        return speciesList;
    }

    /**
     * Loads species from resources (built-in).
     */
    private static void loadFromResources(@Nonnull List<SpeciesData> speciesList) {
        try {
            // List of known species files
            String[] speciesFiles = {
                    "orbian.json", "kweebec.json", "trork.json", "feran.json", "undead.json",
                    "klops.json", "goblin.json", "outlander.json", "slothian.json", "tuluk.json", "saurian.json", "fen_stalker.json",
                    "golem_earth.json", "golem_frost.json", "golem_flame.json",
                    "golem_thunder.json", "golem_sand.json", "golem_firesteel.json",
                    "golem_void.json"
            };

            for (String fileName : speciesFiles) {
                String resourcePath = RESOURCES_PATH + fileName;
                InputStream stream = SpeciesLoader.class.getResourceAsStream(resourcePath);
                if (stream != null) {
                    try {
                        String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        SpeciesData species = SpeciesJsonCodec.fromJson(json);
                        speciesList.add(species);
                        LOGGER.info("Loaded species from resources: " + species.getId() + " (" + fileName + ")");
                    } catch (Exception e) {
                        LOGGER.warning("Failed to load species from resources: " + fileName + " - " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        stream.close();
                    }
                } else {
                    LOGGER.warning("Species resource not found: " + resourcePath);
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading species from resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads species from data directory (user-added).
     */
    private static void loadFromDataDirectory(@Nonnull Path dataDirectory, @Nonnull List<SpeciesData> speciesList) {
        Path speciesDir = dataDirectory.resolve(DATA_DIR_SUBDIR);
        
        if (!Files.exists(speciesDir)) {
            try {
                Files.createDirectories(speciesDir);
                LOGGER.info("Created species directory: " + speciesDir);
            } catch (IOException e) {
                LOGGER.warning("Failed to create species directory: " + e.getMessage());
                return;
            }
        }

        if (!Files.isDirectory(speciesDir)) {
            LOGGER.warning("Species path is not a directory: " + speciesDir);
            return;
        }

        try (Stream<Path> paths = Files.list(speciesDir)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String json = Files.readString(path);
                            SpeciesData species = SpeciesJsonCodec.fromJson(json);
                            
                            // Remove any existing species with the same ID (from resources)
                            speciesList.removeIf(s -> s.getId().equals(species.getId()));
                            
                            speciesList.add(species);
                            LOGGER.info("Loaded species from data directory: " + species.getId() + " (" + path.getFileName() + ")");
                        } catch (Exception e) {
                            LOGGER.warning("Failed to load species from data directory: " + path.getFileName() + " - " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            LOGGER.severe("Error loading species from data directory: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

package com.hexvane.orbisorigins.species;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.server.core.asset.AssetModule;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Loads species definitions from JSON files.
 * <ul>
 *   <li><b>Asset packs</b> — Same merge rules as other Hytale assets: every registered {@link AssetPack}
 *       contributes {@code Species/*.json} at the pack root; later packs in {@link AssetModule}'s list
 *       override the same species id.</li>
 *   <li><b>Classpath</b> — Gap-fill for built-in Orbis species when {@code Files.list} on {@code Species/}
 *       misses files (some JAR/ZIP layouts omit explicit directory entries), or when the mod JAR pack is not discoverable yet.</li>
 *   <li><b>Plugin data directory</b> — {@code Species/*.json} overrides bundled definitions.</li>
 * </ul>
 */
public class SpeciesLoader {
    private static final Logger LOGGER = Logger.getLogger(SpeciesLoader.class.getName());
    private static final String RESOURCES_PATH = "/Species/";
    /** Relative to asset pack root and plugin data directory */
    private static final String SPECIES_SUBDIR = "Species";

    /** Filenames shipped under {@code /Species/} in the Orbis Origins JAR (classpath gap-fill). */
    private static final String[] BUILTIN_SPECIES_RESOURCE_FILES = {
            "orbian.json", "kweebec.json", "trork.json", "feran.json", "undead.json",
            "klops.json", "goblin.json", "outlander.json", "slothian.json", "tuluk.json", "saurian.json", "fen_stalker.json",
            "golem_earth.json", "golem_frost.json", "golem_flame.json",
            "golem_thunder.json", "golem_sand.json", "golem_firesteel.json",
            "golem_void.json"
    };

    /**
     * Loads all species from asset packs, optional classpath fallback, and data directory.
     * Order of precedence (last wins for the same species id): asset packs (in module order, files within a pack sorted),
     * classpath gap-fill, then data directory.
     */
    @Nonnull
    public static List<SpeciesData> loadAll(@Nullable Path dataDirectory) {
        List<SpeciesData> speciesList = new ArrayList<>();

        loadFromAssetPacks(speciesList);

        loadFromResources(speciesList);

        if (dataDirectory != null) {
            loadFromDataDirectory(dataDirectory, speciesList);
        }

        LOGGER.info("Loaded " + speciesList.size() + " species total");
        return speciesList;
    }

    /**
     * Loads species JSON from one asset pack (used when a pack registers after plugin setup).
     */
    @Nonnull
    public static List<SpeciesData> loadSpeciesFromPack(@Nonnull AssetPack pack) {
        List<SpeciesData> out = new ArrayList<>();
        Path speciesDir = pack.getRoot().resolve(SPECIES_SUBDIR);
        mergeJsonSpeciesFromDirectory(speciesDir, "asset pack '" + pack.getName() + "'", out);
        return out;
    }

    private static void loadFromAssetPacks(@Nonnull List<SpeciesData> speciesList) {
        AssetModule module = AssetModule.get();
        if (module == null) {
            LOGGER.warning("AssetModule not available; species will not be loaded from asset packs (fallback may apply)");
            return;
        }
        List<AssetPack> packs = module.getAssetPacks();
        for (int i = 0; i < packs.size(); i++) {
            AssetPack pack = packs.get(i);
            Path speciesDir = pack.getRoot().resolve(SPECIES_SUBDIR);
            mergeJsonSpeciesFromDirectory(
                    speciesDir,
                    "asset pack '" + pack.getName() + "' (" + (i + 1) + "/" + packs.size() + ")",
                    speciesList
            );
        }
    }

    /**
     * Merges every {@code *.json} file in a directory into the list. Later files in {@code jsonFiles} order
     * override earlier entries with the same species id.
     */
    private static void mergeJsonSpeciesFromDirectory(
            @Nonnull Path speciesDir,
            @Nonnull String sourceDescription,
            @Nonnull List<SpeciesData> speciesList
    ) {
        List<Path> jsonFiles = collectSpeciesJsonFiles(speciesDir);
        if (jsonFiles.isEmpty()) {
            return;
        }

        for (Path path : jsonFiles) {
            try {
                String json = Files.readString(path);
                SpeciesData species = SpeciesJsonCodec.fromJson(json);
                speciesList.removeIf(s -> s.getId().equals(species.getId()));
                speciesList.add(species);
                LOGGER.info("Loaded species from " + sourceDescription + ": " + species.getId()
                        + " (" + path.getFileName() + "), abilities=" + species.getAbilities().size());
            } catch (Exception e) {
                LOGGER.warning("Failed to load species from " + sourceDescription + ", file "
                        + path.getFileName() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Lists {@code *.json} under {@code speciesDir}. Some {@link java.nio.file.FileSystem}s (e.g. mod JARs)
     * omit a real directory node for {@code Species/}, so {@link Files#list} fails; we then walk from the pack root.
     */
    @Nonnull
    private static List<Path> collectSpeciesJsonFiles(@Nonnull Path speciesDir) {
        if (Files.isDirectory(speciesDir)) {
            try (Stream<Path> stream = Files.list(speciesDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .sorted()
                        .collect(Collectors.toList());
            } catch (IOException e) {
                LOGGER.warning("Failed to list species directory " + speciesDir + ": " + e.getMessage());
            }
        }

        Path parent = speciesDir.getParent();
        if (parent == null || !Files.exists(parent)) {
            return List.of();
        }
        Path speciesNormalized = speciesDir.normalize();
        List<Path> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(parent, 8)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> p.getParent() != null && p.getParent().normalize().equals(speciesNormalized))
                    .sorted()
                    .forEach(out::add);
        } catch (IOException e) {
            LOGGER.warning("Failed to walk for species files under " + speciesDir + ": " + e.getMessage());
        }
        return out;
    }

    /**
     * Loads built-in Orbis species from the classpath for ids not already present (asset-pack scan may find nothing).
     * Does not override species already merged from asset packs.
     */
    private static void loadFromResources(@Nonnull List<SpeciesData> speciesList) {
        Set<String> presentIds = new HashSet<>();
        for (SpeciesData s : speciesList) {
            presentIds.add(s.getId());
        }
        try {
            for (String fileName : BUILTIN_SPECIES_RESOURCE_FILES) {
                String resourcePath = RESOURCES_PATH + fileName;
                InputStream stream = SpeciesLoader.class.getResourceAsStream(resourcePath);
                if (stream != null) {
                    try {
                        String json = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                        SpeciesData species = SpeciesJsonCodec.fromJson(json);
                        if (presentIds.contains(species.getId())) {
                            continue;
                        }
                        speciesList.add(species);
                        presentIds.add(species.getId());
                        LOGGER.info("Loaded species from classpath (gap-fill): " + species.getId()
                                + " (" + fileName + "), abilities=" + species.getAbilities().size());
                    } catch (Exception e) {
                        LOGGER.warning("Failed to load species from resources: " + fileName + " - " + e.getMessage());
                        e.printStackTrace();
                    } finally {
                        stream.close();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Error loading species from resources: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Loads species from the plugin data directory (user-added / overrides).
     */
    private static void loadFromDataDirectory(@Nonnull Path dataDirectory, @Nonnull List<SpeciesData> speciesList) {
        Path speciesDir = dataDirectory.resolve(SPECIES_SUBDIR);

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

        mergeJsonSpeciesFromDirectory(speciesDir, "plugin data directory", speciesList);
    }
}

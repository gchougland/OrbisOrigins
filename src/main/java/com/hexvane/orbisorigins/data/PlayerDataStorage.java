package com.hexvane.orbisorigins.data;

import com.nimbusds.jose.shaded.gson.Gson;
import com.nimbusds.jose.shaded.gson.GsonBuilder;
import com.nimbusds.jose.shaded.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages persistent file-based storage of player species selection data and first-join tracking.
 * Data is saved to JSON files in the plugin's data directory.
 */
public class PlayerDataStorage {
    private static final Logger LOGGER = Logger.getLogger(PlayerDataStorage.class.getName());
    private static final String DATA_FILE_NAME = "player_species_data.json";
    private static final String FIRST_JOIN_FILE_NAME = "first_join_tracking.json";
    
    private static Path dataDirectory;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // In-memory cache: player UUID -> world name -> species data
    private static final Map<UUID, Map<String, PlayerSpeciesData.SpeciesSelection>> SPECIES_STORAGE = new ConcurrentHashMap<>();
    
    // In-memory cache: player UUID -> world name -> has received selector
    private static final Map<UUID, Map<String, Boolean>> FIRST_JOIN_STORAGE = new ConcurrentHashMap<>();
    
    /**
     * Initialize the storage system with the plugin's data directory.
     */
    public static void initialize(@Nonnull Path pluginDataDirectory) {
        dataDirectory = pluginDataDirectory;
        try {
            // Ensure data directory exists
            Files.createDirectories(dataDirectory);
            
            // Load existing data
            loadSpeciesData();
            loadFirstJoinData();
            
            LOGGER.info("PlayerDataStorage initialized. Loaded data for " + SPECIES_STORAGE.size() + " players");
        } catch (IOException e) {
            LOGGER.severe("Failed to initialize PlayerDataStorage: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all data to disk.
     */
    public static void saveAll() {
        saveSpeciesData();
        saveFirstJoinData();
    }
    
    // ========== Species Selection Storage ==========
    
    @Nullable
    public static PlayerSpeciesData.SpeciesSelection getSpeciesSelection(@Nonnull UUID playerId, @Nonnull String worldName) {
        Map<String, PlayerSpeciesData.SpeciesSelection> worldData = SPECIES_STORAGE.get(playerId);
        if (worldData == null) {
            return null;
        }
        return worldData.get(worldName);
    }
    
    public static void setSpeciesSelection(
            @Nonnull UUID playerId,
            @Nonnull String worldName,
            @Nonnull String speciesId,
            int variantIndex
    ) {
        setSpeciesSelection(playerId, worldName, speciesId, variantIndex, new HashMap<>(), null);
    }

    public static void setSpeciesSelection(
            @Nonnull UUID playerId,
            @Nonnull String worldName,
            @Nonnull String speciesId,
            int variantIndex,
            @Nonnull Map<String, String> attachmentSelections
    ) {
        setSpeciesSelection(playerId, worldName, speciesId, variantIndex, attachmentSelections, null);
    }

    public static void setSpeciesSelection(
            @Nonnull UUID playerId,
            @Nonnull String worldName,
            @Nonnull String speciesId,
            int variantIndex,
            @Nonnull Map<String, String> attachmentSelections,
            @javax.annotation.Nullable String textureSelection
    ) {
        SPECIES_STORAGE.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(worldName, new PlayerSpeciesData.SpeciesSelection(speciesId, variantIndex, true, attachmentSelections, textureSelection));
        saveSpeciesData();
    }
    
    // ========== First Join Tracking ==========
    // Tracks whether a player has ever received the species selector (server-wide, not per-world).
    // Storage still records world name for backwards compatibility with existing save files.

    /**
     * Returns true if the player has ever received the species selector on this server (in any world).
     * Used so the selector is given only on first join to the server, not when travelling to other worlds.
     */
    public static boolean hasReceivedSelector(@Nonnull UUID playerId, @Nonnull String worldName) {
        Map<String, Boolean> worldData = FIRST_JOIN_STORAGE.get(playerId);
        if (worldData == null) {
            return false;
        }
        return worldData.values().stream().anyMatch(Boolean.TRUE::equals);
    }

    public static void setReceivedSelector(@Nonnull UUID playerId, @Nonnull String worldName) {
        FIRST_JOIN_STORAGE.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                .put(worldName, true);
        saveFirstJoinData();
    }
    
    // ========== File I/O ==========
    
    private static void loadSpeciesData() {
        if (dataDirectory == null) {
            return;
        }
        
        Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
        if (!Files.exists(dataFile)) {
            LOGGER.info("Species data file does not exist, starting fresh");
            return;
        }
        
        try {
            String json = Files.readString(dataFile);
            if (json == null || json.trim().isEmpty()) {
                return;
            }
            
            TypeToken<Map<String, Map<String, SpeciesSelectionData>>> typeToken = 
                new TypeToken<Map<String, Map<String, SpeciesSelectionData>>>() {};
            Map<String, Map<String, SpeciesSelectionData>> loaded = GSON.fromJson(json, typeToken.getType());
            
            if (loaded != null) {
                SPECIES_STORAGE.clear();
                for (Map.Entry<String, Map<String, SpeciesSelectionData>> playerEntry : loaded.entrySet()) {
                    UUID playerId = UUID.fromString(playerEntry.getKey());
                    Map<String, PlayerSpeciesData.SpeciesSelection> worldData = new ConcurrentHashMap<>();
                    for (Map.Entry<String, SpeciesSelectionData> worldEntry : playerEntry.getValue().entrySet()) {
                        SpeciesSelectionData data = worldEntry.getValue();
                        Map<String, String> attachmentSelections = data.attachmentSelections != null ? 
                            new HashMap<>(data.attachmentSelections) : new HashMap<>();
                        worldData.put(worldEntry.getKey(), 
                            new PlayerSpeciesData.SpeciesSelection(data.speciesId, data.variantIndex, data.hasChosen, attachmentSelections, data.textureSelection));
                    }
                    SPECIES_STORAGE.put(playerId, worldData);
                }
                LOGGER.info("Loaded species data for " + SPECIES_STORAGE.size() + " players");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load species data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void saveSpeciesData() {
        if (dataDirectory == null) {
            return;
        }
        
        try {
            // Convert to serializable format
            Map<String, Map<String, SpeciesSelectionData>> toSave = new HashMap<>();
            for (Map.Entry<UUID, Map<String, PlayerSpeciesData.SpeciesSelection>> playerEntry : SPECIES_STORAGE.entrySet()) {
                Map<String, SpeciesSelectionData> worldData = new HashMap<>();
                for (Map.Entry<String, PlayerSpeciesData.SpeciesSelection> worldEntry : playerEntry.getValue().entrySet()) {
                    PlayerSpeciesData.SpeciesSelection selection = worldEntry.getValue();
                    worldData.put(worldEntry.getKey(), new SpeciesSelectionData(
                        selection.getSpeciesId(), selection.getVariantIndex(), selection.hasChosen(), 
                        selection.getAttachmentSelections(), selection.getTextureSelection()));
                }
                toSave.put(playerEntry.getKey().toString(), worldData);
            }
            
            Path dataFile = dataDirectory.resolve(DATA_FILE_NAME);
            String json = GSON.toJson(toSave);
            Files.writeString(dataFile, json);
        } catch (Exception e) {
            LOGGER.severe("Failed to save species data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void loadFirstJoinData() {
        if (dataDirectory == null) {
            return;
        }
        
        Path dataFile = dataDirectory.resolve(FIRST_JOIN_FILE_NAME);
        if (!Files.exists(dataFile)) {
            LOGGER.info("First join data file does not exist, starting fresh");
            return;
        }
        
        try {
            String json = Files.readString(dataFile);
            if (json == null || json.trim().isEmpty()) {
                return;
            }
            
            TypeToken<Map<String, Map<String, Boolean>>> typeToken = 
                new TypeToken<Map<String, Map<String, Boolean>>>() {};
            Map<String, Map<String, Boolean>> loaded = GSON.fromJson(json, typeToken.getType());
            
            if (loaded != null) {
                FIRST_JOIN_STORAGE.clear();
                for (Map.Entry<String, Map<String, Boolean>> playerEntry : loaded.entrySet()) {
                    UUID playerId = UUID.fromString(playerEntry.getKey());
                    FIRST_JOIN_STORAGE.put(playerId, new ConcurrentHashMap<>(playerEntry.getValue()));
                }
                LOGGER.info("Loaded first join data for " + FIRST_JOIN_STORAGE.size() + " players");
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to load first join data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void saveFirstJoinData() {
        if (dataDirectory == null) {
            return;
        }
        
        try {
            // Convert to serializable format
            Map<String, Map<String, Boolean>> toSave = new HashMap<>();
            for (Map.Entry<UUID, Map<String, Boolean>> playerEntry : FIRST_JOIN_STORAGE.entrySet()) {
                toSave.put(playerEntry.getKey().toString(), new HashMap<>(playerEntry.getValue()));
            }
            
            Path dataFile = dataDirectory.resolve(FIRST_JOIN_FILE_NAME);
            String json = GSON.toJson(toSave);
            Files.writeString(dataFile, json);
        } catch (Exception e) {
            LOGGER.severe("Failed to save first join data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Serializable data class for species selection.
     */
    private static class SpeciesSelectionData {
        String speciesId;
        int variantIndex;
        boolean hasChosen;
        Map<String, String> attachmentSelections;
        String textureSelection;
        
        SpeciesSelectionData(String speciesId, int variantIndex, boolean hasChosen) {
            this(speciesId, variantIndex, hasChosen, new HashMap<>(), null);
        }
        
        SpeciesSelectionData(String speciesId, int variantIndex, boolean hasChosen, Map<String, String> attachmentSelections) {
            this(speciesId, variantIndex, hasChosen, attachmentSelections, null);
        }
        
        SpeciesSelectionData(String speciesId, int variantIndex, boolean hasChosen, Map<String, String> attachmentSelections, String textureSelection) {
            this.speciesId = speciesId;
            this.variantIndex = variantIndex;
            this.hasChosen = hasChosen;
            this.attachmentSelections = attachmentSelections != null ? attachmentSelections : new HashMap<>();
            this.textureSelection = textureSelection;
        }
    }
}

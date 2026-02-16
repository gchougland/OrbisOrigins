package com.hexvane.orbisorigins.data;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages persistent storage of player species selection data.
 * Uses PlayerDataStorage for file-based persistence.
 */
public class PlayerSpeciesData {

    public static class SpeciesSelection {
        private final String speciesId;
        private final int variantIndex;
        private final boolean hasChosen;
        private final Map<String, String> attachmentSelections; // attachment type -> selected option name
        private final String textureSelection; // Selected texture path for v2 species

        public SpeciesSelection(@Nonnull String speciesId, int variantIndex, boolean hasChosen) {
            this(speciesId, variantIndex, hasChosen, new HashMap<>(), null);
        }

        public SpeciesSelection(@Nonnull String speciesId, int variantIndex, boolean hasChosen, @Nonnull Map<String, String> attachmentSelections) {
            this(speciesId, variantIndex, hasChosen, attachmentSelections, null);
        }

        public SpeciesSelection(@Nonnull String speciesId, int variantIndex, boolean hasChosen, @Nonnull Map<String, String> attachmentSelections, @Nullable String textureSelection) {
            this.speciesId = speciesId;
            this.variantIndex = variantIndex;
            this.hasChosen = hasChosen;
            this.attachmentSelections = new HashMap<>(attachmentSelections);
            this.textureSelection = textureSelection;
        }

        @Nonnull
        public String getSpeciesId() {
            return speciesId;
        }

        public int getVariantIndex() {
            return variantIndex;
        }

        public boolean hasChosen() {
            return hasChosen;
        }

        @Nonnull
        public Map<String, String> getAttachmentSelections() {
            return new HashMap<>(attachmentSelections);
        }

        @Nullable
        public String getTextureSelection() {
            return textureSelection;
        }
    }

    @Nullable
    private static UUID getPlayerUuid(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            return uuidComponent.getUuid();
        }
        return null;
    }

    @Nullable
    private static UUID getPlayerUuidFromHolder(@Nonnull com.hypixel.hytale.component.Holder<EntityStore> holder) {
        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            return uuidComponent.getUuid();
        }
        return null;
    }

    @Nullable
    public static SpeciesSelection getSpeciesSelection(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        UUID playerId = getPlayerUuid(ref, store);
        if (playerId == null) {
            return null;
        }
        String worldName = world.getName();
        return PlayerDataStorage.getSpeciesSelection(playerId, worldName);
    }

    public static void setSpeciesSelection(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull String speciesId,
            int variantIndex
    ) {
        setSpeciesSelection(ref, store, world, speciesId, variantIndex, new HashMap<>(), null);
    }

    public static void setSpeciesSelection(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull String speciesId,
            int variantIndex,
            @Nonnull Map<String, String> attachmentSelections
    ) {
        setSpeciesSelection(ref, store, world, speciesId, variantIndex, attachmentSelections, null);
    }

    public static void setSpeciesSelection(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull String speciesId,
            int variantIndex,
            @Nonnull Map<String, String> attachmentSelections,
            @Nullable String textureSelection
    ) {
        UUID playerId = getPlayerUuid(ref, store);
        if (playerId == null) {
            return;
        }
        String worldName = world.getName();
        PlayerDataStorage.setSpeciesSelection(playerId, worldName, speciesId, variantIndex, attachmentSelections, textureSelection);
    }

    @Nullable
    public static String getTextureSelection(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        SpeciesSelection selection = getSpeciesSelection(ref, store, world);
        return selection != null ? selection.getTextureSelection() : null;
    }

    @Nonnull
    public static Map<String, String> getAttachmentSelections(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        SpeciesSelection selection = getSpeciesSelection(ref, store, world);
        return selection != null ? selection.getAttachmentSelections() : new HashMap<>();
    }

    public static boolean hasChosenSpecies(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        SpeciesSelection selection = getSpeciesSelection(ref, store, world);
        return selection != null && selection.hasChosen();
    }

    public static boolean hasChosenSpecies(@Nonnull com.hypixel.hytale.component.Holder<EntityStore> holder, @Nonnull World world) {
        UUID playerId = getPlayerUuidFromHolder(holder);
        if (playerId == null) {
            return false;
        }
        String worldName = world.getName();
        SpeciesSelection selection = PlayerDataStorage.getSpeciesSelection(playerId, worldName);
        return selection != null && selection.hasChosen();
    }

    @Nullable
    public static String getSelectedSpeciesId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        SpeciesSelection selection = getSpeciesSelection(ref, store, world);
        return selection != null ? selection.getSpeciesId() : null;
    }

    /**
     * Returns the species id that should be used for gameplay (model, stats, damage resistance).
     * If the player's stored species has been removed from the mod, returns the default species id ("orbian")
     * so they are treated as the default species instead of breaking.
     */
    @Nullable
    public static String getEffectiveSpeciesId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        String storedId = getSelectedSpeciesId(ref, store, world);
        if (storedId == null) {
            return null;
        }
        return SpeciesRegistry.getSpecies(storedId) != null ? storedId : getDefaultSpeciesId();
    }

    private static String getDefaultSpeciesId() {
        SpeciesData defaultSpecies = SpeciesRegistry.getDefaultSpecies();
        return defaultSpecies != null ? defaultSpecies.getId() : "orbian";
    }

    /**
     * Returns the variant index to use for the effective species (clamped to valid range).
     * When the stored species was removed and we fall back to default, returns 0.
     */
    public static int getEffectiveVariantIndex(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        String effectiveId = getEffectiveSpeciesId(ref, store, world);
        String storedId = getSelectedSpeciesId(ref, store, world);
        if (effectiveId == null) {
            return 0;
        }
        if (!effectiveId.equals(storedId)) {
            return 0;
        }
        int variantIndex = getSelectedVariantIndex(ref, store, world);
        SpeciesData species = SpeciesRegistry.getSpecies(effectiveId);
        if (species == null) {
            return 0;
        }
        int count = species.getVariantCount();
        if (count <= 0) {
            return 0;
        }
        return Math.min(Math.max(0, variantIndex), count - 1);
    }

    public static int getSelectedVariantIndex(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        SpeciesSelection selection = getSpeciesSelection(ref, store, world);
        return selection != null ? selection.getVariantIndex() : 0;
    }

    @Nullable
    public static String getSpeciesSelection(@Nonnull com.hypixel.hytale.component.Holder<EntityStore> holder, @Nonnull World world) {
        UUID playerId = getPlayerUuidFromHolder(holder);
        if (playerId == null) {
            return null;
        }
        String worldName = world.getName();
        SpeciesSelection selection = PlayerDataStorage.getSpeciesSelection(playerId, worldName);
        return selection != null ? selection.getSpeciesId() : null;
    }

    public static int getVariantIndex(@Nonnull com.hypixel.hytale.component.Holder<EntityStore> holder, @Nonnull World world) {
        UUID playerId = getPlayerUuidFromHolder(holder);
        if (playerId == null) {
            return 0;
        }
        String worldName = world.getName();
        SpeciesSelection selection = PlayerDataStorage.getSpeciesSelection(playerId, worldName);
        return selection != null ? selection.getVariantIndex() : 0;
    }
}

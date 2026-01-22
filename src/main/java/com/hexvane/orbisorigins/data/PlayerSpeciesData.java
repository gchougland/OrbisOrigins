package com.hexvane.orbisorigins.data;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
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

        public SpeciesSelection(@Nonnull String speciesId, int variantIndex, boolean hasChosen) {
            this.speciesId = speciesId;
            this.variantIndex = variantIndex;
            this.hasChosen = hasChosen;
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
        UUID playerId = getPlayerUuid(ref, store);
        if (playerId == null) {
            return;
        }
        String worldName = world.getName();
        PlayerDataStorage.setSpeciesSelection(playerId, worldName, speciesId, variantIndex);
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

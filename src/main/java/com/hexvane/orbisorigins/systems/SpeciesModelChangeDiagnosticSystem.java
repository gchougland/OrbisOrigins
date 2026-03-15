package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Diagnostic-only system that logs when {@link ModelComponent} is set on a player who has
 * a chosen species to something other than the expected species model. Used to identify
 * which code path overwrites the species model (e.g. effect restore, blocking, damage).
 * Does not re-apply the model. Enable with {@code -Dorbisorigins.diagnoseModelChanges=true}.
 */
public class SpeciesModelChangeDiagnosticSystem extends RefChangeSystem<EntityStore, ModelComponent> {

    private static final Logger LOGGER = Logger.getLogger(SpeciesModelChangeDiagnosticSystem.class.getName());
    private static final String DIAGNOSE_PROP = "orbisorigins.diagnoseModelChanges";

    /** Use only PlayerRef: ModelComponent.getComponentType() is null during plugin setup (like Player). We still observe ModelComponent via componentType(); callbacks are filtered to players with a species in checkAndLogMismatch. */
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    private static boolean isDiagnosticEnabled() {
        return Boolean.getBoolean(DIAGNOSE_PROP);
    }

    @Nonnull
    @Override
    public com.hypixel.hytale.component.ComponentType<EntityStore, ModelComponent> componentType() {
        return ModelComponent.getComponentType();
    }

    @Override
    public void onComponentAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ModelComponent newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!isDiagnosticEnabled()) {
            return;
        }
        checkAndLogMismatch(ref, null, newComponent, store);
    }

    @Override
    public void onComponentSet(
            @Nonnull Ref<EntityStore> ref,
            @Nullable ModelComponent oldComponent,
            @Nonnull ModelComponent newComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        if (!isDiagnosticEnabled()) {
            return;
        }
        checkAndLogMismatch(ref, oldComponent, newComponent, store);
    }

    private void checkAndLogMismatch(
            @Nonnull Ref<EntityStore> ref,
            @Nullable ModelComponent oldComponent,
            @Nonnull ModelComponent newComponent,
            @Nonnull Store<EntityStore> store
    ) {
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }
        if (!PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
            return;
        }
        if (PlayerSpeciesData.getSpeciesModelHidden(ref, store)) {
            return;
        }

        String speciesId = PlayerSpeciesData.getEffectiveSpeciesId(ref, store, world);
        int variantIndex = PlayerSpeciesData.getEffectiveVariantIndex(ref, store, world);
        if (speciesId == null) {
            return;
        }

        SpeciesData species = SpeciesRegistry.getSpeciesOrDefault(speciesId);
        if (species == null || species.usesPlayerModel()) {
            return;
        }

        String expectedModelName = species.getModelName(variantIndex);
        Model newModel = newComponent.getModel();
        String newModelAssetId = newModel.getModelAssetId();
        if (expectedModelName.equals(newModelAssetId)) {
            return;
        }

        String oldModelAssetId = oldComponent != null
                ? oldComponent.getModel().getModelAssetId()
                : "(none)";
        String playerUuid = "?";
        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        if (uuidComponent != null) {
            playerUuid = String.valueOf(uuidComponent.getUuid());
        }
        String worldName = world.getName() != null ? world.getName() : "?";

        LOGGER.log(Level.WARNING,
                "Species model overwrite: player=%s world=%s expected=%s old=%s new=%s",
                new Object[]{playerUuid, worldName, expectedModelName, oldModelAssetId, newModelAssetId});

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder("Stack trace:");
        for (StackTraceElement e : stack) {
            sb.append("\n  at ").append(e.toString());
        }
        LOGGER.log(Level.WARNING, sb.toString());
    }

    @Override
    public void onComponentRemoved(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ModelComponent removedComponent,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No logging on remove for this diagnostic
    }
}

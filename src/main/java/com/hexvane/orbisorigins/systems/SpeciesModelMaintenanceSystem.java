package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Ticking system that periodically checks and re-applies species models to ensure they persist.
 * Runs every tick but only applies models every 20 ticks to avoid performance issues.
 */
public class SpeciesModelMaintenanceSystem extends EntityTickingSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger(SpeciesModelMaintenanceSystem.class.getName());
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();
    private static final int CHECK_INTERVAL = 20; // Check every 20 ticks (1 second at 20 TPS)
    
    private int tickCounter = 0;

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Only check every N ticks to avoid performance issues
        tickCounter++;
        if (tickCounter < CHECK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Get the entity ref
        var ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        // Check if player has chosen a species
        if (!PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
            return;
        }

        // Get player's selected species
        String speciesId = PlayerSpeciesData.getSelectedSpeciesId(ref, store, world);
        int variantIndex = PlayerSpeciesData.getSelectedVariantIndex(ref, store, world);

        if (speciesId == null) {
            return;
        }

        SpeciesData species = SpeciesRegistry.getSpecies(speciesId);
        if (species == null) {
            return;
        }

        // Skip orbian (no model to maintain)
        if (species.getId().equals("orbian")) {
            return;
        }

        // Get expected model name
        String expectedModelName = species.getModelName(variantIndex);
        
        // Check if model component exists and matches expected model
        ModelComponent modelComponent = store.getComponent(ref, ModelComponent.getComponentType());
        boolean needsReapply = false;
        
        if (modelComponent == null) {
            // Model component is missing
            needsReapply = true;
        } else {
            // Check if the current model matches the expected model
            Model currentModel = modelComponent.getModel();
            String currentModelAssetId = currentModel.getModelAssetId();
            if (!expectedModelName.equals(currentModelAssetId)) {
                // Model doesn't match - it was reset
                needsReapply = true;
            }
        }
        
        if (needsReapply) {
            // Reapply the model
            final String finalSpeciesId = speciesId;
            final int finalVariantIndex = variantIndex;
            world.execute(() -> {
                if (ref.isValid()) {
                    SpeciesData speciesToApply = SpeciesRegistry.getSpecies(finalSpeciesId);
                    if (speciesToApply != null && !speciesToApply.getId().equals("orbian")) {
                        String modelName = speciesToApply.getModelName(finalVariantIndex);
                        float eyeHeightModifier = speciesToApply.getEyeHeightModifier(modelName);
                        float hitboxHeightModifier = speciesToApply.getHitboxHeightModifier(modelName);
                        ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier);
                        LOGGER.info("SpeciesModelMaintenanceSystem: Re-applied model (was wrong/missing): " + modelName);
                    }
                }
            });
        }
    }
}

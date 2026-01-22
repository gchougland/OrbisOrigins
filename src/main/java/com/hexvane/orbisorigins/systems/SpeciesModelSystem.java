package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * System that re-applies species model and stats when a player spawns.
 * Runs after PlayerSpawnedSystem to ensure all spawn initialization is complete.
 */
public class SpeciesModelSystem extends RefSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger(SpeciesModelSystem.class.getName());
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Note: We can't depend on PlayerSpawnedSystem here because it may not be registered yet during plugin setup.
        // However, since we're using RefSystem.onEntityAdded with AddReason.SPAWN, it will be called after
        // the entity is fully added to the store. PlayerSpawnedSystem also uses onEntityAdded, so the order
        // should be fine - both systems will run when the entity is added, and since PlayerSpawnedSystem
        // is registered by EntityModule (before plugins), it should run first.
        return Set.of();
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void onEntityAdded(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // Only apply on spawn
        if (reason != AddReason.SPAWN) {
            return;
        }

        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Check if player has already chosen a species
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
            LOGGER.warning("SpeciesModelSystem: Species not found: " + speciesId);
            return;
        }

        LOGGER.info("SpeciesModelSystem: Re-applying species " + speciesId + " variant " + variantIndex + " for spawned player");

        // Apply stats immediately
        SpeciesStatUtil.applySpeciesStats(ref, store, species);

        // Defer model application - the maintenance system will handle ensuring it's correct
        // We still apply it here with a delay, but the maintenance system is the real safety net
        final String finalSpeciesId = speciesId;
        final int finalVariantIndex = variantIndex;
        if (world != null) {
            // Apply after 5 ticks - maintenance system will catch it if something resets it
            scheduleDelayedModelApply(world, ref, store, finalSpeciesId, finalVariantIndex, 5);
        }
    }
    
    private void scheduleDelayedModelApply(World world, Ref<EntityStore> ref, Store<EntityStore> store, 
                                          String speciesId, int variantIndex, int ticksRemaining) {
        if (ticksRemaining <= 0) {
            if (ref.isValid()) {
                applyModel(ref, store, speciesId, variantIndex);
            }
        } else {
            world.execute(() -> {
                scheduleDelayedModelApply(world, ref, store, speciesId, variantIndex, ticksRemaining - 1);
            });
        }
    }
    
    private void applyModel(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String speciesId, int variantIndex) {
        SpeciesData species = SpeciesRegistry.getSpecies(speciesId);
        if (species == null) {
            return;
        }
        
        // Apply model (if not orbian)
        if (!species.getId().equals("orbian")) {
            String modelName = species.getModelName(variantIndex);
            float eyeHeightModifier = species.getEyeHeightModifier(modelName);
            float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
            ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier);
            LOGGER.info("SpeciesModelSystem: Re-applied model: " + modelName + " (eyeHeightModifier: " + eyeHeightModifier + ", hitboxHeightModifier: " + hitboxHeightModifier + ")");
        } else {
            ModelUtil.resetToPlayerSkin(ref, store);
            LOGGER.info("SpeciesModelSystem: Reset to player skin");
        }
    }

    @Override
    public void onEntityRemove(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        // No cleanup needed
    }
}

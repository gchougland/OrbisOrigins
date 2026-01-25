package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * System that applies species-specific damage resistances to players.
 */
public class SpeciesDamageResistanceSystem extends DamageEventSystem {
    // Use PlayerRef.getComponentType() which is available during plugin setup
    // Player.getComponentType() is null during setup, so we query for PlayerRef instead
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        // Create dependencies lazily since DamageModule groups may not be available during plugin setup
        // Check if groups are available before creating dependencies
        var damageModule = DamageModule.get();
        var gatherGroup = damageModule.getGatherDamageGroup();
        var filterGroup = damageModule.getFilterDamageGroup();
        
        if (gatherGroup == null || filterGroup == null) {
            // Groups not available yet, return empty set
            return Set.of();
        }
        
        return Set.of(
                new SystemGroupDependency<>(Order.AFTER, gatherGroup),
                new SystemGroupDependency<>(Order.AFTER, filterGroup)
        );
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // Query for PlayerRef (which is always present on players) instead of Player component
        // Player.getComponentType() is null during plugin setup
        return QUERY;
    }

    @Override
    public void handle(
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer,
            @Nonnull Damage damage
    ) {
        // Get the entity reference from the archetype chunk
        Ref<EntityStore> targetRef = archetypeChunk.getReferenceTo(index);
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        // Get player component
        Player playerComponent = archetypeChunk.getComponent(index, Player.getComponentType());
        if (playerComponent == null) {
            return;
        }

        // Get world
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        // Get player's selected species
        String speciesId = PlayerSpeciesData.getSelectedSpeciesId(targetRef, store, world);
        if (speciesId == null) {
            return;
        }

        SpeciesData species = SpeciesRegistry.getSpecies(speciesId);
        if (species == null) {
            return;
        }

        // Get damage cause
        int damageCauseIndex = damage.getDamageCauseIndex();
        DamageCause damageCause = DamageCause.getAssetMap().getAsset(damageCauseIndex);
        if (damageCause == null) {
            return;
        }

        String damageType = damageCause.getId();

        // Apply resistance multiplier
        float resistance = species.getDamageResistance(damageType);
        if (resistance != 1.0f) {
            float currentAmount = damage.getAmount();
            float newAmount = currentAmount * resistance;
            damage.setAmount(newAmount);
        }
    }
}

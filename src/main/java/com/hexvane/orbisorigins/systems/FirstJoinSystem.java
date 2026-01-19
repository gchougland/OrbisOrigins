package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * System that gives the species selector item to players on their first join.
 */
public class FirstJoinSystem extends HolderSystem<EntityStore> {
    private static final Logger LOGGER = Logger.getLogger(FirstJoinSystem.class.getName());
    private static final String SPECIES_SELECTOR_ITEM_ID = "OrbisOrigins_Species_Selector";
    
    // Cache the query to avoid null issues during plugin setup
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        // HolderSystem can use ComponentType directly - use PlayerRef like the examples
        return QUERY;
    }

    @Override
    public void onEntityAdd(
            @Nonnull Holder<EntityStore> holder,
            @Nonnull AddReason reason,
            @Nonnull Store<EntityStore> store
    ) {
        LOGGER.info("FirstJoinSystem: onEntityAdd called with reason: " + reason);
        
        // Handle both SPAWN and LOAD - both indicate a player joining/loading into the world
        // We check if they've chosen a species anyway, so it's safe to handle both
        if (reason != AddReason.SPAWN && reason != AddReason.LOAD) {
            LOGGER.fine("FirstJoinSystem: Skipping reason: " + reason);
            return;
        }

        Player playerComponent = holder.getComponent(Player.getComponentType());
        if (playerComponent == null) {
            LOGGER.warning("FirstJoinSystem: Player component is null");
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            LOGGER.warning("FirstJoinSystem: World is null");
            return;
        }

        UUIDComponent uuidComponent = holder.getComponent(UUIDComponent.getComponentType());
        UUID playerUuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        LOGGER.info("FirstJoinSystem: Player spawned - UUID: " + playerUuid + ", World: " + world.getName());

        // Check if player has already chosen a species
        boolean hasChosen = PlayerSpeciesData.hasChosenSpecies(holder, world);
        LOGGER.info("FirstJoinSystem: Player has chosen species: " + hasChosen);
        
        if (hasChosen) {
            LOGGER.info("FirstJoinSystem: Player already chose species, skipping item give");
            // Model and stats will be re-applied by SpeciesModelSystem which runs after PlayerSpawnedSystem
            return;
        }

        LOGGER.info("FirstJoinSystem: Player has NOT chosen species, will give item");

        // Defer item giving to next tick to ensure inventory is ready
        // Get PlayerRef component from holder for deferred execution
        PlayerRef playerRefComponent = holder.getComponent(PlayerRef.getComponentType());
        if (playerRefComponent != null) {
            LOGGER.info("FirstJoinSystem: Got PlayerRef component, scheduling deferred item give");
            world.execute(() -> {
                LOGGER.info("FirstJoinSystem: Deferred execution started");
                Ref<EntityStore> ref = playerRefComponent.getReference();
                if (ref != null && ref.isValid()) {
                    Store<EntityStore> playerStore = ref.getStore();
                    Player player = playerStore.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        // Give the species selector item
                        ItemStack selectorItem = new ItemStack(SPECIES_SELECTOR_ITEM_ID, 1);
                        LOGGER.info("FirstJoinSystem: Creating item stack: " + SPECIES_SELECTOR_ITEM_ID);
                        var transaction = player.getInventory().getCombinedHotbarFirst().addItemStack(selectorItem);
                        LOGGER.info("FirstJoinSystem: Attempted to give species selector item. Transaction: " + transaction);
                    } else {
                        LOGGER.warning("FirstJoinSystem: Player component is null in deferred execution");
                    }
                } else {
                    LOGGER.warning("FirstJoinSystem: PlayerRef.getReference() returned null or invalid in deferred execution");
                }
            });
        } else {
            LOGGER.warning("FirstJoinSystem: PlayerRef component is null in holder");
        }
    }

    @Override
    public void onEntityRemoved(
            @Nonnull Holder<EntityStore> holder,
            @Nonnull RemoveReason reason,
            @Nonnull Store<EntityStore> store
    ) {
        // No cleanup needed
    }
}

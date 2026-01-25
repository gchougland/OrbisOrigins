package com.hexvane.orbisorigins.util;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.species.SpeciesData;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Utility class for applying species-specific stat modifiers to players.
 */
public class SpeciesStatUtil {
    private static final Logger LOGGER = Logger.getLogger(SpeciesStatUtil.class.getName());
    private static final String HEALTH_MODIFIER_KEY = "ORBIS_ORIGINS_HEALTH";
    private static final String STAMINA_MODIFIER_KEY = "ORBIS_ORIGINS_STAMINA";
    private static final String MANA_MODIFIER_KEY = "ORBIS_ORIGINS_MANA";
    
    private static final ComponentType<EntityStore, EntityStatMap> ENTITY_STAT_MAP_TYPE = EntityStatMap.getComponentType();

    /**
     * Applies species stat modifiers (health, stamina, and mana) to a player.
     */
    public static void applySpeciesStats(
            @Nonnull Ref<EntityStore> playerRef,
            @Nonnull Store<EntityStore> store,
            @Nonnull SpeciesData species
    ) {
        EntityStatMap entityStatMapComponent = store.getComponent(playerRef, ENTITY_STAT_MAP_TYPE);
        if (entityStatMapComponent == null) {
            LOGGER.warning("applySpeciesStats: EntityStatMap component is null for player");
            return;
        }

        // Get stat indices
        int healthStatIndex = EntityStatType.getAssetMap().getIndex("Health");
        int staminaStatIndex = EntityStatType.getAssetMap().getIndex("Stamina");
        int manaStatIndex = EntityStatType.getAssetMap().getIndex("Mana");
        
        LOGGER.info("applySpeciesStats: Health index=" + healthStatIndex + ", Stamina index=" + staminaStatIndex + ", Mana index=" + manaStatIndex);
        LOGGER.info("applySpeciesStats: Applying modifiers - Health: " + species.getHealthModifier() + ", Stamina: " + species.getStaminaModifier() + ", Mana: " + species.getManaModifier());

        // Always remove previous modifiers first to ensure clean state
        // Use Predictable.SELF to ensure changes sync to the client
        entityStatMapComponent.removeModifier(EntityStatMap.Predictable.SELF, healthStatIndex, HEALTH_MODIFIER_KEY);
        entityStatMapComponent.removeModifier(EntityStatMap.Predictable.SELF, staminaStatIndex, STAMINA_MODIFIER_KEY);
        entityStatMapComponent.removeModifier(EntityStatMap.Predictable.SELF, manaStatIndex, MANA_MODIFIER_KEY);

        // Apply health modifier if non-zero
        if (species.getHealthModifier() != 0) {
            StaticModifier healthModifier = new StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE,
                    species.getHealthModifier()
            );
            entityStatMapComponent.putModifier(EntityStatMap.Predictable.SELF, healthStatIndex, HEALTH_MODIFIER_KEY, healthModifier);
            entityStatMapComponent.maximizeStatValue(EntityStatMap.Predictable.SELF, healthStatIndex);
            LOGGER.info("applySpeciesStats: Applied health modifier: " + species.getHealthModifier());
        }

        // Apply stamina modifier if non-zero
        if (species.getStaminaModifier() != 0) {
            StaticModifier staminaModifier = new StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE,
                    species.getStaminaModifier()
            );
            entityStatMapComponent.putModifier(EntityStatMap.Predictable.SELF, staminaStatIndex, STAMINA_MODIFIER_KEY, staminaModifier);
            entityStatMapComponent.maximizeStatValue(EntityStatMap.Predictable.SELF, staminaStatIndex);
            LOGGER.info("applySpeciesStats: Applied stamina modifier: " + species.getStaminaModifier());
        }

        // Apply mana modifier if non-zero
        if (species.getManaModifier() != 0) {
            StaticModifier manaModifier = new StaticModifier(
                    Modifier.ModifierTarget.MAX,
                    StaticModifier.CalculationType.ADDITIVE,
                    species.getManaModifier()
            );
            entityStatMapComponent.putModifier(EntityStatMap.Predictable.SELF, manaStatIndex, MANA_MODIFIER_KEY, manaModifier);
            entityStatMapComponent.maximizeStatValue(EntityStatMap.Predictable.SELF, manaStatIndex);
            LOGGER.info("applySpeciesStats: Applied mana modifier: " + species.getManaModifier());
        }
        
        // Component changes are automatically tracked by the ECS system - no need to putComponent
    }
}

package com.hexvane.orbisorigins.ability;

import com.hexvane.abilityapi.ability.AbilityConditionSpec;
import com.hexvane.abilityapi.api.AbilityService;
import com.hexvane.orbisorigins.species.SpeciesAbilityConfig;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Thin bridge from OrbisOrigins to AbilityAPI.
 * Applies and clears abilities for a player based on species configuration.
 */
public final class AbilityApiBridge {
    private static final Logger LOGGER = Logger.getLogger(AbilityApiBridge.class.getName());

    private AbilityApiBridge() {
    }

    public static void clearSpeciesAbilities(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nullable SpeciesData currentSpecies
    ) {
        UUID uuid = playerRef.getUuid();
        if (currentSpecies != null) {
            for (SpeciesAbilityConfig ability : currentSpecies.getAbilities()) {
                if (ability == null) {
                    continue;
                }
                String id = ability.getId();
                if (id == null || id.isEmpty()) {
                    continue;
                }
                AbilityService.removeAbility(uuid, id);
            }
        }
        AbilityService.applyForPlayer(ref, store, world);
    }

    public static void applySpeciesAbilities(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull SpeciesData species
    ) {
        UUID uuid = playerRef.getUuid();
        for (SpeciesAbilityConfig ability : species.getAbilities()) {
            if (ability == null) {
                continue;
            }
            String id = ability.getId();
            if (id == null || id.isEmpty()) {
                continue;
            }
            Object value = resolveAbilityValue(ability);
            if (value == null) {
                continue;
            }
            AbilityService.setAbility(uuid, id, value);
            List<AbilityConditionSpec> conditions = buildConditions(ability);
            if (!conditions.isEmpty()) {
                AbilityService.setConditions(uuid, id, conditions);
            }
        }
        AbilityService.applyForPlayer(ref, store, world);
    }

    @Nullable
    private static Object resolveAbilityValue(@Nonnull SpeciesAbilityConfig ability) {
        Float v = ability.getValue();
        if (v != null) {
            return v.doubleValue();
        }
        return Boolean.TRUE;
    }

    @Nonnull
    private static List<AbilityConditionSpec> buildConditions(@Nonnull SpeciesAbilityConfig ability) {
        List<AbilityConditionSpec> out = new ArrayList<>();
        String condition = ability.getCondition();
        if (condition == null || condition.isEmpty()) {
            return out;
        }
        Map<String, Object> meta = ability.getMetadata();

        // Conditions should use the same IDs as AbilityAPI's AbilityConditionSpec.TYPE_* constants.
        switch (condition) {
            case AbilityConditionSpec.TYPE_IN_ZONE -> {
                Object zonesObj = meta.get("zones");
                List<Integer> zoneIds = new ArrayList<>();
                if (zonesObj instanceof List<?> list) {
                    for (Object o : list) {
                        if (o instanceof Number n) {
                            zoneIds.add(n.intValue());
                        }
                    }
                }
                if (!zoneIds.isEmpty()) {
                    if (zoneIds.size() == 1) {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_ZONE, zoneIds.get(0)));
                    } else {
                        out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_ZONE, zoneIds.get(0), zoneIds));
                    }
                }
            }
            case AbilityConditionSpec.TYPE_IN_SUNLIGHT -> out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_IN_SUNLIGHT, 0));
            case AbilityConditionSpec.TYPE_HEALTH_BELOW -> {
                int percent = 30;
                Object thresholdObj = meta.get("healthThreshold");
                if (thresholdObj instanceof Number n) {
                    percent = Math.round(n.floatValue() * 100f);
                }
                out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_HEALTH_BELOW, percent));
            }
            case AbilityConditionSpec.TYPE_TARGET_HEALTH_BELOW -> {
                int percent = 50;
                Object thresholdObj = meta.get("enemyHealthThreshold");
                if (thresholdObj instanceof Number n) {
                    percent = Math.round(n.floatValue() * 100f);
                }
                out.add(new AbilityConditionSpec(AbilityConditionSpec.TYPE_TARGET_HEALTH_BELOW, percent));
            }
            default -> LOGGER.warning("AbilityApiBridge: Unknown condition type '" + condition + "' for ability " + ability.getId());
        }
        return out;
    }
}


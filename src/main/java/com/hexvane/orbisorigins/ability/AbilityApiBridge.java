package com.hexvane.orbisorigins.ability;

import com.hexvane.orbisorigins.species.SpeciesAbilityConfig;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Calls AbilityAPI through the loaded {@code hexvane:AbilityAPI} plugin classloader.
 * Uses reflection so OrbisOrigins does not bundle or classpath AbilityAPI at runtime
 * (a duplicate copy would write to a separate {@code PlayerAbilityStorage}).
 */
public final class AbilityApiBridge {
    private static final Logger LOGGER = Logger.getLogger(AbilityApiBridge.class.getName());
    private static final PluginIdentifier ABILITY_API_ID = PluginIdentifier.fromString("hexvane:AbilityAPI");
    private static final String SERVICE_CLASS = "com.hexvane.abilityapi.api.AbilityService";
    private static final String CONDITION_CLASS = "com.hexvane.abilityapi.ability.AbilityConditionSpec";

    private static final String TYPE_IN_ZONE = "in_zone";
    private static final String TYPE_IN_SUNLIGHT = "in_sunlight";
    private static final String TYPE_HEALTH_BELOW = "health_below";
    private static final String TYPE_TARGET_HEALTH_BELOW = "target_health_below";

    private AbilityApiBridge() {
    }

    public static boolean isAvailable() {
        PluginManager manager = PluginManager.get();
        return manager != null && manager.getPlugin(ABILITY_API_ID) != null;
    }

    public static void clearSpeciesAbilities(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nullable SpeciesData currentSpecies
    ) {
        if (!isAvailable()) {
            return;
        }
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
                invokeRemoveAbility(uuid, id);
            }
        }
        invokeApplyForPlayer(ref, store, world);
    }

    /**
     * Grants every ability defined on the species and reapplies movement/stat effects.
     * Does not remove abilities that are not listed on the species.
     */
    public static void applySpeciesAbilities(
            @Nonnull PlayerRef playerRef,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull World world,
            @Nonnull SpeciesData species
    ) {
        if (!isAvailable()) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        int applied = 0;
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
            if (!invokeSetAbility(uuid, id, value)) {
                continue;
            }
            List<Object> conditions = buildConditions(ability);
            if (!conditions.isEmpty()) {
                invokeSetConditions(uuid, id, conditions);
            }
            applied++;
        }
        invokeApplyForPlayer(ref, store, world);
        LOGGER.log(Level.INFO, "Applied {0} species ability(ies) for {1} (species {2})",
                new Object[]{applied, uuid, species.getId()});
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
    private static List<Object> buildConditions(@Nonnull SpeciesAbilityConfig ability) {
        List<Object> out = new ArrayList<>();
        String condition = ability.getCondition();
        if (condition == null || condition.isEmpty()) {
            return out;
        }
        Map<String, Object> meta = ability.getMetadata();
        ClassLoader cl = abilityClassLoader();
        if (cl == null) {
            return out;
        }

        try {
            switch (condition) {
                case TYPE_IN_ZONE -> {
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
                            out.add(newCondition(cl, TYPE_IN_ZONE, zoneIds.get(0)));
                        } else {
                            out.add(newConditionWithZones(cl, TYPE_IN_ZONE, zoneIds.get(0), zoneIds));
                        }
                    }
                }
                case TYPE_IN_SUNLIGHT -> out.add(newCondition(cl, TYPE_IN_SUNLIGHT, 0));
                case TYPE_HEALTH_BELOW -> {
                    int percent = 30;
                    Object thresholdObj = meta.get("healthThreshold");
                    if (thresholdObj instanceof Number n) {
                        percent = Math.round(n.floatValue() * 100f);
                    }
                    out.add(newCondition(cl, TYPE_HEALTH_BELOW, percent));
                }
                case TYPE_TARGET_HEALTH_BELOW -> {
                    int percent = 50;
                    Object thresholdObj = meta.get("enemyHealthThreshold");
                    if (thresholdObj instanceof Number n) {
                        percent = Math.round(n.floatValue() * 100f);
                    }
                    out.add(newCondition(cl, TYPE_TARGET_HEALTH_BELOW, percent));
                }
                default -> LOGGER.warning("Unknown condition type '" + condition + "' for ability " + ability.getId());
            }
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "Failed to build conditions for ability " + ability.getId(), e);
        }
        return out;
    }

    @Nullable
    private static ClassLoader abilityClassLoader() {
        PluginManager manager = PluginManager.get();
        if (manager == null) {
            return null;
        }
        PluginBase plugin = manager.getPlugin(ABILITY_API_ID);
        if (plugin instanceof JavaPlugin javaPlugin) {
            return javaPlugin.getClassLoader();
        }
        return null;
    }

    private static boolean invokeSetAbility(@Nonnull UUID uuid, @Nonnull String id, @Nonnull Object value) {
        try {
            ClassLoader cl = abilityClassLoader();
            if (cl == null) {
                return false;
            }
            Class<?> service = cl.loadClass(SERVICE_CLASS);
            Method method = service.getMethod("setAbility", UUID.class, String.class, Object.class);
            method.invoke(null, uuid, id, value);
            return true;
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "setAbility failed for " + id, e);
            return false;
        }
    }

    private static void invokeRemoveAbility(@Nonnull UUID uuid, @Nonnull String id) {
        try {
            ClassLoader cl = abilityClassLoader();
            if (cl == null) {
                return;
            }
            Class<?> service = cl.loadClass(SERVICE_CLASS);
            Method method = service.getMethod("removeAbility", UUID.class, String.class);
            method.invoke(null, uuid, id);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "removeAbility failed for " + id, e);
        }
    }

    private static void invokeSetConditions(@Nonnull UUID uuid, @Nonnull String id, @Nonnull List<Object> conditions) {
        try {
            ClassLoader cl = abilityClassLoader();
            if (cl == null) {
                return;
            }
            Class<?> service = cl.loadClass(SERVICE_CLASS);
            Method method = service.getMethod("setConditions", UUID.class, String.class, List.class);
            method.invoke(null, uuid, id, conditions);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "setConditions failed for " + id, e);
        }
    }

    private static void invokeApplyForPlayer(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> store,
            @Nonnull World world
    ) {
        try {
            ClassLoader cl = abilityClassLoader();
            if (cl == null) {
                return;
            }
            Class<?> service = cl.loadClass(SERVICE_CLASS);
            Method method = service.getMethod(
                    "applyForPlayer",
                    Ref.class,
                    ComponentAccessor.class,
                    World.class
            );
            method.invoke(null, ref, store, world);
        } catch (ReflectiveOperationException e) {
            LOGGER.log(Level.WARNING, "applyForPlayer failed", e);
        }
    }

    @Nonnull
    private static Object newCondition(@Nonnull ClassLoader cl, @Nonnull String type, int param)
            throws ReflectiveOperationException {
        Class<?> conditionClass = cl.loadClass(CONDITION_CLASS);
        return conditionClass.getConstructor(String.class, int.class).newInstance(type, param);
    }

    @Nonnull
    private static Object newConditionWithZones(
            @Nonnull ClassLoader cl,
            @Nonnull String type,
            int param,
            @Nonnull List<Integer> zoneIds
    ) throws ReflectiveOperationException {
        Class<?> conditionClass = cl.loadClass(CONDITION_CLASS);
        return conditionClass.getConstructor(String.class, int.class, List.class).newInstance(type, param, zoneIds);
    }
}

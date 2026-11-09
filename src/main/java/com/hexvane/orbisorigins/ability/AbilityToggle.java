package com.hexvane.orbisorigins.ability;

import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Switches a player's racial abilities off and on. The choice is saved, and every place that grants
 * species abilities (join, reload, choosing a species) respects it. Runs on the player's world thread.
 */
public final class AbilityToggle {
    private AbilityToggle() {}

    /**
     * @param enable true to switch on, false to switch off, null to flip the current state
     * @return the message to show the player
     */
    @Nonnull
    public static Message set(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world,
            @Nullable Boolean enable
    ) {
        boolean wasDisabled = PlayerSpeciesData.getAbilitiesDisabled(ref, store);
        boolean disable = enable == null ? !wasDisabled : !enable;
        PlayerSpeciesData.setAbilitiesDisabled(ref, store, disable);

        SpeciesData species = currentSpecies(ref, store, world);
        String name = species != null && !species.getAbilities().isEmpty() ? species.getDisplayName() : null;
        if (species != null && AbilityApiBridge.isAvailable() && !species.getAbilities().isEmpty()) {
            if (disable) {
                AbilityApiBridge.clearSpeciesAbilities(playerRef, ref, store, world, species);
            } else {
                AbilityApiBridge.applySpeciesAbilities(playerRef, ref, store, world, species);
            }
        }

        String state = disable ? "off" : "on";
        if (name == null) {
            return Message.raw("[Orbis Origins] Racial abilities " + state + ". (Your species has none right now.)");
        }
        return Message.raw("[Orbis Origins] " + name + " abilities " + state + ".");
    }

    @Nullable
    private static SpeciesData currentSpecies(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull World world) {
        if (!PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
            return null;
        }
        String id = PlayerSpeciesData.getEffectiveSpeciesId(ref, store, world);
        return id == null ? null : SpeciesRegistry.getSpeciesOrDefault(id);
    }
}

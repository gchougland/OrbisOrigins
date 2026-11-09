package com.hexvane.orbisorigins.commands;

import com.hexvane.orbisorigins.ability.AbilityToggle;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Switches your racial abilities off or on; the choice is remembered. Anyone can run it for themselves.
 * Usage: /origins ability [on|off], omit to toggle. Crouch and press the game mode key (O) to toggle too.
 */
public class OriginsAbilityCommand extends AbstractPlayerCommand {

    public OriginsAbilityCommand() {
        super("ability", "Toggle your racial abilities");
        requireNoPermission();
        addUsageVariant(new SetVariant());
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        context.sendMessage(AbilityToggle.set(ref, store, playerRef, world, null));
    }

    /** /origins ability on|off */
    private static final class SetVariant extends AbstractPlayerCommand {
        @Nonnull
        private final RequiredArg<String> stateArg = this.withRequiredArg("on|off", "Switch your racial abilities on or off", ArgTypes.STRING);

        SetVariant() {
            super("Switch your racial abilities on or off");
            requireNoPermission();
        }

        @Override
        protected void execute(
                @Nonnull CommandContext context,
                @Nonnull Store<EntityStore> store,
                @Nonnull Ref<EntityStore> ref,
                @Nonnull PlayerRef playerRef,
                @Nonnull World world
        ) {
            String raw = stateArg.get(context);
            String state = raw == null ? "" : raw.trim().toLowerCase();
            if (!"on".equals(state) && !"off".equals(state)) {
                context.sendMessage(Message.raw("[Orbis Origins] Use /origins ability on or /origins ability off."));
                return;
            }
            context.sendMessage(AbilityToggle.set(ref, store, playerRef, world, "on".equals(state)));
        }
    }
}

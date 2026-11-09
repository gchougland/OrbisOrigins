package com.hexvane.orbisorigins.commands;

import com.hexvane.orbisorigins.gui.SpeciesSelectionPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

/**
 * Opens the species selection page without a Species Selector item. Needs the command's
 * permission (operators by default), so players cannot re-pick their species for free.
 * Usage: /origins choose
 */
public class OriginsChooseCommand extends AbstractPlayerCommand {

    public OriginsChooseCommand() {
        super("choose", "Open the species selection page");
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        player.getPageManager().openCustomPage(ref, store, new SpeciesSelectionPage(playerRef, world));
    }
}

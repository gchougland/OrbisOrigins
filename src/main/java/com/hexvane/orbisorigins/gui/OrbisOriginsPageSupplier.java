package com.hexvane.orbisorigins.gui;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OrbisOriginsPageSupplier implements OpenCustomUIInteraction.CustomPageSupplier {
    public static final BuilderCodec<OrbisOriginsPageSupplier> CODEC = BuilderCodec.builder(
            OrbisOriginsPageSupplier.class, OrbisOriginsPageSupplier::new
    ).build();

    @Override
    @Nullable
    public CustomUIPage tryCreate(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull ComponentAccessor<EntityStore> componentAccessor,
            @Nonnull PlayerRef playerRef,
            @Nonnull InteractionContext context
    ) {
        Player playerComponent = componentAccessor.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            return null;
        }

        World world = ref.getStore().getExternalData().getWorld();
        if (world == null) {
            return null;
        }

        return new SpeciesSelectionPage(playerRef, world);
    }
}

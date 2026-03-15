package com.hexvane.orbisorigins.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Command to hide or show the player's species model (default skin vs species appearance).
 * No permission required; anyone can run it for themselves.
 * Usage: /origins hidemodel [hide|show] — omit to toggle.
 */
public class OriginsHideModelCommand extends AbstractPlayerCommand {

    @Nonnull
    private final OptionalArg<String> actionArg = this.withOptionalArg(
            "hide|show",
            "Hide or show your species model (omit to toggle)",
            ArgTypes.STRING
    );

    public OriginsHideModelCommand() {
        super("hidemodel", "Hide or show your species model");
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    protected void execute(
            @Nonnull CommandContext context,
            @Nonnull Store<EntityStore> store,
            @Nonnull Ref<EntityStore> ref,
            @Nonnull PlayerRef playerRef,
            @Nonnull World world
    ) {
        boolean wantHide;
        if (actionArg.provided(context)) {
            String raw = actionArg.get(context);
            if (raw == null) {
                raw = "";
            }
            String action = raw.trim().toLowerCase();
            if ("hide".equals(action)) {
                wantHide = true;
            } else if ("show".equals(action)) {
                wantHide = false;
            } else {
                context.sendMessage(Message.raw("[Orbis Origins] Use /origins hidemodel hide or /origins hidemodel show."));
                return;
            }
        } else {
            // Toggle
            wantHide = !PlayerSpeciesData.getSpeciesModelHidden(ref, store);
        }

        if (wantHide) {
            PlayerSpeciesData.setSpeciesModelHidden(ref, store, true);
            ModelUtil.resetToPlayerSkin(ref, store);
            context.sendMessage(Message.raw("[Orbis Origins] Species model hidden. You now appear as your default skin."));
            return;
        }

        // Show: clear flag and re-apply species model
        PlayerSpeciesData.setSpeciesModelHidden(ref, store, false);
        if (!PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
            context.sendMessage(Message.raw("[Orbis Origins] Species model shown. (You have not chosen a species yet.)"));
            return;
        }
        String speciesId = PlayerSpeciesData.getEffectiveSpeciesId(ref, store, world);
        int variantIndex = PlayerSpeciesData.getEffectiveVariantIndex(ref, store, world);
        if (speciesId == null) {
            context.sendMessage(Message.raw("[Orbis Origins] Species model shown."));
            return;
        }
        SpeciesData species = SpeciesRegistry.getSpeciesOrDefault(speciesId);
        if (species == null || species.usesPlayerModel()) {
            context.sendMessage(Message.raw("[Orbis Origins] Species model shown."));
            return;
        }
        Map<String, String> attachmentSelections = PlayerSpeciesData.getAttachmentSelections(ref, store, world);
        String textureSelection = PlayerSpeciesData.getTextureSelection(ref, store, world);
        if (species.isVersion2()) {
            ModelUtil.applyModelToPlayerV2(ref, store, species, variantIndex, textureSelection, attachmentSelections);
        } else {
            String modelName = species.getModelName(variantIndex);
            float eyeHeightModifier = species.getEyeHeightModifier(modelName);
            float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
            float scale = species.getModelScale(variantIndex);
            ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier, attachmentSelections, scale);
        }
        context.sendMessage(Message.raw("[Orbis Origins] Species model shown."));
    }
}

package com.hexvane.orbisorigins.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SpeciesSelectionPage extends InteractiveCustomUIPage<SpeciesSelectionPage.SpeciesEventData> {
    private final World world;
    private final PlayerRef playerRef;
    @Nullable
    private String selectedSpeciesId;
    private final Map<String, Integer> variantIndices = new HashMap<>(); // species ID -> variant index
    @Nullable
    private Ref<EntityStore> modelPreview;
    private Vector3d previewPosition;
    private Vector3f previewRotation;

    public SpeciesSelectionPage(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpeciesEventData.CODEC);
        this.world = world;
        this.playerRef = playerRef;
        
        // Initialize with first species selected
        List<SpeciesData> allSpecies = SpeciesRegistry.getAllSpecies();
        if (!allSpecies.isEmpty()) {
            this.selectedSpeciesId = allSpecies.get(0).getId();
            this.variantIndices.put(this.selectedSpeciesId, 0);
        }
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            @Nonnull Store<EntityStore> store
    ) {
        // Append the main UI layout
        commandBuilder.append("Pages/SpeciesSelectionPage.ui");
        
        // Build species list
        buildSpeciesList(ref, store, commandBuilder, eventBuilder);
        
        // Update description and preview for selected species
        if (selectedSpeciesId != null) {
            updateDescription(commandBuilder);
            updateVariantControls(commandBuilder);
        }
        
        // Schedule preview entity creation for next tick (can't modify store during build)
        if (selectedSpeciesId != null) {
            world.execute(() -> {
                Ref<EntityStore> playerRef = this.playerRef.getReference();
                if (playerRef != null && playerRef.isValid()) {
                    Store<EntityStore> playerStore = playerRef.getStore();
                    createPreviewEntity(playerRef, playerStore);
                }
            });
        }
        
        // Register event bindings
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ConfirmButton",
                EventData.of("Action", "Confirm"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelButton",
                EventData.of("Action", "Cancel"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#LeftArrow",
                EventData.of("Action", "PreviousVariant"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RightArrow",
                EventData.of("Action", "NextVariant"),
                false
        );
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull SpeciesEventData data
    ) {
        if (data.action != null) {
            switch (data.action) {
                case "SelectSpecies":
                    if (data.speciesId != null) {
                        selectSpecies(ref, store, data.speciesId);
                    }
                    break;
                case "Confirm":
                    confirmSelection(ref, store);
                    break;
                case "Cancel":
                    this.close();
                    break;
                case "PreviousVariant":
                    cycleVariant(ref, store, -1);
                    break;
                case "NextVariant":
                    cycleVariant(ref, store, 1);
                    break;
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (this.modelPreview != null && this.modelPreview.isValid()) {
            store.removeEntity(this.modelPreview, RemoveReason.REMOVE);
        }
    }

    private void buildSpeciesList(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        commandBuilder.clear("#SpeciesList");
        
        List<SpeciesData> allSpecies = SpeciesRegistry.getAllSpecies();
        for (int i = 0; i < allSpecies.size(); i++) {
            SpeciesData species = allSpecies.get(i);
            String selector = "#SpeciesList[" + i + "]";
            
            // Append species entry template
            commandBuilder.append("#SpeciesList", "Pages/SpeciesEntry.ui");
            
            // Set species name
            commandBuilder.set(selector + " #Name.Text", species.getDisplayName());
            
            // Highlight selected species
            boolean isSelected = species.getId().equals(selectedSpeciesId);
            if (isSelected) {
                commandBuilder.set(selector + ".Style.Default.Background", "#4a90e2");
            }
            
            // Register click event
            EventData eventData = EventData.of("Action", "SelectSpecies");
            eventData.append("SpeciesId", species.getId());
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selector,
                    eventData,
                    false
            );
        }
    }

    private void selectSpecies(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull String speciesId) {
        this.selectedSpeciesId = speciesId;
        if (!variantIndices.containsKey(speciesId)) {
            variantIndices.put(speciesId, 0);
        }
        
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        
        // Rebuild species list to update selection highlight
        buildSpeciesList(ref, store, commandBuilder, eventBuilder);
        
        // Update description and variant controls
        updateDescription(commandBuilder);
        updateVariantControls(commandBuilder);
        
        // Schedule preview entity update for next tick (can't modify store during event handling)
        world.execute(() -> {
            Ref<EntityStore> playerRef = this.playerRef.getReference();
            if (playerRef != null && playerRef.isValid()) {
                Store<EntityStore> playerStore = playerRef.getStore();
                createPreviewEntity(playerRef, playerStore);
            }
        });
        
        this.sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void updateDescription(@Nonnull UICommandBuilder commandBuilder) {
        if (selectedSpeciesId == null) return;
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;
        
        commandBuilder.set("#SpeciesName.Text", species.getDisplayName());
        
        // Build full description with buffs/debuffs
        StringBuilder descriptionText = new StringBuilder();
        
        // Manually wrap description text (split into lines of ~25 characters)
        String description = species.getDescription();
        String[] words = description.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > 25) {
                if (currentLine.length() > 0) {
                    descriptionText.append(currentLine.toString().trim()).append("\n");
                    currentLine = new StringBuilder();
                }
            }
            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }
        if (currentLine.length() > 0) {
            descriptionText.append(currentLine.toString().trim());
        }
        
        descriptionText.append("\n\n");
        
        // Health modifier
        int healthMod = species.getHealthModifier();
        if (healthMod != 0) {
            descriptionText.append("Max Health: ");
            if (healthMod > 0) {
                descriptionText.append("+").append(healthMod);
            } else {
                descriptionText.append(healthMod);
            }
            descriptionText.append("\n");
        }
        
        // Stamina modifier
        int staminaMod = species.getStaminaModifier();
        if (staminaMod != 0) {
            descriptionText.append("Max Stamina: ");
            if (staminaMod > 0) {
                descriptionText.append("+").append(staminaMod);
            } else {
                descriptionText.append(staminaMod);
            }
            descriptionText.append("\n");
        }
        
        // Mana modifier
        int manaMod = species.getManaModifier();
        if (manaMod != 0) {
            descriptionText.append("Max Mana: ");
            if (manaMod > 0) {
                descriptionText.append("+").append(manaMod);
            } else {
                descriptionText.append(manaMod);
            }
            descriptionText.append("\n");
        }
        
        // Damage resistances
        Map<String, Float> resistances = species.getDamageResistances();
        if (!resistances.isEmpty()) {
            descriptionText.append("\nResistances:\n");
            for (Map.Entry<String, Float> entry : resistances.entrySet()) {
                String damageType = entry.getKey();
                Float multiplier = entry.getValue();
                if (multiplier == 0.0f) {
                    descriptionText.append("  ").append(damageType).append(": Immune\n");
                } else if (multiplier < 1.0f) {
                    int reduction = Math.round((1.0f - multiplier) * 100);
                    descriptionText.append("  ").append(damageType).append(": -").append(reduction).append("%\n");
                } else if (multiplier > 1.0f) {
                    int increase = Math.round((multiplier - 1.0f) * 100);
                    descriptionText.append("  ").append(damageType).append(": +").append(increase).append("%\n");
                }
            }
        }
        
        // Starter items
        List<String> starterItems = species.getStarterItems();
        if (!starterItems.isEmpty()) {
            descriptionText.append("\nStarter Items:\n");
            for (String itemId : starterItems) {
                descriptionText.append("  - ").append(itemId).append("\n");
            }
        }
        
        commandBuilder.set("#SpeciesDescription.Text", descriptionText.toString());
    }

    private void createPreviewEntity(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (selectedSpeciesId == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("createPreviewEntity: selectedSpeciesId is null");
            return;
        }
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("createPreviewEntity: species not found: " + selectedSpeciesId);
            return;
        }
        
        // Skip preview for orbian (no model)
        if (species.getId().equals("orbian")) {
            if (modelPreview != null && modelPreview.isValid()) {
                store.removeEntity(modelPreview, RemoveReason.REMOVE);
                modelPreview = null;
            }
            return;
        }
        
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        String modelName = species.getModelName(variantIndex);
        
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        logger.info("createPreviewEntity: Creating preview for model: " + modelName);
        
        Model model = ModelUtil.getModel(modelName);
        if (model == null) {
            logger.warning("createPreviewEntity: Failed to get model: " + modelName);
            return;
        }
        
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        if (transformComponent == null) {
            logger.warning("createPreviewEntity: TransformComponent is null");
            return;
        }
        
        HeadRotation headRotationComponent = store.getComponent(ref, HeadRotation.getComponentType());
        if (headRotationComponent == null) {
            logger.warning("createPreviewEntity: HeadRotation is null");
            return;
        }
        
        Vector3d playerPosition = transformComponent.getPosition();
        Vector3f headRotation = headRotationComponent.getRotation();
        
        // Spawn entity 4 blocks in front of player
        Vector3d direction = Transform.getDirection(headRotation.getPitch(), headRotation.getYaw());
        Vector3d previewPosition = TargetUtil.getTargetLocation(ref, 8.0, store);
        if (previewPosition == null) {
            previewPosition = playerPosition.clone().add(direction.scale(4.0));
        }
        
        // Find ground level for the preview entity
        Vector3d targetGround = TargetUtil.getTargetLocation(
                store.getExternalData().getWorld(),
                blockId -> blockId != 0,
                previewPosition.x, previewPosition.y, previewPosition.z,
                0.0, -1.0, 0.0, 8.0
        );
        if (targetGround != null) {
            previewPosition = targetGround;
        } else {
            // Fallback: spawn at player's feet level
            previewPosition.setY(playerPosition.y);
        }
        
        // Make entity face player
        Vector3d relativePos = playerPosition.clone().subtract(previewPosition);
        relativePos.setY(0.0);
        Vector3f previewRot = Vector3f.lookAt(relativePos);
        
        Vector3d spawnPosition = previewPosition;
        
        if (modelPreview == null || !modelPreview.isValid()) {
            // Create new preview entity
            logger.info("createPreviewEntity: Creating new preview entity at " + spawnPosition);
            Holder<EntityStore> holder = store.getRegistry().newHolder();
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(previewRot));
            
            this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
            logger.info("createPreviewEntity: Preview entity created with ref: " + (modelPreview != null ? modelPreview.isValid() : "null"));
        } else {
            // Update existing preview model and position
            logger.info("createPreviewEntity: Updating existing preview entity");
            store.putComponent(modelPreview, ModelComponent.getComponentType(), new ModelComponent(model));
            store.putComponent(modelPreview, TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            store.putComponent(modelPreview, HeadRotation.getComponentType(), new HeadRotation(previewRot));
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
        }
    }

    private void cycleVariant(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int direction) {
        if (selectedSpeciesId == null) return;
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;
        
        List<String> variants = species.getVariants();
        if (variants.isEmpty()) return;
        
        int currentIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        int newIndex = currentIndex + direction;
        
        // Wrap around
        if (newIndex < 0) {
            newIndex = variants.size() - 1;
        } else if (newIndex >= variants.size()) {
            newIndex = 0;
        }
        
        variantIndices.put(selectedSpeciesId, newIndex);
        
        UICommandBuilder commandBuilder = new UICommandBuilder();
        updateVariantControls(commandBuilder);
        
        // Schedule preview entity update for next tick (can't modify store during event handling)
        world.execute(() -> {
            Ref<EntityStore> playerRef = this.playerRef.getReference();
            if (playerRef != null && playerRef.isValid()) {
                Store<EntityStore> playerStore = playerRef.getStore();
                createPreviewEntity(playerRef, playerStore);
            }
        });
        
        this.sendUpdate(commandBuilder, null, false);
    }

    private void updateVariantControls(@Nonnull UICommandBuilder commandBuilder) {
        if (selectedSpeciesId == null) return;
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;
        
        List<String> variants = species.getVariants();
        int currentIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        
        // Show variant label if there are multiple variants
        if (variants.size() > 1) {
            commandBuilder.set("#VariantLabel.Text", String.format("Variant %d / %d", currentIndex + 1, variants.size()));
            commandBuilder.set("#LeftArrow.Visible", true);
            commandBuilder.set("#RightArrow.Visible", true);
        } else {
            commandBuilder.set("#VariantLabel.Text", "");
            commandBuilder.set("#LeftArrow.Visible", false);
            commandBuilder.set("#RightArrow.Visible", false);
        }
    }

    private void confirmSelection(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        if (selectedSpeciesId == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: selectedSpeciesId is null");
            return;
        }
        
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: Player component is null");
            return;
        }
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) {
            java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName()).warning("confirmSelection: Species not found: " + selectedSpeciesId);
            return;
        }
        
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        logger.info("confirmSelection: Applying species " + selectedSpeciesId + " variant " + variantIndex);
        
        // Apply model (if not orbian)
        if (!species.getId().equals("orbian")) {
            String modelName = species.getModelName(variantIndex);
            float eyeHeightModifier = species.getEyeHeightModifier(modelName);
            float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
            logger.info("confirmSelection: Applying model: " + modelName);
            ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier);
        } else {
            // Reset to player skin for orbian
            logger.info("confirmSelection: Resetting to player skin");
            ModelUtil.resetToPlayerSkin(ref, store);
        }
        
        // Apply species stats
        SpeciesStatUtil.applySpeciesStats(ref, store, species);
        
        // Give starter items
        for (String itemId : species.getStarterItems()) {
            ItemStack itemStack = new ItemStack(itemId, 1);
            playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(itemStack);
        }
        
        // Store choice
        PlayerSpeciesData.setSpeciesSelection(ref, store, world, selectedSpeciesId, variantIndex);
        
        // Consume item from inventory
        consumeSelectorItem(playerComponent);
        
        // Clean up preview
        if (modelPreview != null && modelPreview.isValid()) {
            store.removeEntity(modelPreview, RemoveReason.REMOVE);
        }
        
        // Close GUI
        this.close();
    }

    private void consumeSelectorItem(@Nonnull Player playerComponent) {
        // Find and remove the species selector item
        var inventory = playerComponent.getInventory();
        var hotbar = inventory.getHotbar();
        
        // Check hotbar first
        short hotbarCapacity = hotbar.getCapacity();
        for (short i = 0; i < hotbarCapacity; i++) {
            var itemStack = hotbar.getItemStack(i);
            if (itemStack != null && "OrbisOrigins_Species_Selector".equals(itemStack.getItemId())) {
                hotbar.removeItemStackFromSlot(i);
                return;
            }
        }
        
        // Check storage inventory if not found in hotbar
        var storage = inventory.getStorage();
        short storageCapacity = storage.getCapacity();
        for (short i = 0; i < storageCapacity; i++) {
            var itemStack = storage.getItemStack(i);
            if (itemStack != null && "OrbisOrigins_Species_Selector".equals(itemStack.getItemId())) {
                storage.removeItemStackFromSlot(i);
                return;
            }
        }
    }

    public static class SpeciesEventData {
        public static final BuilderCodec<SpeciesEventData> CODEC = BuilderCodec.builder(
                SpeciesEventData.class, SpeciesEventData::new
        )
        .addField(new KeyedCodec<>("Action", Codec.STRING), (data, s) -> data.action = s, data -> data.action)
        .addField(new KeyedCodec<>("SpeciesId", Codec.STRING), (data, s) -> data.speciesId = s, data -> data.speciesId)
        .build();
        
        private String action;
        private String speciesId;
    }
}

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
import com.hexvane.orbisorigins.species.AttachmentOption;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import com.hexvane.orbisorigins.util.ModelUtil;
import com.hexvane.orbisorigins.util.SpeciesStatUtil;
import java.util.ArrayList;
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
    private final Map<String, Map<String, String>> attachmentSelections = new HashMap<>(); // species ID -> attachment type -> selected option name
    private final Map<String, String> textureSelections = new HashMap<>(); // species ID -> selected texture path (v2 only)
    @Nullable
    private Ref<EntityStore> modelPreview;
    private Vector3d previewPosition;
    private Vector3f previewRotation;
    /** Yaw offset in radians for rotating the preview model so the player can see all sides. */
    private float previewRotationYawOffset;
    private volatile boolean dismissed;

    public SpeciesSelectionPage(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, SpeciesEventData.CODEC);
        this.world = world;
        this.playerRef = playerRef;
        
        // Initialize with first species selected
        List<SpeciesData> allSpecies = SpeciesRegistry.getAllSpecies();
        if (!allSpecies.isEmpty()) {
            this.selectedSpeciesId = allSpecies.get(0).getId();
            this.variantIndices.put(this.selectedSpeciesId, 0);
            this.attachmentSelections.put(this.selectedSpeciesId, new HashMap<>());
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
            updateTextureSelector(ref, store, commandBuilder, eventBuilder);
            updateAttachmentSelectors(ref, store, commandBuilder, eventBuilder);
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
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RotatePreviewLeft",
                EventData.of("Action", "RotatePreviewLeft"),
                false
        );
        
        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#RotatePreviewRight",
                EventData.of("Action", "RotatePreviewRight"),
                false
        );
        
        // Attachment selector events are registered dynamically in updateAttachmentSelectors
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
                case "PreviousAttachment":
                    if (data.attachmentType != null) {
                        cycleAttachment(ref, store, data.attachmentType, -1);
                    }
                    break;
                case "NextAttachment":
                    if (data.attachmentType != null) {
                        cycleAttachment(ref, store, data.attachmentType, 1);
                    }
                    break;
                case "PreviousTexture":
                    cycleTexture(ref, store, -1);
                    break;
                case "NextTexture":
                    cycleTexture(ref, store, 1);
                    break;
                case "RotatePreviewLeft":
                    rotatePreview(ref, store, 1);
                    break;
                case "RotatePreviewRight":
                    rotatePreview(ref, store, -1);
                    break;
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        this.dismissed = true;
        if (this.modelPreview != null && this.modelPreview.isValid()) {
            Ref<EntityStore> toRemove = this.modelPreview;
            this.modelPreview = null;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (toRemove.isValid()) {
                    storeRef.removeEntity(toRemove, RemoveReason.REMOVE);
                }
            });
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
        if (!attachmentSelections.containsKey(speciesId)) {
            attachmentSelections.put(speciesId, new HashMap<>());
        }
        // Initialize texture selection for v2 species
        SpeciesData species = SpeciesRegistry.getSpecies(speciesId);
        if (species != null && species.isVersion2()) {
            com.hexvane.orbisorigins.species.SpeciesVariantData v = species.getVariantData(variantIndices.get(speciesId));
            if (v != null && !v.getTextures().isEmpty() && !textureSelections.containsKey(speciesId)) {
                textureSelections.put(speciesId, v.getTextures().get(0));
            }
        }
        
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        
        // Rebuild species list to update selection highlight
        buildSpeciesList(ref, store, commandBuilder, eventBuilder);
        
        // Update description and variant controls
        updateDescription(commandBuilder);
        updateVariantControls(commandBuilder);
        updateTextureSelector(ref, store, commandBuilder, eventBuilder);
        updateAttachmentSelectors(ref, store, commandBuilder, eventBuilder);
        
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
        if (dismissed) {
            return;
        }
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
        if (species.usesPlayerModel()) {
            if (modelPreview != null && modelPreview.isValid()) {
                store.removeEntity(modelPreview, RemoveReason.REMOVE);
                modelPreview = null;
            }
            return;
        }
        
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        Map<String, String> attachmentSelectionsForSpecies = attachmentSelections.getOrDefault(selectedSpeciesId, new HashMap<>());
        String textureSelectionForSpecies = textureSelections.get(selectedSpeciesId);
        
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        com.hypixel.hytale.server.core.asset.type.model.config.Model model;
        if (species.isVersion2()) {
            model = ModelUtil.createModelForV2(species, variantIndex, textureSelectionForSpecies, attachmentSelectionsForSpecies);
            if (model == null) {
                logger.warning("createPreviewEntity: Failed to create v2 model");
                return;
            }
        } else {
            String modelName = species.getModelName(variantIndex);
            com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset modelAsset = 
                com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.getAssetMap().getAsset(modelName);
            if (modelAsset == null) {
                logger.warning("createPreviewEntity: Model asset not found: " + modelName);
                return;
            }
            com.hypixel.hytale.server.core.asset.type.model.config.Model baseModel = ModelUtil.getModel(modelName);
            if (baseModel == null) {
                logger.warning("createPreviewEntity: Failed to get model: " + modelName);
                return;
            }
            Map<String, String> attachmentMap = new HashMap<>();
            if (baseModel.getRandomAttachmentIds() != null) {
                attachmentMap.putAll(baseModel.getRandomAttachmentIds());
            }
            for (Map.Entry<String, String> entry : attachmentSelectionsForSpecies.entrySet()) {
                String attachmentType = entry.getKey();
                String selectedOption = entry.getValue();
                if (selectedOption != null && !selectedOption.isEmpty() && !"null".equals(selectedOption)) {
                    attachmentMap.put(attachmentType, selectedOption);
                } else {
                    attachmentMap.remove(attachmentType);
                }
            }
            model = com.hypixel.hytale.server.core.asset.type.model.config.Model.createScaledModel(
                modelAsset, baseModel.getScale(), attachmentMap);
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
        
        // Make entity face player, then apply rotation offset so player can spin the preview
        Vector3d relativePos = playerPosition.clone().subtract(previewPosition);
        relativePos.setY(0.0);
        Vector3f previewRot = Vector3f.lookAt(relativePos);
        previewRot = new Vector3f(previewRot.getPitch(), previewRot.getYaw() + previewRotationYawOffset, previewRot.getRoll());
        
        Vector3d spawnPosition = previewPosition;
        
        if (modelPreview == null || !modelPreview.isValid()) {
            // Create new preview entity
            Holder<EntityStore> holder = store.getRegistry().newHolder();
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(store.getExternalData().takeNextNetworkId()));
            holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
            holder.addComponent(HeadRotation.getComponentType(), new HeadRotation(previewRot));
            
            this.modelPreview = store.addEntity(holder, AddReason.SPAWN);
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
        } else {
            // Update existing preview model and position
            store.putComponent(modelPreview, ModelComponent.getComponentType(), new ModelComponent(model));
            store.putComponent(modelPreview, TransformComponent.getComponentType(), new TransformComponent(spawnPosition, previewRot));
            store.putComponent(modelPreview, HeadRotation.getComponentType(), new HeadRotation(previewRot));
            this.previewPosition = spawnPosition;
            this.previewRotation = previewRot;
        }
    }

    private static final float ROTATE_PREVIEW_STEP_RADIANS = (float) (Math.PI / 12); // 15 degrees per click

    private void rotatePreview(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int direction) {
        if (dismissed) {
            return;
        }
        previewRotationYawOffset += direction * ROTATE_PREVIEW_STEP_RADIANS;
        if (previewRotation != null) {
            float newYaw = previewRotation.getYaw() + direction * ROTATE_PREVIEW_STEP_RADIANS;
            this.previewRotation = new Vector3f(previewRotation.getPitch(), newYaw, previewRotation.getRoll());
            Ref<EntityStore> previewRef = this.modelPreview;
            Vector3d pos = this.previewPosition;
            Vector3f rot = this.previewRotation;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (dismissed || previewRef == null || !previewRef.isValid()) {
                    return;
                }
                storeRef.putComponent(previewRef, TransformComponent.getComponentType(), new TransformComponent(pos, rot));
                storeRef.putComponent(previewRef, HeadRotation.getComponentType(), new HeadRotation(rot));
            });
        }
    }

    private void cycleVariant(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int direction) {
        if (selectedSpeciesId == null) return;
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;
        
        int variantCount = species.getVariantCount();
        if (variantCount == 0) return;
        
        int currentIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        int newIndex = currentIndex + direction;
        
        // Wrap around
        if (newIndex < 0) {
            newIndex = variantCount - 1;
        } else if (newIndex >= variantCount) {
            newIndex = 0;
        }
        
        variantIndices.put(selectedSpeciesId, newIndex);
        
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        updateVariantControls(commandBuilder);
        updateTextureSelector(ref, store, commandBuilder, eventBuilder);
        updateAttachmentSelectors(ref, store, commandBuilder, eventBuilder);
        
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

    private void updateVariantControls(@Nonnull UICommandBuilder commandBuilder) {
        if (selectedSpeciesId == null) return;
        
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;
        
        int variantCount = species.getVariantCount();
        int currentIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        
        // Show variant label if there are multiple variants
        if (variantCount > 1) {
            String label;
            if (species.isVersion2()) {
                com.hexvane.orbisorigins.species.SpeciesVariantData v = species.getVariantData(currentIndex);
                label = v != null ? v.getVariantName() : String.format("Variant %d / %d", currentIndex + 1, variantCount);
            } else {
                label = String.format("Variant %d / %d", currentIndex + 1, variantCount);
            }
            commandBuilder.set("#VariantLabel.Text", label);
            commandBuilder.set("#LeftArrow.Visible", true);
            commandBuilder.set("#RightArrow.Visible", true);
        } else {
            commandBuilder.set("#VariantLabel.Text", "");
            commandBuilder.set("#LeftArrow.Visible", false);
            commandBuilder.set("#RightArrow.Visible", false);
        }
    }

    private void updateTextureSelector(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        // Texture selector is added as first item in #AttachmentSelectors when v2 has multiple textures
        // Handled in updateAttachmentSelectors
    }

    private void cycleTexture(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, int direction) {
        if (selectedSpeciesId == null) return;
        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null || !species.isVersion2()) return;
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        com.hexvane.orbisorigins.species.SpeciesVariantData variant = species.getVariantData(variantIndex);
        if (variant == null || variant.getTextures().size() <= 1) return;
        java.util.List<String> textures = variant.getTextures();
        String current = textureSelections.getOrDefault(selectedSpeciesId, textures.get(0));
        int idx = textures.indexOf(current);
        if (idx < 0) idx = 0;
        int newIdx = idx + direction;
        if (newIdx < 0) newIdx = textures.size() - 1;
        else if (newIdx >= textures.size()) newIdx = 0;
        textureSelections.put(selectedSpeciesId, textures.get(newIdx));
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        updateAttachmentSelectors(ref, store, commandBuilder, eventBuilder);
        world.execute(() -> {
            Ref<EntityStore> playerRef = this.playerRef.getReference();
            if (playerRef != null && playerRef.isValid()) {
                createPreviewEntity(playerRef, playerRef.getStore());
            }
        });
        this.sendUpdate(commandBuilder, eventBuilder, false);
    }

    private void updateAttachmentSelectors(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder
    ) {
        if (selectedSpeciesId == null) {
            commandBuilder.set("#AttachmentSelectors.Visible", false);
            return;
        }

        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) {
            commandBuilder.set("#AttachmentSelectors.Visible", false);
            return;
        }

        // Skip orbian (no model, no attachments)
        if (species.usesPlayerModel()) {
            commandBuilder.set("#AttachmentSelectors.Visible", false);
            return;
        }

        // Get current variant to discover attachments for
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        String modelName = species.isVersion2() ? null : species.getModelName(variantIndex);

        // Get available attachments for this species/variant
        Map<String, Map<String, AttachmentOption>> availableAttachments = 
            SpeciesRegistry.getAvailableAttachments(species, variantIndex, modelName);

        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        
        // Count non-empty attachment types
        int nonEmptyTypes = 0;
        for (Map.Entry<String, Map<String, AttachmentOption>> entry : availableAttachments.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                nonEmptyTypes++;
            }
        }

        boolean hasTextureSelector = species.isVersion2() && species.getVariantData(variantIndex) != null
            && species.getVariantData(variantIndex).getTextures().size() > 1;
        if (nonEmptyTypes == 0 && !hasTextureSelector) {
            commandBuilder.set("#AttachmentSelectors.Visible", false);
            return;
        }

        // Show attachment selectors
        commandBuilder.set("#AttachmentSelectors.Visible", true);
        commandBuilder.clear("#AttachmentSelectors");

        int attachmentIndex = 0;
        // For v2 with multiple textures, add texture selector as first item
        if (species.isVersion2()) {
            com.hexvane.orbisorigins.species.SpeciesVariantData variant = species.getVariantData(variantIndex);
            if (variant != null && variant.getTextures().size() > 1) {
                java.util.List<String> textures = variant.getTextures();
                String currentTex = textureSelections.getOrDefault(selectedSpeciesId, textures.get(0));
                if (!textures.contains(currentTex)) {
                    currentTex = textures.get(0);
                    textureSelections.put(selectedSpeciesId, currentTex);
                }
                String displayName = currentTex.substring(Math.max(0, currentTex.lastIndexOf('/') + 1)).replace(".png", "");
                String selectorPrefix = "#AttachmentSelectors[" + attachmentIndex + "]";
                commandBuilder.append("#AttachmentSelectors", "Pages/AttachmentSelector.ui");
                commandBuilder.set(selectorPrefix + " #AttachmentTypeLabel.Text", "Texture:");
                commandBuilder.set(selectorPrefix + " #AttachmentLabel.Text", displayName);
                commandBuilder.set(selectorPrefix + " #AttachmentLeftArrow.Visible", true);
                commandBuilder.set(selectorPrefix + " #AttachmentRightArrow.Visible", true);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selectorPrefix + " #AttachmentLeftArrow",
                    EventData.of("Action", "PreviousTexture"), false);
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, selectorPrefix + " #AttachmentRightArrow",
                    EventData.of("Action", "NextTexture"), false);
                attachmentIndex++;
            }
        }

        // Get or initialize attachment selections for this species
        Map<String, String> selections = attachmentSelections.computeIfAbsent(selectedSpeciesId, k -> new HashMap<>());

        // Build a selector for each attachment type
        for (Map.Entry<String, Map<String, AttachmentOption>> attachmentTypeEntry : availableAttachments.entrySet()) {
            String attachmentType = attachmentTypeEntry.getKey();
            Map<String, AttachmentOption> options = attachmentTypeEntry.getValue();

            if (options.isEmpty()) {
                continue;
            }

            // Get current selection or default to first option
            String currentSelection = selections.get(attachmentType);
            List<String> optionNames = new ArrayList<>(options.keySet());
            if (currentSelection == null || !optionNames.contains(currentSelection)) {
                // Default to first option, or "null" if available
                if (optionNames.contains("null")) {
                    currentSelection = "null";
                } else {
                    currentSelection = optionNames.get(0);
                }
                selections.put(attachmentType, currentSelection);
            }

            int currentIndex = optionNames.indexOf(currentSelection);
            if (currentIndex < 0) {
                currentIndex = 0;
            }

            // Create selector UI elements
            String selectorPrefix = "#AttachmentSelectors[" + attachmentIndex + "]";
            
            // Append attachment selector template (we'll create it inline)
            commandBuilder.append("#AttachmentSelectors", "Pages/AttachmentSelector.ui");

            // Set attachment type label
            commandBuilder.set(selectorPrefix + " #AttachmentTypeLabel.Text", attachmentType + ":");

            // Set current selection label
            AttachmentOption selectedOption = options.get(currentSelection);
            String displayName = selectedOption != null ? selectedOption.getDisplayNameOrDefault(currentSelection) : currentSelection;
            commandBuilder.set(selectorPrefix + " #AttachmentLabel.Text", displayName);

            // Show/hide arrows based on number of options
            if (optionNames.size() > 1) {
                commandBuilder.set(selectorPrefix + " #AttachmentLeftArrow.Visible", true);
                commandBuilder.set(selectorPrefix + " #AttachmentRightArrow.Visible", true);
            } else {
                commandBuilder.set(selectorPrefix + " #AttachmentLeftArrow.Visible", false);
                commandBuilder.set(selectorPrefix + " #AttachmentRightArrow.Visible", false);
            }

            // Register event bindings
            EventData prevEvent = EventData.of("Action", "PreviousAttachment");
            prevEvent.append("AttachmentType", attachmentType);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selectorPrefix + " #AttachmentLeftArrow",
                    prevEvent,
                    false
            );

            EventData nextEvent = EventData.of("Action", "NextAttachment");
            nextEvent.append("AttachmentType", attachmentType);
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    selectorPrefix + " #AttachmentRightArrow",
                    nextEvent,
                    false
            );

            attachmentIndex++;
        }
    }

    private void cycleAttachment(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull String attachmentType,
            int direction
    ) {
        if (selectedSpeciesId == null) return;

        SpeciesData species = SpeciesRegistry.getSpecies(selectedSpeciesId);
        if (species == null) return;

        // Skip orbian
        if (species.usesPlayerModel()) return;

        // Get current variant
        int variantIndex = variantIndices.getOrDefault(selectedSpeciesId, 0);
        String modelName = species.isVersion2() ? null : species.getModelName(variantIndex);

        // Get available attachments
        Map<String, Map<String, AttachmentOption>> availableAttachments = 
            SpeciesRegistry.getAvailableAttachments(species, variantIndex, modelName);

        Map<String, AttachmentOption> options = availableAttachments.get(attachmentType);
        if (options == null || options.isEmpty()) return;

        // Get current selection
        Map<String, String> selections = attachmentSelections.computeIfAbsent(selectedSpeciesId, k -> new HashMap<>());
        String currentSelection = selections.get(attachmentType);
        List<String> optionNames = new ArrayList<>(options.keySet());

        // Find current index
        int currentIndex = optionNames.indexOf(currentSelection);
        if (currentIndex < 0) {
            currentIndex = 0;
        }

        // Cycle
        int newIndex = currentIndex + direction;
        if (newIndex < 0) {
            newIndex = optionNames.size() - 1;
        } else if (newIndex >= optionNames.size()) {
            newIndex = 0;
        }

        selections.put(attachmentType, optionNames.get(newIndex));

        // Update UI
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();
        updateAttachmentSelectors(ref, store, commandBuilder, eventBuilder);

        // Schedule preview update
        world.execute(() -> {
            Ref<EntityStore> playerRef = this.playerRef.getReference();
            if (playerRef != null && playerRef.isValid()) {
                Store<EntityStore> playerStore = playerRef.getStore();
                createPreviewEntity(playerRef, playerStore);
            }
        });

        this.sendUpdate(commandBuilder, eventBuilder, false);
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
        Map<String, String> attachmentSelectionsForSpecies = attachmentSelections.getOrDefault(selectedSpeciesId, new HashMap<>());
        String textureSelectionForSpecies = textureSelections.get(selectedSpeciesId);
        java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SpeciesSelectionPage.class.getName());
        
        // Apply model (if not orbian)
        if (!species.usesPlayerModel()) {
            if (species.isVersion2()) {
                ModelUtil.applyModelToPlayerV2(ref, store, species, variantIndex, textureSelectionForSpecies, attachmentSelectionsForSpecies);
            } else {
                String modelName = species.getModelName(variantIndex);
                float eyeHeightModifier = species.getEyeHeightModifier(modelName);
                float hitboxHeightModifier = species.getHitboxHeightModifier(modelName);
                ModelUtil.applyModelToPlayer(ref, store, modelName, eyeHeightModifier, hitboxHeightModifier, attachmentSelectionsForSpecies);
            }
        } else {
            // Reset to player skin for orbian
            ModelUtil.resetToPlayerSkin(ref, store);
        }
        
        // Apply species stats
        SpeciesStatUtil.applySpeciesStats(ref, store, species);
        
        // Give starter items
        for (String itemId : species.getStarterItems()) {
            ItemStack itemStack = new ItemStack(itemId, 1);
            playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(itemStack);
        }
        
        // Store choice with attachment selections and texture selection
        PlayerSpeciesData.setSpeciesSelection(ref, store, world, selectedSpeciesId, variantIndex, attachmentSelectionsForSpecies, textureSelectionForSpecies);
        
        // Consume item from inventory
        consumeSelectorItem(playerComponent);
        
        // Clean up preview - defer to next tick to avoid race with ChunkSavingSystems during serialization
        if (modelPreview != null && modelPreview.isValid()) {
            Ref<EntityStore> toRemove = modelPreview;
            modelPreview = null;
            Store<EntityStore> storeRef = store;
            world.execute(() -> {
                if (toRemove.isValid()) {
                    storeRef.removeEntity(toRemove, RemoveReason.REMOVE);
                }
            });
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
        .append(new KeyedCodec<>("Action", Codec.STRING), (data, s) -> data.action = s, data -> data.action)
        .add()
        .append(new KeyedCodec<>("SpeciesId", Codec.STRING), (data, s) -> data.speciesId = s, data -> data.speciesId)
        .add()
        .append(new KeyedCodec<>("AttachmentType", Codec.STRING), (data, s) -> data.attachmentType = s, data -> data.attachmentType)
        .add()
        .build();
        
        private String action;
        private String speciesId;
        private String attachmentType;
    }
}

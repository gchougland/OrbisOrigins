package com.hexvane.orbisorigins.systems;

import com.hypixel.hytale.builtin.mounts.BlockMountComponent;
import com.hypixel.hytale.builtin.mounts.MountedComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMount;
import com.hypixel.hytale.protocol.MountController;
import com.hypixel.hytale.protocol.MountedUpdate;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.mountpoints.BlockMountPoint;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hexvane.orbisorigins.data.PlayerSpeciesData;
import com.hexvane.orbisorigins.species.SpeciesData;
import com.hexvane.orbisorigins.species.SpeciesRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;

/**
 * Raises the player's world position when they enter the sleeping state (and lowers it when they wake)
 * by the amount configured in their species (sleepingRaiseHeight / per-variant SleepingRaiseHeight).
 * Prevents species whose models clip into the ground when lying down from suffocating.
 *
 * <p>The client draws the sleeping player at the position sent in the MountedUpdate's BlockMount,
 * not the entity transform. So we also send a corrected MountedUpdate with position.y raised to
 * all viewers so the model renders at the raised height. This system runs in the default group
 * (not QUEUE_UPDATE_GROUP) to avoid scheduler deadlocks during plugin registration.
 */
public class SpeciesSleepingRaiseSystem extends EntityTickingSystem<EntityStore> {
    private static final Query<EntityStore> QUERY = PlayerRef.getComponentType();

    private final Map<UUID, Boolean> previousSleeping = new HashMap<>();
    private final Map<UUID, Double> amountRaised = new HashMap<>();

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null || !ref.isValid()) {
            return;
        }

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
        MovementStatesComponent movementStatesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (uuidComponent == null || transformComponent == null || movementStatesComponent == null) {
            return;
        }

        UUID uuid = uuidComponent.getUuid();
        boolean nowSleeping = movementStatesComponent.getMovementStates().sleeping;
        Boolean wasSleeping = previousSleeping.get(uuid);

        if (nowSleeping && (wasSleeping == null || !wasSleeping)) {
            // Transition to sleeping
            World world = store.getExternalData().getWorld();
            if (world != null && PlayerSpeciesData.hasChosenSpecies(ref, store, world)) {
                String speciesId = PlayerSpeciesData.getEffectiveSpeciesId(ref, store, world);
                int variantIndex = PlayerSpeciesData.getEffectiveVariantIndex(ref, store, world);
                SpeciesData species = SpeciesRegistry.getSpeciesOrDefault(speciesId);
                if (species != null) {
                    float raise = species.getSleepingRaiseHeight(variantIndex);
                    if (raise > 0f) {
                        transformComponent.getPosition().y += raise;
                        amountRaised.put(uuid, (double) raise);
                        queueRaisedMountedUpdate(ref, store, raise);
                    }
                }
            }
        } else if (!nowSleeping && Boolean.TRUE.equals(wasSleeping)) {
            // Transition to not sleeping
            Double raised = amountRaised.remove(uuid);
            if (raised != null && raised != 0) {
                transformComponent.getPosition().y -= raised;
            }
        }

        previousSleeping.put(uuid, nowSleeping);
    }

    /**
     * Sends a MountedUpdate with the bed position raised by {@code raise} to all viewers,
     * so the client draws the sleeping model at the raised height.
     */
    private void queueRaisedMountedUpdate(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, float raise) {
        MountedComponent mounted = store.getComponent(ref, MountedComponent.getComponentType());
        if (mounted == null || mounted.getControllerType() != MountController.BlockMount) {
            return;
        }
        Ref<ChunkStore> blockRef = mounted.getMountedToBlock();
        if (blockRef == null || !blockRef.isValid()) {
            return;
        }
        Store<ChunkStore> chunkStore = blockRef.getStore();
        BlockMountComponent blockMountComponent = chunkStore.getComponent(blockRef, BlockMountComponent.getComponentType());
        if (blockMountComponent == null) {
            return;
        }
        BlockMountPoint occupiedSeat = blockMountComponent.getSeatBlockBySeatedEntity(ref);
        if (occupiedSeat == null) {
            return;
        }
        Vector3f position = occupiedSeat.computeWorldSpacePosition(blockMountComponent.getBlockPos());
        Vector3f rotationEuler = occupiedSeat.computeRotationEuler(blockMountComponent.getExpectedRotation());
        BlockType blockType = blockMountComponent.getExpectedBlockType();
        int blockTypeId = BlockType.getAssetMap().getIndex(blockType.getId());

        com.hypixel.hytale.protocol.Vector3f raisedPosition = new com.hypixel.hytale.protocol.Vector3f(position.x, position.y + raise, position.z);
        com.hypixel.hytale.protocol.Vector3f orientation = new com.hypixel.hytale.protocol.Vector3f(rotationEuler.x, rotationEuler.y, rotationEuler.z);
        BlockMount blockMount = new BlockMount(
                blockMountComponent.getType(),
                raisedPosition,
                orientation,
                blockTypeId
        );
        Vector3f attachmentOffset = mounted.getAttachmentOffset();
        com.hypixel.hytale.protocol.Vector3f netOffset = new com.hypixel.hytale.protocol.Vector3f(attachmentOffset.x, attachmentOffset.y, attachmentOffset.z);
        MountedUpdate mountedUpdate = new MountedUpdate(0, netOffset, MountController.BlockMount, blockMount);

        EntityTrackerSystems.Visible visible = store.getComponent(ref, EntityTrackerSystems.Visible.getComponentType());
        if (visible == null || visible.visibleTo.isEmpty()) {
            return;
        }
        for (EntityTrackerSystems.EntityViewer viewer : visible.visibleTo.values()) {
            viewer.queueUpdate(ref, mountedUpdate);
        }
    }
}

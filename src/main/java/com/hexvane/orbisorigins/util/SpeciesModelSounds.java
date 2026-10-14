package com.hexvane.orbisorigins.util;

import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Keeps a player who wears a creature's model from sounding like the creature.
 *
 * <p>A creature's model carries sounds on its animations: a zombie grunts every time its run animation plays, a Klops
 * mutters while it stands idle. On a player those play for as long as the player runs or stands. This returns the model's
 * animation sets with those sounds taken off. Two kinds stay: a sound the player model itself makes on the same animation
 * (jumping, swimming, climbing, falling), and the model's own hurt and death sounds, which answer something that happened
 * to the player.
 */
public final class SpeciesModelSounds {
    private static final String PLAYER_MODEL_ASSET_ID = "Player";

    /** Animation sets that keep the creature's own sound. Matched as a prefix: Hurt, HurtFall, Death, DeathDrown and so on. */
    private static final String[] KEPT_SET_PREFIXES = {"Hurt", "Death"};

    private SpeciesModelSounds() {
    }

    /**
     * @param sets a creature model's animation sets, by id
     * @return the same map when nothing had to change, otherwise a copy in which the changed sets are replaced
     */
    @Nullable
    public static Map<String, ModelAsset.AnimationSet> withoutCreatureSounds(@Nullable Map<String, ModelAsset.AnimationSet> sets) {
        ModelAsset player = ModelAsset.getAssetMap().getAsset(PLAYER_MODEL_ASSET_ID);
        return withoutCreatureSounds(sets, player == null ? null : player.getAnimationSetMap());
    }

    @Nullable
    static Map<String, ModelAsset.AnimationSet> withoutCreatureSounds(
            @Nullable Map<String, ModelAsset.AnimationSet> sets,
            @Nullable Map<String, ModelAsset.AnimationSet> playerSets
    ) {
        if (sets == null || sets.isEmpty()) {
            return sets;
        }
        Map<String, ModelAsset.AnimationSet> quiet = null;
        for (Map.Entry<String, ModelAsset.AnimationSet> entry : sets.entrySet()) {
            String id = entry.getKey();
            ModelAsset.AnimationSet set = entry.getValue();
            if (set == null || set.getAnimations() == null || keepsCreatureSound(id)) {
                continue;
            }
            ModelAsset.AnimationSet replaced = withoutCreatureSounds(set, soundsOf(playerSets == null ? null : playerSets.get(id)));
            if (replaced != set) {
                if (quiet == null) {
                    quiet = new HashMap<>(sets);
                }
                quiet.put(id, replaced);
            }
        }
        return quiet == null ? sets : quiet;
    }

    private static boolean keepsCreatureSound(@Nonnull String animationSetId) {
        for (String prefix : KEPT_SET_PREFIXES) {
            if (animationSetId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    private static Set<String> soundsOf(@Nullable ModelAsset.AnimationSet set) {
        Set<String> sounds = new HashSet<>();
        if (set != null && set.getAnimations() != null) {
            for (ModelAsset.Animation animation : set.getAnimations()) {
                if (animation != null && animation.getSoundEventId() != null) {
                    sounds.add(animation.getSoundEventId());
                }
            }
        }
        return sounds;
    }

    @Nonnull
    private static ModelAsset.AnimationSet withoutCreatureSounds(@Nonnull ModelAsset.AnimationSet set, @Nonnull Set<String> playerSounds) {
        ModelAsset.Animation[] animations = set.getAnimations();
        ModelAsset.Animation[] quiet = null;
        for (int i = 0; i < animations.length; i++) {
            ModelAsset.Animation animation = animations[i];
            if (animation == null || animation.getSoundEventId() == null || playerSounds.contains(animation.getSoundEventId())) {
                continue;
            }
            if (quiet == null) {
                quiet = animations.clone();
            }
            quiet[i] = new SilentAnimation(animation);
        }
        return quiet == null ? set : new ModelAsset.AnimationSet(quiet, set.getNextAnimationDelay());
    }

    /** The same animation with no sound on it. */
    private static final class SilentAnimation extends ModelAsset.Animation {
        SilentAnimation(@Nonnull ModelAsset.Animation source) {
            // The packet is the only public view of the footstep intervals and the passive loop count.
            com.hypixel.hytale.protocol.Animation packet = source.toPacket();
            this.animation = source.getAnimation();
            this.speed = source.getSpeed();
            this.blendingDuration = source.getBlendingDuration();
            this.looping = source.isLooping();
            this.weight = (float) source.getWeight();
            this.footstepIntervals = packet.footstepIntervals;
            this.passiveLoopCount = packet.passiveLoopCount;
            // No sound id and sound index 0, exactly as on an animation that never named a sound.
        }
    }
}

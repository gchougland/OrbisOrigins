package com.hexvane.orbisorigins.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import javax.annotation.Nonnull;

/**
 * Mod configuration loaded from the plugin data directory (config.json).
 * Defaults preserve backward compatibility: both options are true.
 */
public final class OrbisOriginsConfig {

    @Nonnull
    public static final BuilderCodec<OrbisOriginsConfig> CODEC = BuilderCodec.builder(
            OrbisOriginsConfig.class,
            OrbisOriginsConfig::new
        )
        .append(
            new KeyedCodec<>("GiveSpeciesSelectorOnFirstJoin", Codec.BOOLEAN),
            (c, v) -> c.giveSpeciesSelectorOnFirstJoin = v,
            c -> c.giveSpeciesSelectorOnFirstJoin
        )
        .add()
        .append(
            new KeyedCodec<>("AllowCraftingSpeciesSelector", Codec.BOOLEAN),
            (c, v) -> c.allowCraftingSpeciesSelector = v,
            c -> c.allowCraftingSpeciesSelector
        )
        .add()
        .build();

    private boolean giveSpeciesSelectorOnFirstJoin = true;
    private boolean allowCraftingSpeciesSelector = true;

    public boolean isGiveSpeciesSelectorOnFirstJoin() {
        return giveSpeciesSelectorOnFirstJoin;
    }

    public boolean isAllowCraftingSpeciesSelector() {
        return allowCraftingSpeciesSelector;
    }
}

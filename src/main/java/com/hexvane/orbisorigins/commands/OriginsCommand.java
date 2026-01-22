package com.hexvane.orbisorigins.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hexvane.orbisorigins.OrbisOriginsPlugin;
import javax.annotation.Nonnull;

/**
 * Parent command for Orbis Origins commands.
 * Usage: /origins [subcommand]
 */
public class OriginsCommand extends AbstractCommandCollection {
    public OriginsCommand(@Nonnull OrbisOriginsPlugin plugin) {
        super("origins", "Orbis Origins commands");
        this.addSubCommand(new OriginsReloadCommand(plugin));
    }
}

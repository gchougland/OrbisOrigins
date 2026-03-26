package com.hexvane.orbisorigins.util;

import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.console.ConsoleSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

/**
 * Runs species JSON command lines through {@link CommandManager} as {@link ConsoleSender} (full permissions),
 * not as the selecting player.
 */
public final class SpeciesCommandUtil {
    private static final Logger LOGGER = Logger.getLogger(SpeciesCommandUtil.class.getName());

    private SpeciesCommandUtil() {}

    /**
     * Runs each non-blank line in order with console-level permissions.
     * Leading {@code /} is stripped.
     * Placeholders {@code {player}} and {@code {username}} are replaced with the selecting player's username
     * (use in commands that need an explicit target, e.g. {@code give {player} SomeItem}).
     * Failures are logged; execution continues with the next line.
     */
    public static void runSpeciesCommands(@Nonnull Player selectingPlayer, @Nonnull List<String> commands) {
        if (commands.isEmpty()) {
            return;
        }
        String username = selectingPlayer.getDisplayName();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (String raw : commands) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String line = expandPlaceholders(raw, username);
            if (line.startsWith("/")) {
                line = line.substring(1);
            }
            if (line.isEmpty()) {
                continue;
            }
            final String commandLine = line;
            chain = chain.thenCompose(v -> CommandManager.get()
                    .handleCommand(ConsoleSender.INSTANCE, commandLine)
                    .exceptionally(ex -> {
                        LOGGER.log(Level.WARNING, "Species command failed for " + username + ": " + commandLine, ex);
                        return null;
                    }));
        }
        chain.join();
    }

    @Nonnull
    private static String expandPlaceholders(@Nonnull String raw, @Nonnull String username) {
        return raw.replace("{player}", username).replace("{username}", username);
    }
}

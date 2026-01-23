package com.hexvane.orbisorigins.util;

import com.hypixel.hytale.server.core.universe.world.World;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorldUtil {
    private static final Pattern uuidPattern = Pattern.compile("(^instance-(.*))-([0-9a-fA-F]{8}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{4}\\b-[0-9a-fA-F]{12}$)");

    public static String getNormalizedName(World world){
        return getNormalizedName(world.getName());
    }

    public static String getNormalizedName(String worldName){
        Matcher match = uuidPattern.matcher(worldName);
        return match.find() ? match.group(1) : worldName;
    }
}
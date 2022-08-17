package com.finallion.graveyard.util;


import com.finallion.graveyard.TheGraveyard;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v1.ConventionalBiomeTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.biome.Biome;

import java.util.List;

public class BiomeSelectionUtil {

    // structures
    public static boolean parseBiomes(List<String> biomeWhitelist, List<String> modIdWhitelist, RegistryEntry<Biome> biome) {
        if (biomeWhitelist == null || modIdWhitelist == null) {
            TheGraveyard.LOGGER.error("The Graveyard config file (\"the-graveyard-config.json5\") isn't up to date. Please delete the file in your .minecraft/config folder and restart the game to create a new config file. If the error keeps showing up, contact the mod developer via Github or Discord (links can be found here: https://www.curseforge.com/minecraft/mc-mods/the-graveyard-fabric)!");
            return false;
        }

        String biomeName = biome.getKey().get().getValue().toString();
        String modId = biome.getKey().get().getValue().getNamespace();

        // mod is whitelisted, weights higher than biome whitelist
        //if (modIdWhitelist.contains("#" + modId)) {
        //    return true;
        //}

        for (String biomeInList : biomeWhitelist) {
            // tag whitelist
            if (biomeInList.startsWith("c:")) {
                TagKey<Biome> tag = TagKey.of(Registry.BIOME_KEY, new Identifier("c", biomeInList.substring(2)));
                if (BuiltinRegistries.BIOME.getOrCreateEntryList(tag).contains(biome)) {
                    return true;
                }
            } else if (biomeWhitelist.contains(biomeName)) { // biome is whitelisted
                return true;
            }
        }

        return false;
    }

    // mobs
    public static boolean parseBiomes(List<String> biomeWhitelist, List<String> biomeBlacklist, List<String> modIdWhitelist, RegistryEntry<Biome> biome) {
        if (biomeWhitelist == null || modIdWhitelist == null || biomeBlacklist == null) {
            TheGraveyard.LOGGER.error("The Graveyard config file (\"the-graveyard-config.json5\") isn't up to date. Please delete the file in your .minecraft/config folder and restart the game to create a new config file. If the error keeps showing up, contact the mod developer via Github or Discord (links can be found here: https://www.curseforge.com/minecraft/mc-mods/the-graveyard-fabric)!");
            return false;
        }

        String biomeName = biome.getKey().get().getValue().toString();
        String modId = biome.getKey().get().getValue().getNamespace();

        // mod is whitelisted and not on the blacklist
        //if (modIdWhitelist.contains("#" + modId) && !biomeBlacklist.contains(biomeName)) {
        //    return true;
        //}

        for (String biomeInList : biomeWhitelist) {
            // tag whitelist
            if (biomeInList.startsWith("c:")) {
                TagKey<Biome> tag = TagKey.of(Registry.BIOME_KEY, new Identifier("c", biomeInList.substring(2)));
                if (BuiltinRegistries.BIOME.getEntryList(tag).orElseThrow().contains(biome) && !biomeBlacklist.contains(biomeName)) {
                    return true;
                }
            }
        }

        // biome is whitelisted and not on the blacklist
        if (biomeWhitelist.contains(biomeName) && !biomeBlacklist.contains(biomeName)) {
            return true;
        }

        return false;
    }

}


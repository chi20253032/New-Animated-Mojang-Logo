package com.bouncingelf10.animatedLogo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class AnimatedLogoConfig {
    public static final AnimatedLogoConfig INSTANCE = load();

    public float svgScale = 1.0f;
    public float animationSpeed = 1.0f;
    public float glowIntensity = 0.5f;
    public float soundVolume = 1.0f;

    public static AnimatedLogoConfig load() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("animated_logo.json").toFile();
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                return new Gson().fromJson(reader, AnimatedLogoConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new AnimatedLogoConfig();
    }

    public void save() {
        File configFile = FabricLoader.getInstance().getConfigDir().resolve("animated_logo.json").toFile();
        try (FileWriter writer = new FileWriter(configFile)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

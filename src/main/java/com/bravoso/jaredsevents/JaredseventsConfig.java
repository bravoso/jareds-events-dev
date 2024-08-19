package com.bravoso.jaredsevents;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JaredseventsConfig {

    private static final Gson GSON = new Gson();
    private static final String CONFIG_FILE_NAME = "jaredsevents_config.json";
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), CONFIG_FILE_NAME);

    // Initialize with default values
    public int eventDuration = 200;  // Default to 60 seconds (1200 ticks)
    public int cooldownDuration = 100;  // Default to 30 seconds (600 ticks)

    public void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                eventDuration = json.get("eventDuration").getAsInt();
                cooldownDuration = json.get("cooldownDuration").getAsInt();
            } catch (IOException | JsonParseException e) {
                System.err.println("Failed to load config file: " + e.getMessage());
            }
        } else {
            saveDefaultConfig();  // Save the default config if the file does not exist
        }
    }

    private void saveDefaultConfig() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("eventDuration", eventDuration);
            json.addProperty("cooldownDuration", cooldownDuration);

            Files.createDirectories(CONFIG_FILE.getParentFile().toPath());
            Files.write(CONFIG_FILE.toPath(), GSON.toJson(json).getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save default config file: " + e.getMessage());
        }
    }

    public int getEventDuration() {
        return eventDuration;
    }

    public int getCooldownDuration() {
        return cooldownDuration;
    }
}

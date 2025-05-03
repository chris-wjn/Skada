package com.cwjn.skada.mixin;

import com.cwjn.skada.ClientConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

public class SkadaMixinPlugin implements IMixinConfigPlugin {

    private static final String CONFIG_PATH = "config/skada-mixin-config.json";
    private boolean loadCustomReticlesMixins = true;

    @Override
    public void onLoad(String s) {
        loadConfig();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals("com.cwjn.skada.mixin.new_features.CustomReticlesDetectEntities") ||
            mixinClassName.equals("com.cwjn.skada.mixin.new_features.CustomReticlesHitDetectedEntities")) {
            return loadCustomReticlesMixins;
        }
        else return true;
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        if (!configFile.exists()) {
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject defaultConfig = new JsonObject();
                defaultConfig.addProperty("loadCustomReticlesMixins", true);
                writer.write(defaultConfig.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();
                loadCustomReticlesMixins = config.get("loadCustomReticlesMixins").getAsBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void acceptTargets(Set<String> set, Set<String> set1) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }

    @Override
    public void postApply(String s, ClassNode classNode, String s1, IMixinInfo iMixinInfo) {

    }
}

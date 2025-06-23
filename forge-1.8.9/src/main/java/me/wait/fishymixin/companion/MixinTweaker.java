package me.wait.fishymixin.companion;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.util.List;
import java.util.Collections;

public class MixinTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        MixinBootstrap.init();
        MixinEnvironment.setCompatibilityLevel(MixinEnvironment.CompatibilityLevel.JAVA_8);
        
        Mixins.addConfiguration("mixins.fishyaddons.json");
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addClassLoaderExclusion("me.wait.fishymixin.shadowed.");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}

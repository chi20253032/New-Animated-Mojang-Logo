package com.bouncingelf10.animatedLogo.mixin;

import com.bouncingelf10.animatedLogo.AnimatedLogoConfig;
import com.bouncingelf10.animatedLogo.AnimationState;
import com.bouncingelf10.animatedLogo.SvgRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.InputUtil;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashOverlay.class)
public class SplashOverlayMixin {

    @Unique
    private static final SvgRenderer svgRenderer = new SvgRenderer();
    @Unique
    private final AnimationState animationState = new AnimationState();
    @Unique
    private boolean soundPlayed = false;

    @org.spongepowered.asm.mixin.Shadow
    private net.minecraft.resource.ResourceReload reload;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        long currentTime = System.currentTimeMillis();

        svgRenderer.initialize();

        // Input Handling: Skip on ESC
        if (InputUtil.isKeyPressed(client.getWindow().getHandle(), GLFW.GLFW_KEY_ESCAPE)) {
            client.setOverlay(null);
            ci.cancel();
            return;
        }

        // Update Animation
        float progress = 0.0f;
        if (this.reload != null) {
            progress = this.reload.getProgress();
        }
        animationState.update(currentTime, progress);
        
        // Sound Sync (e.g., at 500ms)
        if (!soundPlayed && AnimatedLogoConfig.INSTANCE.soundVolume > 0) {
            // Rough check for timing, say start immediately or delayed
            // Play at start
            client.getSoundManager().play(PositionedSoundInstance.master(SoundEvent.of(Identifier.of("animated_mojang_logo", "jingle")), 1.0f, AnimatedLogoConfig.INSTANCE.soundVolume));
            soundPlayed = true;
        }
        
        // Render Background (Black or configured color from DarkLoadingScreen if compatible)
        context.fill(0, 0, width, height, 0xFF000000); // ARGB Black

        // Render SVG
        svgRenderer.render(context, width, height, animationState);

        // Cancel vanilla rendering
        ci.cancel();
    }
}

package com.bouncingelf10.animatedLogo;

import net.minecraft.util.math.MathHelper;

public class AnimationState {
    private long startTime = -1;
    private final long DURATION_MS = 3000;
    
    public float currentAlpha = 0f;
    public float currentScale = 1f;
    public float currentGlow = 0f;
    
    public boolean scaleChanged = true; // To trigger re-rasterization if scaling strategy is dynamic

    public void update(long currentTime, float loadProgress) {
        if (startTime == -1) {
            startTime = currentTime;
        }

        long elapsed = currentTime - startTime;
        
        // Alpha: Ease In Out
        float alphaProgress = MathHelper.clamp(elapsed / 1000f, 0f, 1f);
        currentAlpha = easeInOutSine(alphaProgress);

        // Scale: Zoom from center
        float targetScale = AnimatedLogoConfig.INSTANCE.svgScale;
        float startScale = targetScale * 0.5f;
        float ease = easeOutBack(MathHelper.clamp(elapsed / (float) DURATION_MS, 0f, 1f));
        currentScale = MathHelper.lerp(ease, startScale, targetScale);
        
        // Glow: Based on load progress (0.0 to 1.0)
        // We pulse it slightly but increase intensity as it finishes.
        // Or just map progress to intensity.
        // Let's make it more intense as it reaches 100%.
        // Base glow from breathing + progress.
        float breathing = (float) Math.sin(currentTime / 500.0) * 0.2f + 0.8f;
        currentGlow = loadProgress * breathing * AnimatedLogoConfig.INSTANCE.glowIntensity;
    }

    private float easeInOutSine(float x) {
        return -(MathHelper.cos((float) (Math.PI * x)) - 1) / 2;
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }
}

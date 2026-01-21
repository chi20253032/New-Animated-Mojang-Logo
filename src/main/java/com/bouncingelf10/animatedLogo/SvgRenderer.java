package com.bouncingelf10.animatedLogo;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.DynamicTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SvgRenderer {
    private static final Identifier LOGO_ID = Identifier.of("animated_mojang_logo", "logo");
    private static final Identifier SVG_PATH = Identifier.of("minecraft", "textures/gui/title/mojang_logo.svg");

    private SVGDocument svgDocument;
    private DynamicTexture glowTexture;
    private Identifier glowTextureId;

    public void render(DrawContext context, int screenWidth, int screenHeight, AnimationState state) {
        if (svgDocument == null) return;

        float scale = state.currentScale;
        int targetWidth = (int) (screenWidth * scale); 
        if (targetWidth < 1) targetWidth = 1;
        
        float aspectRatio = (float) svgDocument.size().width() / (float) svgDocument.size().height();
        int targetHeight = (int) (targetWidth / aspectRatio);
        if (targetHeight < 1) targetHeight = 1;

        if (texture == null || Math.abs(currentWidth - targetWidth) > targetWidth * 0.1 || Math.abs(currentHeight - targetHeight) > targetHeight * 0.1) {
            rasterize(targetWidth, targetHeight);
        }

        if (glowTextureId != null && state.currentGlow > 0.01f) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // Glow Color - let's assume white/gold based on user preference or just white standard glow
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, state.currentGlow);
            
            int drawX = (screenWidth - targetWidth) / 2;
            int drawY = (screenHeight - targetHeight) / 2;
            
            // Draw Glow slightly larger? Or same size if it's an outer glow contained in the image?
            // If I generate glow by blurring, the image effectively spreads.
            // If my rasterization includes padding for glow, I need to account for it.
            // For simplicity, I'll draw it at same rect.
            context.drawTexture(RenderSystem::getShaderTexture, glowTextureId, drawX, drawY, 0, 0, targetWidth, targetHeight, targetWidth, targetHeight);
        }

        if (textureId != null) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, state.currentAlpha);
            
            int drawX = (screenWidth - targetWidth) / 2;
            int drawY = (screenHeight - targetHeight) / 2;
            
            context.drawTexture(RenderSystem::getShaderTexture, textureId, drawX, drawY, 0, 0, targetWidth, targetHeight, targetWidth, targetHeight);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private void rasterize(int width, int height) {
        if (width <= 0 || height <= 0) return;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        double scaleX = (double) width / svgDocument.size().width();
        double scaleY = (double) height / svgDocument.size().height();
        g.scale(scaleX, scaleY);
        
        svgDocument.render(null, g);
        g.dispose();

        // Create Glow Image (Blur)
        // We use a scaled down version for performance? Or same size?
        // Same size for quality.
        BufferedImage glowImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Simple Box Blur or Convolution
        // 5x5 Blur
        float[] matrix = new float[400];
        for (int i = 0; i < 400; i++) matrix[i] = 1.0f / 400.0f;
        java.awt.image.Kernel kernel = new java.awt.image.Kernel(20, 20, matrix); // 20x20 blur for strong glow
        java.awt.image.ConvolveOp op = new java.awt.image.ConvolveOp(kernel, java.awt.image.ConvolveOp.EDGE_NO_OP, null);
        op.filter(image, glowImage);
        
        // Upload Main Texture
        uploadTexture(image, false);
        // Upload Glow Texture
        uploadTexture(glowImage, true);
        
        currentWidth = width;
        currentHeight = height;
    }
    
    private void uploadTexture(BufferedImage image, boolean isGlow) {
        int width = image.getWidth();
        int height = image.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                int red = (argb >> 16) & 0xFF;
                int green = (argb >> 8) & 0xFF;
                int blue = (argb) & 0xFF;
                int abgr = (alpha << 24) | (blue << 16) | (green << 8) | red;
                nativeImage.setColor(x, y, abgr);
            }
        }
        
        DynamicTexture dt = new DynamicTexture(nativeImage);
        if (isGlow) {
            if (glowTexture != null) glowTexture.close();
            glowTexture = dt;
            Identifier id = Identifier.of("animated_mojang_logo", "logo_glow_dynamic");
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, glowTexture);
            glowTextureId = id;
        } else {
            if (texture != null) texture.close();
            texture = dt;
            Identifier id = Identifier.of("animated_mojang_logo", "logo_dynamic");
            MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
            textureId = id;
        }
    }

    public void initialize() {
        if (svgDocument != null) return;
        Optional<Resource> resource = MinecraftClient.getInstance().getResourceManager().getResource(SVG_PATH);
        if (resource.isPresent()) {
            try (InputStream is = resource.get().getInputStream()) {
                svgDocument = new SVGLoader().load(is);
            } catch (IOException e) {
                // Assuming a logger exists, or print to console for now
                System.err.println("Failed to load SVG: " + SVG_PATH + " - " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("SVG resource not found: " + SVG_PATH);
        }
    }
}

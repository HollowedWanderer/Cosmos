package net.hollowed.cosmos.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.hollowed.cosmos.Cosmos;
import net.hollowed.cosmos.config.CosmosConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import org.joml.*;
import org.spongepowered.asm.mixin.*;

import java.lang.Math;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static net.minecraft.client.gl.RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET;

@Mixin(SkyRendering.class)
public class SkyRenderingMixin {

    @Unique
    private static final RenderPipeline POSITION_TEXTURE_COLOR_STARS = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/stars")
                    .withVertexShader("core/stars")
                    .withFragmentShader("core/stars")
                    .withBlend(BlendFunction.OVERLAY)
                    .withDepthWrite(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    @Shadow private int starIndexCount;

    @Shadow @Final private RenderSystem.ShapeIndexBuffer indexBuffer;

    @Shadow @Final private GpuBuffer starVertexBuffer;

    /**
     * @author Hollowed
     * @reason I really don't see how this is an issue, if you have an issue with this, please put it on the GitHub and/or make a PR
     */
    @Overwrite
    private GpuBuffer createStars() {
        Random random = Random.create(12936L);

        GpuBuffer var19;
        try (BufferAllocator bufferAllocator = BufferAllocator.method_72201(VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize() * CosmosConfig.starCount * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            boolean northStar = false;
            int limit = CosmosConfig.northStar ? 1 : 0;

            for (int i = 0; i < CosmosConfig.starCount; i++) {
                float g = random.nextFloat() * 2.0F - 1.0F;
                float h = random.nextFloat() * 2.0F - 1.0F;
                float j = random.nextFloat() * 2.0F - 1.0F;
                float k = (CosmosConfig.sizeRange.getFirst() + random.nextFloat() * (CosmosConfig.sizeRange.get(1) - CosmosConfig.sizeRange.getFirst()));

                if (g < -0.48F && h < -0.23F && h > -0.32F && Math.abs(j) < 0.07F && limit > 0) {
                    k = 1.3F;
                    northStar = true;
                    limit--;
                }

                float l = MathHelper.magnitude(g, h, j);
                if (!(l <= 0.010000001F) && !(l >= 1.0F)) {
                    Vector3f vector3f = new Vector3f(g, h, j).normalize(100.0F);
                    float m = (float)(random.nextDouble() * (float) Math.PI * 2.0);
                    Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-m);

                    int alpha = CosmosConfig.alphaRange.getFirst() + random.nextInt(CosmosConfig.alphaRange.get(1) - CosmosConfig.alphaRange.getFirst());

                    List<Integer> colorEntry = CosmosConfig.colors.get(random.nextInt(CosmosConfig.colors.size()));
                    int color = ColorHelper.getArgb(alpha, colorEntry.getFirst(), colorEntry.get(1), colorEntry.get(2));

                    if (northStar) {
                        color = ColorHelper.getArgb(255,255, 255, 255);
                        northStar = false;
                    }

                    bufferBuilder.vertex(new Vector3f(k, -k, 0.0F).mul(matrix3f).add(vector3f)).texture(0.0F, 0.0F).color(color);
                    bufferBuilder.vertex(new Vector3f(k, k, 0.0F).mul(matrix3f).add(vector3f)).texture(0.0F, 1.0F).color(color);
                    bufferBuilder.vertex(new Vector3f(-k, k, 0.0F).mul(matrix3f).add(vector3f)).texture(1.0F, 1.0F).color(color);
                    bufferBuilder.vertex(new Vector3f(-k, -k, 0.0F).mul(matrix3f).add(vector3f)).texture(1.0F, 0.0F).color(color);
                }
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                this.starIndexCount = builtBuffer.getDrawParameters().indexCount();
                var19 = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, builtBuffer.getBuffer());
            }
        }

        return var19;
    }

    /**
     * @author Hollowed
     * @reason I really don't see how this is an issue for 99% of mods, if you have an issue with this, please put it on the GitHub and/or make a PR
     */
    @Overwrite
    private void renderStars(float brightness, MatrixStack matrices) {
        if (MinecraftClient.getInstance().world != null) {
            brightness *= CosmosConfig.brightnessMultiplier;

            Identifier starTextureId = Cosmos.id("textures/environment/star.png");
            TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
            textureManager.registerTexture(starTextureId, new ResourceTexture(starTextureId));
            AbstractTexture abstractTexture = textureManager.getTexture(starTextureId);

            abstractTexture.setUseMipmaps(false);
            abstractTexture.setFilter(false, false);
            Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
            matrix4fStack.pushMatrix();
            matrix4fStack.mul(matrices.peek().getPositionMatrix());
            GpuTextureView gpuTextureView = MinecraftClient.getInstance().getFramebuffer().getColorAttachmentView();
            GpuTextureView gpuTextureView2 = MinecraftClient.getInstance().getFramebuffer().getDepthAttachmentView();
            GpuBuffer gpuBuffer = this.indexBuffer.getIndexBuffer(this.starIndexCount);
            float time = MinecraftClient.getInstance().world.getTime() % 1200 / 20.0F;

            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                    .write(matrix4fStack, new Vector4f(brightness, CosmosConfig.twinkleFrequency.getFirst(), CosmosConfig.twinkleFrequency.get(1), brightness), new Vector3f(), new Matrix4f(), time);

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(() -> "Stars", gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(POSITION_TEXTURE_COLOR_STARS);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.bindSampler("Sampler0", abstractTexture.getGlTextureView());
                renderPass.setVertexBuffer(0, this.starVertexBuffer);
                renderPass.setIndexBuffer(gpuBuffer, this.indexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, this.starIndexCount, 1);
            }

            matrix4fStack.popMatrix();
        }
    }
}

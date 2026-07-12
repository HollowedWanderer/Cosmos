package net.hollowed.cosmos.mixin;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import net.hollowed.cosmos.Cosmos;
import net.hollowed.cosmos.config.CosmosConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.Math;
import java.util.Optional;
import java.util.OptionalDouble;

import static net.minecraft.client.renderer.RenderPipelines.GLOBALS_SNIPPET;

@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {

    @Shadow
    @Final
    private RenderTarget renderTarget;
    @Shadow
    @Final
    private RenderSystem.AutoStorageIndexBuffer quadIndices;

    @Shadow
    @Final
    private TextureAtlas celestialsAtlas;
    @Unique
    private static final RenderPipeline COSMOS_STARS = RenderPipelines.register(
            RenderPipeline.builder(GLOBALS_SNIPPET)
                    .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                    .withLocation(Cosmos.id("pipeline/stars"))
                    .withVertexShader(Cosmos.id("core/stars"))
                    .withFragmentShader(Cosmos.id("core/stars"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.OVERLAY))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .build()
    );


    @Unique
    private int cosmosStarIndexCount;

    @Unique
    private GpuBuffer cosmosStarVertexBuffer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(TextureManager textureManager, AtlasManager atlasManager, RenderTarget renderTarget, CallbackInfo ci) {
        cosmosStarVertexBuffer = createCosmosStars(this.celestialsAtlas);
    }

    @Unique
    private GpuBuffer createCosmosStars(TextureAtlas atlas) {
        RandomSource random = RandomSource.createThreadLocalInstance(10842L);

        GpuBuffer var19;
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * CosmosConfig.starCount * 4)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            TextureAtlasSprite sprite = atlas.getSprite(Cosmos.id("star"));

            boolean northStar = false;
            int limit = CosmosConfig.northStar ? 1 : 0;

            for (int i = 0; i < CosmosConfig.starCount; i++) {
                float x = random.nextFloat() * 2.0F - 1.0F;
                float y = random.nextFloat() * 2.0F - 1.0F;
                float z = random.nextFloat() * 2.0F - 1.0F;
                float starSize = (CosmosConfig.sizeRange.getFirst().floatValue() + random.nextFloat() * (CosmosConfig.sizeRange.get(1).floatValue() - CosmosConfig.sizeRange.getFirst().floatValue()));

                if (x < -0.48F && y < -0.23F && y > -0.32F && Math.abs(z) < 0.07F && limit > 0) {
                    starSize *= 2;
                    northStar = true;
                    limit--;
                }

                float lengthSq = Mth.lengthSquared(x, y, z);
                if (!(lengthSq <= 0.010000001F) && !(lengthSq >= 1.0F)) {
                    Vector3f starCenter = (new Vector3f(x, y, z)).normalize(100.0F);
                    float zRot = (float)(random.nextDouble() * (double)(float)Math.PI * (double)2.0F);
                    Matrix3f rotation = (new Matrix3f()).rotateTowards((new Vector3f(starCenter)).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-zRot);

                    int alpha = CosmosConfig.alphaRange.getFirst() + random.nextInt(CosmosConfig.alphaRange.get(1) - CosmosConfig.alphaRange.getFirst());

                    String colorString = CosmosConfig.colors.get(random.nextInt(CosmosConfig.colors.size()));
                    int[] colorEntry = hexToRGB(colorString);

                    int color = ARGB.color(alpha, colorEntry[0], colorEntry[1], colorEntry[2]);

                    if (northStar) {
                        color = ARGB.color(255,255, 255, 255);
                        northStar = false;
                    }

                    bufferBuilder.addVertex(new Vector3f(starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU0(), sprite.getV0()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU0(), sprite.getV1()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(-starSize, starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU1(), sprite.getV1()).setColor(color);
                    bufferBuilder.addVertex(new Vector3f(-starSize, -starSize, 0.0F).mul(rotation).add(starCenter)).setUv(sprite.getU1(), sprite.getV0()).setColor(color);
                }
            }

            try (MeshData mesh = bufferBuilder.buildOrThrow()) {
                this.cosmosStarIndexCount = mesh.drawState().indexCount();
                var19 = RenderSystem.getDevice().createBuffer(() -> "Stars vertex buffer", 40, mesh.vertexBuffer());
            }
        }

        return var19;
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void renderStars(float starBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (CosmosConfig.enabled) {
            if (Minecraft.getInstance().level != null) {
                starBrightness *= CosmosConfig.brightnessMultiplier;

                Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
                matrix4fStack.pushMatrix();
                matrix4fStack.mul(poseStack.last().pose());
                GpuTextureView color = this.renderTarget.getColorTextureView();
                GpuTextureView depth = this.renderTarget.getDepthTextureView();
                GpuBuffer gpuBuffer = this.quadIndices.getBuffer(this.cosmosStarIndexCount);
                float time = Minecraft.getInstance().level.getGameTime() % 24000 / 20.0F;

                GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                        .writeTransform(matrix4fStack, new Vector4f(starBrightness, CosmosConfig.twinkleFrequency.getFirst().floatValue(), CosmosConfig.twinkleFrequency.get(1).floatValue(), time));

                if (color != null) {
                    try (RenderPass renderPass = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "Stars", color, Optional.empty(), depth, OptionalDouble.empty())) {
                        renderPass.setPipeline(COSMOS_STARS);
                        RenderSystem.bindDefaultUniforms(renderPass);
                        renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                        renderPass.bindTexture("Sampler0", this.celestialsAtlas.getTextureView(), this.celestialsAtlas.getSampler());
                        renderPass.setVertexBuffer(0, this.cosmosStarVertexBuffer.slice());
                        renderPass.setIndexBuffer(gpuBuffer, this.quadIndices.type());
                        renderPass.drawIndexed(this.cosmosStarIndexCount, 1, 0, 0, 0);
                    }
                }

                matrix4fStack.popMatrix();
            }
            ci.cancel();
        }
    }

    @Unique
    private static int[] hexToRGB(String hexColor) {
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }

        int red = Integer.parseInt(hexColor.substring(0, 2), 16);
        int green = Integer.parseInt(hexColor.substring(2, 4), 16);
        int blue = Integer.parseInt(hexColor.substring(4, 6), 16);

        return new int[] { red, green, blue };
    }

}

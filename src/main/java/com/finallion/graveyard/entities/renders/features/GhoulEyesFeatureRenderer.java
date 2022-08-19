package com.finallion.graveyard.entities.renders.features;


import com.finallion.graveyard.TheGraveyard;
import com.finallion.graveyard.entities.GhoulEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib3.renderers.geo.GeoLayerRenderer;
import software.bernie.geckolib3.renderers.geo.IGeoRenderer;


public class GhoulEyesFeatureRenderer extends GeoLayerRenderer<GhoulEntity> {
    private RenderLayer TEXTURE;
    private final IGeoRenderer<GhoulEntity> renderer;

    public GhoulEyesFeatureRenderer(IGeoRenderer<GhoulEntity> entityRendererIn) {
        super(entityRendererIn);
        this.renderer = entityRendererIn;
    }

    @Override
    public void render(MatrixStack matrixStackIn, VertexConsumerProvider bufferIn, int packedLightIn, GhoulEntity entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if (entitylivingbaseIn.getVariant() == 10) {
            TEXTURE = RenderLayer.getEyes(new Identifier(TheGraveyard.MOD_ID, "textures/entity/ghoul_eyes_skin9.png"));
            VertexConsumer vertexConsumer = bufferIn.getBuffer(TEXTURE);

            renderer.render(
                    getEntityModel().getModel(getEntityModel().getModelResource(entitylivingbaseIn)),
                    entitylivingbaseIn,
                    partialTicks,
                    TEXTURE,
                    matrixStackIn,
                    bufferIn,
                    vertexConsumer,
                    15728640,
                    OverlayTexture.DEFAULT_UV,
                    1.0F, 1.0F, 1.0F, 1.0F
            );
        }

    }


}


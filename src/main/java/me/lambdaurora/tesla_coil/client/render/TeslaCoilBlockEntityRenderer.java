/*
 * Copyright (c) 2020 LambdAurora <aurora42lambda@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lambdaurora.tesla_coil.client.render;

import me.lambdaurora.tesla_coil.block.entity.TeslaCoilBlockEntity;
import me.lambdaurora.tesla_coil.client.TeslaCoilClientMod;
import me.lambdaurora.tesla_coil.client.render.model.TeslaCoilEnergySwirlModel;
import me.lambdaurora.tesla_coil.mixin.client.RenderPhaseAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector4f;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3f;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the tesla coil block entity renderer.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@Environment(EnvType.CLIENT)
public class TeslaCoilBlockEntityRenderer implements BlockEntityRenderer<TeslaCoilBlockEntity>
{
    private static final Identifier ENERGY_SWIRL_TEXTURE = new Identifier("textures/entity/creeper/creeper_armor.png");
    private final ModelPart[] models = new ModelPart[3];

    public TeslaCoilBlockEntityRenderer(BlockEntityRendererFactory.Context context)
    {
        this.models[0] = TeslaCoilEnergySwirlModel.buildModel(1).createModel();
        this.models[1] = TeslaCoilEnergySwirlModel.buildModel(2).createModel();
        this.models[2] = TeslaCoilEnergySwirlModel.buildModel(3).createModel();
    }

    @Override
    public void render(TeslaCoilBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
    {
        if (!entity.isEnabled()) return;

        {
            float partialAge = entity.getAge() + tickDelta;
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getEnergySwirl(ENERGY_SWIRL_TEXTURE, this.getEnergySwirlX(partialAge), partialAge * 0.01f));
            this.models[entity.getPower() - 1].render(matrices, vertexConsumer, 15728880, OverlayTexture.DEFAULT_UV, 0.5f, 0.5f, 0.5f, 1.f);
        }

        if (entity.getSmallArcDirection() != null) {
            matrices.push();
            matrices.translate(0, 1 + entity.getPower(), 0);
            VertexConsumer vertexConsumer = vertexConsumers.getBuffer(TeslaCoilClientMod.SMALL_ELECTRIC_ARC_RENDER_LAYER);
            Matrix4f matrix = matrices.peek().getModel();
            this.renderSmallArc(vertexConsumer, matrix, entity.getSmallArcDirection());
            matrices.pop();
        }
    }

    protected void renderSmallArc(VertexConsumer vertexConsumer, Matrix4f matrix, Direction direction)
    {
        Vec3f unit = direction.getUnitVector();
        Vec3f normal = direction.rotateYCounterclockwise().getUnitVector();

        float x1 = 0.5f;
        float z1 = 0.5f;

        float x2 = x1 + unit.getX();
        float z2 = z1 + unit.getZ();

        float[] vertices = {
                x1, 1.f, z1, 0.f, 0.f,
                x1, 0.f, z1, 0.f, 1.f,
                x2, 0.f, z2, 1.f, 1.f,
                x2, 1.f, z2, 1.f, 0.f
        };

        for (int i = 0; i < 4; i++) {
            int start = i * 5;
            Vector4f vec = new Vector4f(vertices[start], vertices[start + 1], vertices[start + 2], 1.f);
            vec.transform(matrix);

            vertexConsumer.vertex(vec.getX(), vec.getY(), vec.getZ(),
                    1.f, 1.f, 1.f, 0.75f,
                    vertices[start + 3], vertices[start + 4],
                    OverlayTexture.DEFAULT_UV,
                    15728880,
                    normal.getX(), normal.getY(), normal.getZ());
        }
    }

    protected float getEnergySwirlX(float partialAge)
    {
        return partialAge * 0.01f;
    }

    public static RenderLayer getEnergySwirl(@NotNull Identifier texture, float x, float y)
    {
        return RenderLayer.of("tesla_coil_energy_swirl", VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.QUADS, 256, false, true,
                RenderLayer.MultiPhaseParameters.builder()
                        .texture(new RenderPhase.Texture(texture, false, false))
                        .texturing(new RenderPhase.OffsetTexturing(x, y))
                        .fog(RenderPhaseAccessor.getBlackFog())
                        .transparency(RenderPhaseAccessor.getAdditiveTransparency())
                        .diffuseLighting(RenderPhaseAccessor.getEnableDiffuseLighting())
                        .alpha(TeslaCoilClientMod.ELECTRIC_ARC_ALPHA)
                        .cull(RenderPhaseAccessor.getDisableCulling())
                        .lightmap(RenderPhaseAccessor.getEnableLightmap())
                        .overlay(RenderPhaseAccessor.getEnableOverlayColor())
                        .build(false));
    }
}

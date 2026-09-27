package com.tacz.guns.client.event;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.config.util.HeadShotAABBConfigRead;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.core.registries.BuiltInRegistries;

@EventBusSubscriber(value = Dist.CLIENT)
public class RenderHeadShotAABB {
    @SubscribeEvent
    public static void onRenderEntity(RenderLivingEvent.Post<?, ?> event) {
        boolean canRender = Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes();
        if (!canRender) {
            return;
        }
        if (!RenderConfig.HEAD_SHOT_DEBUG_HITBOX.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId == null) {
            return;
        }
        AABB aabb = HeadShotAABBConfigRead.getAABB(entityId);
        if (aabb == null) {
            float width = entity.getBbWidth();
            float eyeHeight = entity.getEyeHeight();
            // 扩张 0.01，避免和原版显示重合
            aabb = new AABB(-width / 2, eyeHeight - 0.25, -width / 2, width / 2, eyeHeight + 0.25, width / 2).inflate(0.01);
        }
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(event.getPoseStack(), buffer, aabb, 1.0F, 1.0F, 0.0F, 1.0F);
    }
}

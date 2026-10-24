package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.model.FaceUVsItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;


public class SlotModel extends EntityModel<Entity> {
    private final BedrockPart bone;

    public SlotModel(boolean illuminated) {
        bone = new BedrockPart("slot");
        bone.setPos(8.0F, 24.0F, -10.0F);
        bone.cubes.add(new BedrockCubePerFace(-16.0F, -16.0F, 9.5F, 16.0F, 16.0F, 0, 0, 16, 16, FaceUVsItem.singleSouthFace()));
        bone.illuminated = illuminated;
    }

    public SlotModel() {
        this(false);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        bone.render(poseStack, ItemDisplayContext.GUI, buffer, packedLight, packedOverlay);
    }

    /**
     * 1.21 的 Model 只剩带 int color 的 renderToBuffer，这里保留旧版
     * float 颜色分量重载给尚未迁移的调用方，颜色按 ARGB 打包后等价。
     */
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        this.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, FastColor.ARGB32.colorFromFloat(alpha, red, green, blue));
    }
}

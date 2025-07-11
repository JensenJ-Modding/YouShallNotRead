package net.youshallnotread.outline;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.youshallnotread.YouShallNotRead;

public class OutlineRenderer {

    public static void renderOutline(StandaloneOutline outline, PoseStack stack) {
        prepareRenderer();
        VertexBuffer buffer = outline.buffer();
        stack.pushPose();
        outline.transform(stack);
        renderBuffer(buffer, stack);
        cleanupRenderer();
        stack.popPose();
    }

    public static void renderBatchedOutlines(PoseStack stack) {
        prepareRenderer();
        stack.pushPose();
        applyGlobalTransform(stack);
        renderBuffer(BatchedVertexBuffer.buffer(), stack);
        cleanupRenderer();
        stack.popPose();
    }

    private static void applyGlobalTransform(PoseStack stack) {
        Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = cam.getPosition();

        stack.mulPose(Axis.XP.rotation((float) Math.toRadians(cam.getXRot())));
        stack.mulPose(Axis.YP.rotation((float) Math.toRadians(cam.getYRot())));
        stack.translate(cameraPos.x, -cameraPos.y, cameraPos.z);
        stack.mulPose(Axis.YP.rotation((float) Math.toRadians(180f)));
    }

    private static void prepareRenderer() {
        RenderSystem.assertOnRenderThread();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void cleanupRenderer() {
        RenderSystem.disableDepthTest();
    }

    private static void renderBuffer(VertexBuffer buffer, PoseStack stack) {
        if (buffer != null && !buffer.isInvalid()) {
            buffer.bind();
            buffer.drawWithShader(stack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        } else {
            YouShallNotRead.LOGGER.error("Outline renderer buffer invalid.");
        }
    }
}

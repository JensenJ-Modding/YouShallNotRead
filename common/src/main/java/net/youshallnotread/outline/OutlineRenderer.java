package net.youshallnotread.outline;

import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.youshallnotread.YouShallNotRead;

public class OutlineRenderer {

    public static void renderOutline(Outline outline, PoseStack stack) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        VertexBuffer buffer = outline.buffer();

        stack.pushPose();
        outline.transform(stack);

        if (buffer != null && !buffer.isInvalid()) {
            buffer.bind();
            buffer.drawWithShader(stack.last().pose(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        } else {
            YouShallNotRead.LOGGER.error("Outline renderer buffer invalid.");
        }
        RenderSystem.disableDepthTest();
        stack.popPose();
    }
}

package net.youshallnotread.outline;

import java.util.function.BiConsumer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class StandaloneOutline extends Outline {

    private VertexBuffer vertexBuffer;

    public StandaloneOutline(Builder builder) {
        super(builder);
    }

    @Override
    void setupVertexData() {
        RenderSystem.assertOnRenderThread();
        Tesselator tesselator = Tesselator.getInstance();

        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        BiConsumer<Vector3d, Vector4f> vertexConsumer =
                (position, colour) -> buffer.addVertex((float) position.x, (float) position.y, (float) position.z)
                        .setColor(colour.x, colour.y, colour.z, colour.w);

        OutlineMeshBuilder.buildMesh(this, vertexConsumer);

        vertexBuffer.bind();
        vertexBuffer.upload(buffer.build());
        VertexBuffer.unbind();
    }

    @Override
    boolean hasVertexData() {
        return vertexBuffer != null;
    }

    @Override
    void cleanup() {
        if (hasVertexData()) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }

    public VertexBuffer buffer() {
        return vertexBuffer;
    }
}

package net.youshallnotread.outline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.apache.commons.collections4.collection.CompositeCollection;
import org.joml.Vector3d;
import org.joml.Vector4f;

public class BatchedVertexBuffer {

    public record OutlineVertex(Vector3d position, Vector4f colour) {}

    private static VertexBuffer vertexBuffer;

    public static void populateVertexBuffer(CompositeCollection<OutlineVertex> vertices) {
        if (vertices.isEmpty()) return;
        if (hasVertexData()) return;

        RenderSystem.assertOnRenderThread();
        Tesselator tesselator = Tesselator.getInstance();

        vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (OutlineVertex vertex : vertices) {
            buffer.addVertex((float) vertex.position.x, (float) vertex.position.y, (float) vertex.position.z)
                    .setColor(vertex.colour.x, vertex.colour.y, vertex.colour.z, vertex.colour.w);
        }

        vertexBuffer.bind();
        vertexBuffer.upload(buffer.build());
        VertexBuffer.unbind();
    }

    public static boolean hasVertexData() {
        return vertexBuffer != null;
    }

    public static VertexBuffer buffer() {
        return vertexBuffer;
    }

    public static void cleanup() {
        if (hasVertexData()) {
            vertexBuffer.close();
            vertexBuffer = null;
        }
    }
}

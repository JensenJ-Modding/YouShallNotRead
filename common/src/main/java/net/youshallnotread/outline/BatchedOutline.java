package net.youshallnotread.outline;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import org.joml.Vector3d;
import org.joml.Vector3f;

public class BatchedOutline extends Outline {

    private final List<BatchedVertexBuffer.OutlineVertex> vertices = new ArrayList<>();

    public BatchedOutline(Builder builder) {
        super(builder);
    }

    @Override
    void setupVertexData() {
        super.setupVertexData();
        BiConsumer<Vector3d, Vector3f> vertexConsumer =
                (position, colour) -> vertices.add(new BatchedVertexBuffer.OutlineVertex(position, colour));

        OutlineMeshBuilder.buildMesh(this, vertexConsumer);
    }

    @Override
    void cleanup() {
        super.cleanup();
        vertices.clear();
    }

    public List<BatchedVertexBuffer.OutlineVertex> vertices() {
        return vertices;
    }
}

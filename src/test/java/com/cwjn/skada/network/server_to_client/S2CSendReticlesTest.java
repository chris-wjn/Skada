package com.cwjn.skada.network.server_to_client;

import com.cwjn.skada.client.hud.ReticleCoordinate;
import com.cwjn.skada.client.hud.ReticleShape;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Exercises the reticle packet's real wire codec without starting a Minecraft client. */
class S2CSendReticlesTest {

    private static final int SENTINEL = 0x5A17C0DE;

    @Test
    void emptyReticleListPreservesTrailingData() {
        assertRoundTrip(List.of());
    }

    @Test
    void oneReticlePreservesItsValuesAndTrailingData() {
        assertRoundTrip(List.of(shape("slash", 1.25F)));
    }

    @Test
    void multipleReticlesPreserveOrderValuesAndTrailingData() {
        assertRoundTrip(List.of(shape("slash", 1.25F), shape("thrust", 2.5F), shape("strike", 4.75F)));
    }

    @Test
    void truncatedCountFailsInsteadOfProducingAnEmptyPacket() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeShort(1);
            assertThrows(RuntimeException.class, () -> S2CSendReticles.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void missingLastReticleFailsInsteadOfProducingAPartialPacket() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeInt(2);
            buffer.writeJsonWithCodec(ReticleShape.CODEC, shape("slash", 1.25F));
            assertThrows(RuntimeException.class, () -> S2CSendReticles.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    @Test
    void truncatedReticleJsonFailsInsteadOfProducingAPartialPacket() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new S2CSendReticles(List.of(shape("slash", 1.25F))).encode(buffer);
            buffer.writerIndex(buffer.writerIndex() - 1);
            assertThrows(RuntimeException.class, () -> S2CSendReticles.decode(buffer));
        } finally {
            buffer.release();
        }
    }

    /** Verifies packet boundaries, then re-encodes the decoded packet to inspect its public wire output. */
    private static void assertRoundTrip(List<ReticleShape> expected) {
        FriendlyByteBuf source = new FriendlyByteBuf(Unpooled.buffer());
        FriendlyByteBuf result = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new S2CSendReticles(expected).encode(source);
            source.writeInt(SENTINEL);

            S2CSendReticles decoded = S2CSendReticles.decode(source);

            assertEquals(Integer.BYTES, source.readableBytes(), "Decoder must consume only its own payload");
            assertEquals(SENTINEL, source.readInt());
            decoded.encode(result);
            assertEquals(expected.size(), result.readInt());
            for (ReticleShape expectedShape : expected) {
                ReticleShape actualShape = result.readJsonWithCodec(ReticleShape.CODEC);
                assertEquals(expectedShape.getName(), actualShape.getName());
                assertEquals(outlineVertices(expectedShape), outlineVertices(actualShape));
            }
            assertEquals(0, result.readableBytes());
        } finally {
            source.release();
            result.release();
        }
    }

    /** Creates distinct noninteger coordinates so the round trip checks geometry as well as names. */
    private static ReticleShape shape(String name, float size) {
        return new ReticleShape(name, List.of(
                new ReticleCoordinate(0, -size, -size),
                new ReticleCoordinate(1, size, -size),
                new ReticleCoordinate(2, 0.5F, size)));
    }

    /** The existing shape codec closes polygons again on decode; compare the authored vertices. */
    private static List<ReticleCoordinate> outlineVertices(ReticleShape shape) {
        List<ReticleCoordinate> outline = shape.getOutline();
        int end = outline.size();
        while (end > 1 && outline.get(end - 1).equals(outline.get(0))) end--;
        return outline.subList(0, end);
    }
}

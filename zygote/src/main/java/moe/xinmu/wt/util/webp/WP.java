package moe.xinmu.wt.util.webp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.jcodec.codecs.vp8.VP8Decoder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class WP {
    static final int RIFF_MAGIC = 0x52494646;
    static final int WEBP_MAGIC = 0x57454250;
    static final int VP8__MAGIC = 0x56503820;
    static final int VP8_SIGNATURE_MAGIC = 0x9d012a;

    void decode(byte[] bytes) throws IOException {
        ByteBuf buffer = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
        buffer.writeBytes(bytes);
        eqTest(buffer.readInt(), RIFF_MAGIC);

        int size = buffer.readIntLE();
        if (bytes.length < (size + 8)) {
            throw new IOException("Incomplete.");
        }
        eqTest(buffer.readInt(), WEBP_MAGIC);
        eqTest(buffer.readInt(), VP8__MAGIC);
        buffer.markReaderIndex();
        new VP8Decoder().decode(ByteBuffer.wrap(bytes).position(16));

        readVP8(buffer);

    }
    void readVP8(ByteBuf buffer){
        int chunkSize = buffer.readIntLE();
        buffer.markReaderIndex();
        buffer.skipBytes(3);
//        buffer.position(buffer.position() + 3);
        // VP8CheckSignature

        byte[] bufferTmp = new byte[4];
        buffer.readBytes(bufferTmp, 1, 3);
        eqTest(ByteBuffer.wrap(bufferTmp).getInt(), VP8_SIGNATURE_MAGIC);
        buffer.resetReaderIndex();

//        buffer.position(buffer.position() - 6);

        int bits;
        bufferTmp = new byte[4];
        buffer.readBytes(bufferTmp, 0, 3);
        bits = ByteBuffer.wrap(bufferTmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
        boolean keyFrame_ = (bits & 1) == 0;
        int profile_ = (bits >>> 1) & 7;
        boolean show_ = (bits >>> 4) != 0;
        int partitionLength_ = (bits >>> 5);
        buffer.skipBytes(3);
        boolean keyFrame = (bits & 1) != 1;
        int w = buffer.readShortLE() & 0x3fff;
        int xscale = w >>> 14;
        int h = buffer.readShortLE() & 0x3fff;
        int yscale = w >>> 14;
        buffer.markReaderIndex();
        long in_bits = buffer.readLongLE();
        buffer.resetReaderIndex();
        buffer.skipBytes(6);

        System.out.println();
    }

    void eqTest(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new RuntimeException("need '" + o1 + "' but found '" + o2 + "' .");
        }
    }

    public static void main(String[] args) throws IOException {
        new WP().decode(new FileInputStream("F:\\1583573798372\\1234567891234.webp").readAllBytes());
    }
}

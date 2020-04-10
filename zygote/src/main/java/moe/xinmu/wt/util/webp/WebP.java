package moe.xinmu.wt.util.webp;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// 本代码转制自libwebp，不符合规范等问题待兼容后会处理
@Deprecated
@SuppressWarnings("all")
public class WebP {
    static final int RIFF_MAGIC = 0x52494646;
    static final int WEBP_MAGIC = 0x57454250;
    static final int VP8__MAGIC = 0x56503820;
    static final int VP8_SIGNATURE_MAGIC = 0x9d012a;

    void decode(byte[] bytes) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        eqTest(buffer.getInt(), RIFF_MAGIC);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int size = buffer.getInt();
        if (bytes.length < (size + 8)) {
            throw new IOException("Incomplete.");
        }
        buffer.order(ByteOrder.BIG_ENDIAN);
        eqTest(buffer.getInt(), WEBP_MAGIC);
        eqTest(buffer.getInt(), VP8__MAGIC);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int chunkSize = buffer.getInt();
        buffer.position(buffer.position() + 3);
        // VP8CheckSignature
        buffer.order(ByteOrder.BIG_ENDIAN);
        byte[] bufferTmp = new byte[4];
        buffer.get(bufferTmp, 1, 3);
        eqTest(ByteBuffer.wrap(bufferTmp).getInt(), VP8_SIGNATURE_MAGIC);
        buffer.position(buffer.position() - 6);

        int bits;
        bufferTmp = new byte[4];
        buffer.get(bufferTmp, 0, 3);
        bits = ByteBuffer.wrap(bufferTmp).order(ByteOrder.LITTLE_ENDIAN).getInt();
        boolean keyFrame_ = (bits & 1) == 0;
        int profile_ = (bits >>> 1) & 7;
        boolean show_ = (bits >>> 4) != 0;
        int partitionLength_ = (bits >>> 5);
        buffer.position(buffer.position() + 3);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        boolean keyFrame = (bits & 1) != 1;
        int w = buffer.getShort() & 0x3fff;
        int xscale = w >>> 14;
        int h = buffer.getShort() & 0x3fff;
        int yscale = w >>> 14;
        long in_bits = buffer.getLong();
        buffer.position(buffer.position() - 1);
        System.out.println();
    }
    void eqTest(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            throw new RuntimeException("need '" + o1 + "' but found '" + o2 + "' .");
        }
    }

    public static void main(String[] args) throws IOException {
        new WebP().decode(new FileInputStream("F:\\1583573798372\\1583573798372.webp").readAllBytes());
    }
}

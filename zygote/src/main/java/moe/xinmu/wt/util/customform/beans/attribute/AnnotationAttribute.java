package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class AnnotationAttribute implements BaseAttribute {
    String annotation;
    public static final String TAG = "Annotation";

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void parse(ByteBuf buf) {
        byte[] buffer = new byte[buf.readInt()];
        buf.readBytes(buffer);
        annotation = new String(buffer);
    }

    @Override
    public void dump(ByteBuf buf) {
        byte[] aBuffer = annotation.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(aBuffer.length);
        buf.writeBytes(aBuffer);
    }
}

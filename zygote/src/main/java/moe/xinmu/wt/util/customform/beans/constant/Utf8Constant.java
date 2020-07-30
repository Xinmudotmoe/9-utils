package moe.xinmu.wt.util.customform.beans.constant;

import io.netty.buffer.ByteBuf;

public class Utf8Constant extends BaseConstant {
    String s;
    public static final int TAG=1;
    @Override
    public byte getTag() {
        return 1;
    }

    @Override
    public void parse(ByteBuf buf) {
        byte[] buffer = new byte[buf.readInt()];
        buf.readBytes(buffer);
        s = new String(buffer);
    }

    @Override
    public void dump(ByteBuf buf) {
        byte[] buffer = s.getBytes();
        buf.writeInt(buffer.length);
        buf.writeBytes(buffer);
    }

}

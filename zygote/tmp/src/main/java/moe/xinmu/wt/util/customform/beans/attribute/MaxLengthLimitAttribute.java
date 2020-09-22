package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;

public class MaxLengthLimitAttribute implements BaseLimitAttribute {
    int length;
    public static final String TAG = "MaxLengthLimit";

    @Override
    public String getName() {
        return TAG;
    }
    @Override
    public void parse(ByteBuf buf) {
        length = buf.readInt();
    }

    @Override
    public void dump(ByteBuf buf) {
        buf.writeInt(length);
    }

    @Override
    public boolean supportMetaType(MetaType type) {
        return type == MetaType.STRING;
    }

    @Override
    public boolean check(String res) {
        return length > res.length();
    }
}

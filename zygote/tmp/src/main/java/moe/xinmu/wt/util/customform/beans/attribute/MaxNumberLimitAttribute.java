package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;

public class MaxNumberLimitAttribute implements BaseLimitAttribute {
    long max;
    public static final String TAG = "MaxLimit";

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void parse(ByteBuf buf) {
        max = buf.readLong();
    }

    @Override
    public void dump(ByteBuf buf) {
        buf.writeLong(max);
    }

    @Override
    public boolean supportMetaType(MetaType type) {
        return type == MetaType.STRING;
    }

    @Override
    public boolean check(String res) {
        return Long.parseLong(res) < max;
    }
}

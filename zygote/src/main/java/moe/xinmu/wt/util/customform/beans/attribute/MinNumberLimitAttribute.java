package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;

public class MinNumberLimitAttribute implements BaseLimitAttribute {
    long min;
    public static final String TAG = "MinLimit";

    @Override
    public String getName() {
        return TAG;
    }
    @Override
    public void dump(ByteBuf buf) {
        buf.writeLong(min);
    }

    @Override
    public void parse(ByteBuf buf) {
        min = buf.readLong();
    }

    @Override
    public boolean supportMetaType(MetaType type) {
        return type == MetaType.NUMBER;
    }

    @Override
    public boolean check(String res) {
        return Long.parseLong(res) > min;
    }
}

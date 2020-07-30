package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;

public class NotEmptyLimitAttribute implements BaseLimitAttribute {
    public static final String TAG = "NotEmptyLimit";

    @Override
    public String getName() {
        return TAG;
    }

    @Override
    public void parse(ByteBuf buf) {

    }

    @Override
    public void dump(ByteBuf buf) {

    }

    @Override
    public boolean supportMetaType(MetaType type) {
        return type == MetaType.STRING;
    }

    @Override
    public boolean check(String res) {
        return !res.isEmpty();
    }
}

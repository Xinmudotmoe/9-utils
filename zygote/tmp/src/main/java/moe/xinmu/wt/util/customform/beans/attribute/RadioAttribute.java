package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;
@Deprecated
public class RadioAttribute implements BaseLimitAttribute {

    @Override
    public String getName() {
        return "Radio";
    }

    @Override
    public void parse(ByteBuf buf) {

    }

    @Override
    public void dump(ByteBuf buf) {

    }

    @Override
    public boolean check(String res) {
        return false;
    }

    @Override
    public boolean supportMetaType(MetaType type) {
        return type == MetaType.RADIO;
    }
}

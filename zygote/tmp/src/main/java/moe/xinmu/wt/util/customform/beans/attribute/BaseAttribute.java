package moe.xinmu.wt.util.customform.beans.attribute;

import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.MetaType;

public interface BaseAttribute {
    String getName();

    void parse(ByteBuf buf);

    void dump(ByteBuf buf);

    default boolean supportMetaType(MetaType type) {
        return true;
    }
}

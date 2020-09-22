package moe.xinmu.wt.util.customform.beans.constant;

import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class BaseConstant {
    public abstract byte getTag();

    public abstract void parse(ByteBuf buf);

    public abstract void dump(ByteBuf buf);
}

package moe.xinmu.wt.util.customform.beans;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import moe.xinmu.wt.util.customform.beans.attribute.*;
import moe.xinmu.wt.util.customform.beans.constant.ConstantDistribution;

import java.util.List;

public class Field {
    String name;
    List<BaseAttribute> attributes;

    public void parse(ConstantDistribution distribution, ByteBuf buf) {
        name = distribution.get(String.class, buf.readInt());
        int attributeSize = buf.readInt();
        attributes = Lists.newArrayList();
        for (int i = 0; i < attributeSize; i++) {
            BaseAttribute attribute;
            switch (distribution.get(String.class, buf.readInt())) {
                case AnnotationAttribute.TAG:
                    attribute = new AnnotationAttribute();
                    attribute.parse(buf);
                    break;
                case MaxLengthLimitAttribute.TAG:
                    attribute = new MaxLengthLimitAttribute();
                    attribute.parse(buf);
                    break;
                case MaxNumberLimitAttribute.TAG:
                    attribute = new MaxNumberLimitAttribute();
                    attribute.parse(buf);
                    break;
                case MinLengthLimitAttribute.TAG:
                    attribute = new MinLengthLimitAttribute();
                    attribute.parse(buf);
                    break;
                case MinNumberLimitAttribute.TAG:
                    attribute = new MinNumberLimitAttribute();
                    attribute.parse(buf);
                    break;
                case NotEmptyLimitAttribute.TAG:
                    attribute = new NotEmptyLimitAttribute();
                    attribute.parse(buf);
                    break;
                default:
                    throw new RuntimeException();
            }
        }
    }

    public void dump(ConstantDistribution distribution, ByteBuf buf) {
        buf.writeInt(distribution.addString(name));
        buf.writeInt(attributes.size());
        for (BaseAttribute attribute : attributes) {
            attribute.dump(buf);
        }
    }
}

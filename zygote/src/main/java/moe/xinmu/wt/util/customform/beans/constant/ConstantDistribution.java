package moe.xinmu.wt.util.customform.beans.constant;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringJoiner;

public class ConstantDistribution {
    ArrayList<Object> list = new ArrayList<>();
    HashSet<Object> set = new HashSet<>();

    public int addString(String s) {
        return add(s);
    }

    private int add(Object o) {
        if (!set.add(o)) {
            //如果增加失败 则查找
            return list.indexOf(o);
        }
        list.add(o);
        //否则进行最终增加
        return list.size() - 1;
    }

    public  <T> T get(Class<T> tClass, int index) {
        return (T) list.get(index);
    }

    public void dump(ByteBuf buf) {
        buf.writeInt(list.size());
        for (Object o : list) {
            if (o instanceof String) {
                byte[] b = ((String) o).getBytes(StandardCharsets.UTF_8);
                buf.writeByte(Utf8Constant.TAG);
                buf.writeInt(b.length);
                buf.writeBytes(b);
            }
        }
    }

    public void parse(ByteBuf buf) {
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            switch (buf.readByte()) {
                case Utf8Constant.TAG:
                    int sLength = buf.readInt();
                    byte[] sBuffer = new byte[sLength];
                    buf.readBytes(sBuffer);
                    list.add(new String(sBuffer, StandardCharsets.UTF_8));
                    break;
                default:
                    break;
            }
        }
    }
}

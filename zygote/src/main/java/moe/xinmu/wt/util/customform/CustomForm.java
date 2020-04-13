package moe.xinmu.wt.util.customform;

import com.google.common.collect.Lists;
import moe.xinmu.wt.util.customform.beans.ConstantType;
import moe.xinmu.wt.util.customform.beans.MetaConstant;
import moe.xinmu.wt.util.customform.beans.MetaInformation;
import moe.xinmu.wt.util.customform.beans.MetaType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

/**
 * 自定义表单相关工具
 *
 * @author Xinmu
 */
@NoArgsConstructor
public class CustomForm {
    @Getter
    private MetaInformation information = new MetaInformation();

    /**
     * 此方法用于从填报数据头转换为元信息
     *
     * @param information 元信息对象
     */
    public CustomForm(MetaInformation information) {
        this.information = information;
    }

    /**
     * 通过字节数据获取元信息
     *
     * @param bytes 字节数据
     */
    public void readMetaBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        information.setVersion(buffer.get());
        if (information.getVersion() > 0) {
            throw new RuntimeException();
        }
        information.setConstantList(Lists.newArrayList());
        int size = buffer.getInt();
        for (int i = 0; i < size; i++) {
            byte type = buffer.get();
            int nameLength = buffer.getInt();
            byte[] nameBuffer = new byte[nameLength];
            buffer.get(nameBuffer);
            String name = new String(nameBuffer, StandardCharsets.UTF_8);
            Integer width = buffer.getInt();
            MetaConstant constant = MetaConstant.builder()
                    .type(MetaType.values()[type])
                    .name(name)
                    .width(width)
                    .build();
            information.getConstantList().add(constant);
        }
    }

    /**
     * 将元信息写回到字节数据
     */
    public byte[] writeMetaBytes() {
        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
        buf.writeByte(information.getVersion());
        buf.writeInt(information.getConstantList().size());
        for (MetaConstant constant : information.getConstantList()) {
            buf.writeByte((byte) constant.getType().ordinal());
            byte[] nameBuffer = constant.getName().getBytes(StandardCharsets.UTF_8);
            buf.writeInt(nameBuffer.length);
            buf.writeBytes(nameBuffer);
            buf.writeInt(constant.getWidth());
        }
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes, 0, buf.writerIndex());
        return bytes;
    }

    /**
     * 生成归档信息
     *
     * @param data 数据源
     * @return 字节数据
     */
    public byte[] genDataBytes(List<String> data) {
        ByteBuf buf = UnpooledByteBufAllocator.DEFAULT.heapBuffer();
        for (int i = 0; i < information.getConstantList().size(); i++) {
            String d = data.get(i);
            switch (information.getConstantList().get(i).getType()) {
                case NUMBER:
                    buf.writeByte(ConstantType.NUMBER.ordinal());
                    if (Objects.isNull(d) || d.isEmpty()) {
                        buf.writeLong(0);
                    } else {
                        buf.writeLong(Long.decode(d));
                    }
                    break;
                case STRING:
                    if (Objects.isNull(d)) {
                        buf.writeByte(ConstantType.NULL.ordinal());
                    } else {
                        buf.writeByte(ConstantType.STRING.ordinal());
                        byte[] sdata = d.getBytes(StandardCharsets.UTF_8);
                        buf.writeInt(sdata.length);
                        buf.writeBytes(sdata);
                    }
                    break;
                default:
                    throw new RuntimeException("Unsupport.");
            }
        }
        byte[] bytes = new byte[buf.writerIndex()];
        buf.getBytes(0, bytes, 0, buf.writerIndex());
        return bytes;
    }


    /**
     * 解析归档信息
     *
     * @param data 字节数据
     * @return 解析数据
     */
    public List<Object> parseDataBytes(byte[] data) {
        List<Object> objects = Lists.newArrayList();
        if (Objects.isNull(data) || data.length == 0) {
            for (MetaConstant constant : information.getConstantList()) {
                switch (constant.getType()) {
                    case STRING:
                        objects.add(null);
                        break;
                    case NUMBER:
                        objects.add(0);
                        break;
                    default:
                        throw new RuntimeException("");
                }
            }
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.order(ByteOrder.BIG_ENDIAN);
            for (MetaConstant constant : information.getConstantList()) {
                ConstantType type = ConstantType.values()[buffer.get()];
                switch (constant.getType()) {
                    case STRING:
                        switch (type) {
                            case NULL:
                                objects.add(null);
                                break;
                            case STRING:
                                int length = buffer.getInt();
                                byte[] stringBuffer = new byte[length];
                                buffer.get(stringBuffer);
                                objects.add(new String(stringBuffer, StandardCharsets.UTF_8));
                                break;
                            default:
                                throw new RuntimeException();
                        }
                        break;
                    case NUMBER:
                        if (type != ConstantType.NUMBER) {
                            throw new RuntimeException();
                        }
                        objects.add(buffer.getLong());
                        break;
                    default:
                        throw new RuntimeException("");
                }
            }
        }
        return objects;
    }

    /**
     * 解析归档信息 并生成统计信息
     *
     * @param source 源字节数据列表
     * @return 解析数据列表
     */
    public List<List<Object>> parseDataAndStatistics(List<byte[]> source) {
        List<List<Object>> target = Lists.newArrayList();
        source.forEach(bytes -> target.add(parseDataBytes(bytes)));
        List<Object> objects = Lists.newArrayList();
        for (int i = 0; i < information.getConstantList().size(); i++) {
            MetaConstant constant = information.getConstantList().get(i);

            if (constant.getType() == MetaType.NUMBER) {
                int finalI = i;
                long sum = target.stream()
                        .map(m -> m.get(finalI))
                        .flatMapToLong(m -> LongStream.of((Long) m))
                        .sum();
                objects.add(sum);
            } else {
                objects.add(null);
            }
        }
        target.add(objects);
        return target;
    }
}

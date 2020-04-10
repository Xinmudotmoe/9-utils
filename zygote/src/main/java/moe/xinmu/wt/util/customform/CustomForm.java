package moe.xinmu.wt.util.customform;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import moe.xinmu.wt.util.customform.beans.MetaConstant;
import moe.xinmu.wt.util.customform.beans.MetaInformation;
import moe.xinmu.wt.util.customform.beans.MetaType;
import moe.xinmu.wt.util.customform.beans.ConstantType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

/**
 * 自定义表单相关工具
 */
@NoArgsConstructor
@AllArgsConstructor
public class CustomForm {
    @Getter
    MetaInformation information = new MetaInformation();

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

    @SneakyThrows
    public byte[] writeMetaBytes() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(outputStream);
        os.write(information.getVersion());
        os.writeInt(information.getConstantList().size());
        for (MetaConstant constant : information.getConstantList()) {
            outputStream.write((byte) constant.getType().ordinal());
            byte[] nameBuffer = constant.getName().getBytes(StandardCharsets.UTF_8);
            os.writeInt(nameBuffer.length);
            os.write(nameBuffer);
            os.writeInt(constant.getWidth());
        }
        return outputStream.toByteArray();
    }

    @SneakyThrows
    public byte[] genDataBytes(List<String> data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream os = new DataOutputStream(outputStream);
        for (int i = 0; i < information.getConstantList().size(); i++) {
            String d = data.get(i);
            switch (information.getConstantList().get(i).getType()) {
                case NUMBER:
                    os.write(ConstantType.NUMBER.ordinal());
                    if (Objects.isNull(d) || d.isEmpty()) {
                        os.writeLong(0);
                    } else {
                        os.writeLong(Long.decode(d));
                    }
                    break;
                case STRING:
                    if (Objects.isNull(d)) {
                        os.write(ConstantType.NULL.ordinal());
                    } else {
                        os.write(ConstantType.STRING.ordinal());
                        byte[] sdata = d.getBytes(StandardCharsets.UTF_8);
                        os.writeInt(sdata.length);
                        os.write(sdata);
                    }
                    break;
                default:
                    throw new RuntimeException("Unsupport.");
            }
        }
        return outputStream.toByteArray();
    }

    @SneakyThrows
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
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            for (MetaConstant constant : information.getConstantList()) {
                ConstantType type = ConstantType.values()[in.read()];
                switch (constant.getType()) {
                    case STRING:
                        switch (type) {
                            case NULL:
                                objects.add(null);
                                break;
                            case STRING:
                                int length = in.readInt();
                                byte[] buffer = new byte[length];
                                in.read(buffer);
                                objects.add(new String(buffer, StandardCharsets.UTF_8));
                                break;
                            default:
                                throw new RuntimeException();
                        }
                        break;
                    case NUMBER:
                        if (type != ConstantType.NUMBER) {
                            throw new RuntimeException();
                        }
                        objects.add(in.readLong());
                        break;
                    default:
                        throw new RuntimeException("");
                }
            }
        }
        return objects;
    }

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

package moe.xinmu.beanutils2;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 注意 此BeanUtil仍未完成
 *
 * @author xinmu
 * @since 2020/09/19
 */
public class BusyBeanUtils {
    static final Map<Class<?>, BeanContext<?>> CONTEXT_MAP = new HashMap<>();
    static final Map<SimpleDoubleKey<Class<?>, Class<?>>, Function<?, ?>> CAST_PROCESS_MAP = new HashMap<>();

    static {
        // TODO 单步路径式查询支持
        addCastProcess(Integer.class, Long.class, Integer::longValue);
        addCastProcess(Date.class, LocalDateTime.class, date -> date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        addCastProcess(BigDecimal.class, Integer.class, BigDecimal::intValue);
        addCastProcess(BigDecimal.class, Long.class, BigDecimal::longValue);
        addCastProcess(BigDecimal.class, Double.class, BigDecimal::doubleValue);
        addCastProcess(BigDecimal.class, Float.class, BigDecimal::floatValue);
//        addCastProcess(java.sql.Date.class, LocalDateTime.class, date -> new Date(date.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//        addCastProcess(java.sql.Time.class, LocalDateTime.class, date -> new Date(date.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
//        addCastProcess(java.sql.Timestamp.class, LocalDateTime.class, date -> new Date(date.getTime()).toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    public static <A, B> void addCastProcess(Class<A> aClass, Class<B> bClass, Function<A, B> function) {
        CAST_PROCESS_MAP.put(new SimpleDoubleKey<>(aClass, bClass), function);
    }

    public static <T> BeanContext<T> genBeanContext(Class<T> tClass) {
        BeanContext.BeanContextBuilder<T> builder = BeanContext.<T>builder()
                .tClass(tClass)
                .init(LambdaControl.genInit(tClass));
        List<PropertyDescriptor> descriptorList;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(tClass, Object.class);
            descriptorList = Arrays.asList(beanInfo.getPropertyDescriptors());
        } catch (Throwable t) {
            builder.setters(Collections.emptyMap());
            builder.getters(Collections.emptyMap());
            builder.setterTypes(Collections.emptyMap());
            builder.getterTypes(Collections.emptyMap());
            return builder.build();
        }
        Map<String, BiConsumer<T, ?>> setters = new HashMap<>();
        Map<String, Class<?>> setterTypes = new HashMap<>();
        Map<String, Function<T, ?>> getters = new HashMap<>();
        Map<String, Class<?>> getterTypes = new HashMap<>();

        for (PropertyDescriptor descriptor : descriptorList) {
            Method readMethod = descriptor.getReadMethod();
            if (readMethod != null) {
                Function<T, ?> getter = LambdaControl.genGetter(tClass,
                        descriptor.getPropertyType(),
                        readMethod.getName());
                if (getter != null) {
                    getters.put(descriptor.getName(), getter);
                    getterTypes.put(descriptor.getName(), LambdaControl.primitiveTypeToBeanClass(descriptor.getPropertyType()));
                }
            }
            Method writeMethod = descriptor.getWriteMethod();
            if (writeMethod != null) {
                BiConsumer<T, ?> setter = LambdaControl.genSetter(tClass,
                        descriptor.getPropertyType(),
                        writeMethod.getName());
                if (setter != null) {
                    setters.put(descriptor.getName(), setter);
                    setterTypes.put(descriptor.getName(), LambdaControl.primitiveTypeToBeanClass(descriptor.getPropertyType()));
                }
            }
        }
        builder.setters(Collections.unmodifiableMap(setters));
        builder.setterTypes(setterTypes);
        builder.getters(Collections.unmodifiableMap(getters));
        builder.getterTypes(getterTypes);
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static synchronized <T> BeanContext<T> get(Class<T> tClass) {
        BeanContext<T> context = (BeanContext<T>) CONTEXT_MAP.get(tClass);
        if (context == null) {
            context = genBeanContext(tClass);
            CONTEXT_MAP.put(tClass, context);
        }
        return context;
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertMap(Map<String, Object> map, Class<T> tClass, int... flags) {
        int flag = 0;
        for (int f : flags) {
            flag |= f;
        }
        BeanContext<T> context = get(tClass);
        T target = context.getInit().get();

        if (target == null) {
            return null;
        }
        Map<String, String> ignoreKeyNameMap = Collections.emptyMap();
        if ((flag & BeanFlags.IGNORE_CASE) != 0) {
            ignoreKeyNameMap = new HashMap<>();
            for (String key : context.getSetterTypes().keySet()) {
                ignoreKeyNameMap.put(key.toLowerCase(), key);
            }
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fieldName;
            if ((flag & BeanFlags.DISABLE_CASE) == 0) {
                fieldName = underScoreCaseToCamelCase(entry.getKey());
            } else {
                fieldName = entry.getKey();
            }
            if ((flag & BeanFlags.IGNORE_CASE) != 0) {
                fieldName = ignoreKeyNameMap.getOrDefault(fieldName.toLowerCase(), fieldName);
            }
            BiConsumer<T, Object> consumer = (BiConsumer<T, Object>) context.getSetters()
                    .get(fieldName);
            if (consumer != null && entry.getValue() != null) {
                Object object = entry.getValue();
                Class<?> needType = context.getSetterTypes().get(fieldName);

                if (!needType.isAssignableFrom(object.getClass())) {
                    SimpleDoubleKey<Class<?>, Class<?>> classSimpleDoubleKey = new SimpleDoubleKey<>(object.getClass(), needType);
                    Function<Object, ?> caster = (Function<Object, ?>) CAST_PROCESS_MAP.get(classSimpleDoubleKey);
                    if (caster == null) {
                        continue;
                    }
                    object = caster.apply(object);
                }
                consumer.accept(target, object);
            }
        }
        return target;
    }

    /**
     * 下划线命名法转驼峰命名法
     *
     * @param underScoreCase 下划线命名
     * @return 驼峰命名法
     */
    public static String underScoreCaseToCamelCase(String underScoreCase) {
        if (!underScoreCase.contains("_")) {
            return underScoreCase;
        }
        StringBuilder builder = new StringBuilder(underScoreCase.length());
        underScoreCase = underScoreCase.toLowerCase();
        boolean flag = false;
        for (char c : underScoreCase.toCharArray()) {
            if (c == '_') {
                flag = true;
                continue;
            }
            if (flag) {
                c = Character.toUpperCase(c);
                flag = false;
            }
            builder.append(c);
        }
        return builder.toString();
    }
}

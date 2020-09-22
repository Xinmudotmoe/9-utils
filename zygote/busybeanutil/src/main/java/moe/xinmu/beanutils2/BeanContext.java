package moe.xinmu.beanutils2;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Bean控制器上下文
 * 其逻辑与XBeanUtil中动态生成的类基本一致（并且更加占用Metaspace）
 * 但相对XBeanUtil，不再有晦涩的字节码操控方法，而是基于Lambda的实现
 *
 * @param <T>
 * @author xinmu
 */
@Getter
@Builder
@ToString
public class BeanContext<T> {
    final Class<T> tClass;
    final Map<String, BiConsumer<T, ?>> setters;
    final Map<String, Class<?>> setterTypes;
    final Map<String, Function<T, ?>> getters;
    final Map<String, Class<?>> getterTypes;

    final Supplier<T> init;
}

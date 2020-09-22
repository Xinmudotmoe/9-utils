package moe.xinmu.beanutils2;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lambda控制器，用于快速创建Lambda代理类
 * 不建议维护这里的代码
 *
 * @author xinmu
 */
public class LambdaControl {

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    static final Map<Class<?>, Class<?>> primitiveTypeToBeanClassMap = new HashMap<>();

    static {
        primitiveTypeToBeanClassMap.put(boolean.class, Boolean.class);
        primitiveTypeToBeanClassMap.put(byte.class, Byte.class);
        primitiveTypeToBeanClassMap.put(short.class, Short.class);
        primitiveTypeToBeanClassMap.put(int.class, Integer.class);
        primitiveTypeToBeanClassMap.put(long.class, Long.class);
        primitiveTypeToBeanClassMap.put(float.class, Float.class);
        primitiveTypeToBeanClassMap.put(double.class, Double.class);
        primitiveTypeToBeanClassMap.put(char.class, Character.class);
    }

    /**
     * 生成构造方法
     *
     * @param tClass 主Bean类
     * @param <T>    主Bean类
     * @return Bean 构造方法
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> genInit(Class<T> tClass) {
        try {
            return (Supplier<T>) LambdaMetafactory.metafactory(LOOKUP,
                    "get",
                    MethodType.methodType(Supplier.class),
                    MethodType.methodType(Object.class),
                    LOOKUP.findConstructor(tClass, MethodType.methodType(void.class)),
                    MethodType.methodType(tClass)
            ).dynamicInvoker().invoke();
        } catch (Throwable t) {
            return () -> null;
        }
    }

    /**
     * 生成Getter方法
     *
     * @param tClass       主Bean类
     * @param rClass       出参类
     * @param functionName getter方法名
     * @param <T>          主Bean类
     * @param <R>          出参类
     * @return Getter操纵方法 (入Bean 出getter)
     */
    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, R> genGetter(Class<T> tClass, Class<R> rClass, String functionName) {
        try {
            return (Function<T, R>) LambdaMetafactory.metafactory(LOOKUP,
                    "apply",
                    MethodType.methodType(Function.class),
                    MethodType.methodType(Object.class, Object.class),
                    LOOKUP.findVirtual(tClass, functionName, MethodType.methodType(rClass)),
                    MethodType.methodType(primitiveTypeToBeanClass(rClass), tClass)
            ).dynamicInvoker().invoke();
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * 生成Setter方法
     *
     * @param tClass       主Bean类
     * @param aClass       入参类
     * @param functionName setter方法名
     * @param <T>          主Bean类
     * @param <A>          入参类
     * @return Setter操纵方法
     */
    @SuppressWarnings("unchecked")
    public static <T, A> BiConsumer<T, A> genSetter(Class<T> tClass, Class<A> aClass, String functionName) {
        try {
            return (BiConsumer<T, A>) LambdaMetafactory.metafactory(LOOKUP,
                    "accept",
                    MethodType.methodType(BiConsumer.class),
                    MethodType.methodType(void.class, Object.class, Object.class),
                    LOOKUP.findVirtual(tClass, functionName, MethodType.methodType(void.class, aClass)),
                    MethodType.methodType(void.class, tClass, primitiveTypeToBeanClass(aClass))
            ).dynamicInvoker().invoke();
        } catch (Throwable t) {
            return null;
        }
    }

    public static Class<?> primitiveTypeToBeanClass(Class<?> pClass) {
        return primitiveTypeToBeanClassMap.getOrDefault(pClass, pClass);
    }
}

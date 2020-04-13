package moe.xinmu.wt.util.beanutil;

import com.google.common.collect.Maps;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于字节码生成器的BeanUtil
 * 优点：高效，快速，弱异常
 * 缺点：配置困难 启动缓慢 类型转换差 无法完全替代BeanUtil
 * 慎用 待重写
 *
 * @author ZhangYuhao
 */
public class XBeanUtil {
    private static final org.objectweb.asm.commons.Method OBJECT_CONSTRUCTOR = new org.objectweb.asm.commons.Method("<init>", "()V");
    //TODO PathFinding Algorithm
    private static final Map<DoubleKey<Class<?>, Class<?>>, Process<?>> CAST_PROCESS_HASH_MAP = new HashMap<>();
    private static final Map<Class<?>, IBeanHandler<?>> BEAN_HANDLER_MAP = new HashMap<>();
    private static final HashSet<LoadSetMethodRegister<?>> INIT_REGISTERS = new HashSet<>();
    private static final Type OBJECT_CLASS = Type.getType(Object.class);
    private static final Type HASH_MAP_TYPE = Type.getType(HashMap.class);
    public final static int FALSE = 0;
    public final static int TRUE = 1;
    public static final int ZERO = 0;

    /**
     * 获取转换器
     *
     * @param tClass 源类型
     * @param <T>    源类型
     * @return 转换器
     */
    @SuppressWarnings("all")
    public static <T> IBeanHandler<T> getHandler(Class<T> tClass) {
        return (IBeanHandler<T>) BEAN_HANDLER_MAP.get(tClass);
    }

    /**
     * Bean初始化 兼容原始方案
     *
     * @param entity Bean对象 修改时会直接修改此对象 如果初始化处理器生成失败 会返回null
     */
    public static <T> T entityInit(T entity) {
        @SuppressWarnings("unchecked")
        T target = entityInit((Class<T>) entity.getClass(), entity);
        return target;
    }

    /**
     * Bean初始化
     *
     * @param entityClass Bean类型 会生成新Bean 如果没有空的构造方法 会报错 如果初始化处理器生成失败 会返回null
     */
    public static <T> T entityInit(Class<? extends T> entityClass) {
        return entityInit(entityClass, null);
    }

    private static <T> T entityInit(Class<? extends T> entityClass, T entity) {
        if (!BEAN_HANDLER_MAP.containsKey(entityClass)) {
            BEAN_HANDLER_MAP.put(entityClass, genConvert(entityClass));
        }
        @SuppressWarnings("unchecked")
        IBeanHandler<T> cast = (IBeanHandler<T>) BEAN_HANDLER_MAP.get(entityClass);
        if (cast == null) {
            return entity;
        }
        return cast.init(entity);
    }

    /**
     * Bean转换 注意此处也会进行初始化 与原始方案有差距
     *
     * @param source 源对象
     * @param zClass 目标类对象
     */

    public static <T, Z> Z cast(T source, Class<Z> zClass) {
        return copyProperties(source, entityInit(zClass));
    }

    /**
     * Bean之间的数据转换
     * 第一次转换速度最慢 当转换器就绪后时间将统一为恒定值
     * 注意 内部使用的是公共非静态的赋值、取值方法
     *
     * @param source 源对象
     * @param target 目标对象
     * @return Z 当转换器生成失败时 会返回null 否则将返回目标对象
     */
    public static <T, Z> Z copyProperties(T source, Z target) {
        if (!BEAN_HANDLER_MAP.containsKey(source.getClass())) {
            BEAN_HANDLER_MAP.put(source.getClass(), genConvert(source.getClass()));
        }
        if (!BEAN_HANDLER_MAP.containsKey(target.getClass())) {
            BEAN_HANDLER_MAP.put(target.getClass(), genConvert(target.getClass()));
        }
        @SuppressWarnings("unchecked")
        IBeanHandler<T> sourceProcess = (IBeanHandler<T>) BEAN_HANDLER_MAP.get(source.getClass());
        @SuppressWarnings("unchecked")
        IBeanHandler<Z> targetProcess = (IBeanHandler<Z>) BEAN_HANDLER_MAP.get(target.getClass());
        return targetProcess.cast(sourceProcess.cast(source), target);
    }

    /**
     * Bean之间的数据转换，过滤空值
     *
     * @param source 源对象
     * @param target 目标对象
     * @return Z 当转换器生成失败时 会返回null 否则将返回目标对象
     */
    public static <T, Z> Z copyPropertiesFilterNull(T source, Z target) {
        if (!BEAN_HANDLER_MAP.containsKey(source.getClass())) {
            BEAN_HANDLER_MAP.put(source.getClass(), genConvert(source.getClass()));
        }
        if (!BEAN_HANDLER_MAP.containsKey(target.getClass())) {
            BEAN_HANDLER_MAP.put(target.getClass(), genConvert(target.getClass()));
        }
        @SuppressWarnings("unchecked")
        IBeanHandler<T> sourceProcess = (IBeanHandler<T>) BEAN_HANDLER_MAP.get(source.getClass());
        @SuppressWarnings("unchecked")
        IBeanHandler<Z> targetProcess = (IBeanHandler<Z>) BEAN_HANDLER_MAP.get(target.getClass());
        Map<String, ?> map = sourceProcess.cast(source);
        Map<String, Object> map1 = Maps.newHashMap();
        for (String key : map.keySet()) {
            if (Objects.nonNull(map.get(key))) {
                map1.put(key, map.get(key));
            }
        }
        return targetProcess.cast(map1, target);
    }

    /**
     * Bean与Map之间的相互转换 转换为Map
     * 与第一次转换速度最慢 当转换器就绪后耗时为恒定值
     *
     * @param t 源对象
     * @return Map 当转换器生成失败时 会返回null 否则将返回目标Map对象
     */
    public static <T> Map<String, ?> convertBean(T t) {
        if (!BEAN_HANDLER_MAP.containsKey(t.getClass())) {
            BEAN_HANDLER_MAP.put(t.getClass(), genConvert(t.getClass()));
        }
        @SuppressWarnings("unchecked")
        IBeanHandler<T> cast = (IBeanHandler<T>) BEAN_HANDLER_MAP.get(t.getClass());
        if (cast == null) {
            return null;
        }
        return cast.cast(t);
    }

    /**
     * Bean与Map之间的相互转换 转换为Bean
     * 注意 Bean类应带有NoArgsConstructor注释 或包含公用无参构造方法
     * 第一次转换速度最慢 当转换器就绪后耗时为恒定值
     *
     * @param map    源数据
     * @param tClass 源类型
     * @return T 当转换器生成失败时 会返回null 否则将返回目标对象
     */
    public static <T> T convertMap(Map<String, ?> map, Class<T> tClass) {
        if (!BEAN_HANDLER_MAP.containsKey(tClass)) {
            BEAN_HANDLER_MAP.put(tClass, genConvert(tClass));
        }
        @SuppressWarnings("unchecked")
        IBeanHandler<T> cast = (IBeanHandler<T>) BEAN_HANDLER_MAP.get(tClass);
        if (cast == null) {
            return null;
        }
        return cast.cast(map, null);
    }

    /**
     * Bean处理器
     *
     * @param <T>
     */
    public interface IBeanHandler<T> {
        /**
         * 取出所有数据 将引用复制到Map中
         *
         * @param t 源数据（不为Null）
         * @return 引用数据
         */
        Map<String, ?> cast(T t);

        /**
         * 构造方法
         *
         * @return 构造对象
         */
        T construct();

        /**
         * bean初始化 为Null时会使用构造方法进行创建
         *
         * @param t 源
         * @return 初始化完成的对象
         */
        T init(T t);

        /**
         * 将Map中的所有数据引用赋值给Bean
         *
         * @param map 数据源
         * @param t   为null时使用构造方法
         * @return 赋值完成的对象
         */

        T cast(Map<String, ?> map, T t);

        String getLog();
    }

    private static void genConstructor(ClassWriter cw) {
        final GeneratorAdapter constructor = new GeneratorAdapter(cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                "()V", null, null), Opcodes.ACC_PUBLIC, "<init>", "()V");
        constructor.loadThis();
        constructor.invokeConstructor(OBJECT_CLASS, OBJECT_CONSTRUCTOR);
        constructor.returnValue();
        constructor.endMethod();
    }

    private static void genConstruct(ClassWriter cw, String name, Type tType, Class<?> clazz, StringJoiner log) {
        String constructDescriptor = "()" + Type.getDescriptor(clazz);
        /*gen Transformer Method*/
        MethodVisitor construct = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "construct",
                "()Ljava/lang/Object;", null, null);
        GeneratorAdapter constructAdapter = new GeneratorAdapter(construct, Opcodes.ACC_PUBLIC, "construct", "()Ljava/lang/Object;");
        constructAdapter.loadThis();
        constructAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("construct", constructDescriptor));
        constructAdapter.returnValue();
        constructAdapter.endMethod();
        /*gen Transformer Method End*/

        /*gen Construct Method*/
        construct = cw.visitMethod(Opcodes.ACC_PUBLIC, "construct", constructDescriptor, null, null);
        constructAdapter = new GeneratorAdapter(construct, Opcodes.ACC_PUBLIC, "construct", constructDescriptor);
        boolean notHasParameterLess;
        try {
            clazz.getConstructor();
            notHasParameterLess = false;
        } catch (Exception ignored) {
            notHasParameterLess = true;
        }
        if (notHasParameterLess) {
            constructAdapter.throwException(Type.getType(NoSuchMethodException.class), clazz.getName() + " Not Found NoArgsConstructor.");
        } else {
            constructAdapter.newInstance(tType);
            constructAdapter.dup();
            constructAdapter.invokeConstructor(tType, OBJECT_CONSTRUCTOR);
            constructAdapter.returnValue();
        }
        log.add("parameter less = " + !notHasParameterLess);
        constructAdapter.endMethod();
        /*gen Construct Method End*/
    }

    private static void genInit(ClassWriter cw, String name, Type tType, Class<?> clazz, StringJoiner log) {
        String initDescriptor = "(" + tType + ")" + tType;
        /*gen Initialization Transformer*/
        MethodVisitor init = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "init",
                "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        GeneratorAdapter initAdapter = new GeneratorAdapter(init, Opcodes.ACC_PUBLIC, "init", "(Ljava/lang/Object;)Ljava/lang/Object;");
        initAdapter.loadThis();
        initAdapter.loadArg(0);
        initAdapter.checkCast(tType);
        initAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("init", initDescriptor));
        initAdapter.returnValue();
        initAdapter.endMethod();
        /*gen Initialization Transformer end*/
        /*gen Initialization*/

        init = cw.visitMethod(Opcodes.ACC_PUBLIC, "init", initDescriptor, null, null);
        initAdapter = new GeneratorAdapter(init, Opcodes.ACC_PUBLIC, "init", initDescriptor);
        /*Initialization check null*/
        Label lnull = initAdapter.newLabel();
        initAdapter.loadArg(0);
        initAdapter.ifNonNull(lnull);
        initAdapter.loadThis();
        initAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("construct", "()" + tType));
        initAdapter.storeArg(0);
        initAdapter.goTo(lnull);
        initAdapter.visitLabel(lnull);
        /*Initialization check null*/
        List<Method> methods = Arrays.asList(clazz.getMethods());
        List<String> methodNames = Arrays.asList(Arrays.stream(clazz.getMethods()).map(Method::getName).distinct().toArray(String[]::new));

        for (LoadSetMethodRegister<?> register : INIT_REGISTERS) {
            try {
                if (!methodNames.contains(register.methodName)) {
                    continue;
                }

                Method[] setters = methods.stream()
                        .filter(z -> z.getName().equals(register.methodName))
                        .toArray(Method[]::new);
                for (Method setter : setters) {
                    try {
                        if (Modifier.isStatic(setter.getModifiers())) {
                            continue;
                        }
                        Class<?>[] setterParameterTypes = setter.getParameterTypes();
                        if (setterParameterTypes.length != 1) {
                            continue;
                        }
                        Process<?> castProcess = null;
                        if (setterParameterTypes[0] != register.tClass) {
                            castProcess = CAST_PROCESS_HASH_MAP.get(new DoubleKey<Class<?>, Class<?>>(register.tClass, setterParameterTypes[0]));
                            if (castProcess == null) {
                                continue;// Can`t Cast
                            }
                        }
                        Label l1 = initAdapter.newLabel();
                        Label l3 = initAdapter.newLabel();
                        if (castProcess != null) {
                            initAdapter.visitLabel(l1);
                        }
                        initAdapter.loadArg(0);
                        register.process.process(initAdapter);
                        if (castProcess != null) {
                            castProcess.process(initAdapter);
                        }
                        initAdapter.invokeVirtual(tType, org.objectweb.asm.commons.Method.getMethod(setter));
                        Label l2 = initAdapter.newLabel();
                        if (castProcess != null) {
                            initAdapter.goTo(l3);
                            initAdapter.visitLabel(l2);
                            initAdapter.catchException(l1, l2, Type.getType(Exception.class));
                            initAdapter.pop();
                            initAdapter.visitLabel(l3);
                        }
                        log.add("found init method = " + register.methodName);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        initAdapter.loadArg(0);
        /*gen Initialization end*/

        initAdapter.returnValue();
        initAdapter.endMethod();
    }

    private static void genCast(ClassWriter cw, String name, Type tType, Class<?> clazz, List<PropertyDescriptor> propertyDescriptors, StringJoiner log) {
        String castFromMapDescriptor = "(Ljava/util/Map;" + tType + ")Ljava/lang/Object;";
        /*gen Transformer Method*/
        MethodVisitor castFromObject = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "cast",
                "(Ljava/lang/Object;)Ljava/util/Map;", null, null);
        GeneratorAdapter castFromObjectAdapter = new GeneratorAdapter(castFromObject, Opcodes.ACC_PUBLIC, "cast",
                "(Ljava/lang/Object;)Ljava/util/Map;");

        castFromObjectAdapter.loadThis();
        castFromObjectAdapter.loadArg(0);
        castFromObjectAdapter.checkCast(tType);
        castFromObjectAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("cast",
                "(" + tType + ")Ljava/util/Map;"));
        castFromObjectAdapter.returnValue();
        castFromObjectAdapter.endMethod();
        /*gen Transformer End*/

        /*gen Cast Method*/
        castFromObject = cw.visitMethod(Opcodes.ACC_PUBLIC, "cast",
                "(" + tType + ")Ljava/util/Map;", null, null);
        castFromObjectAdapter = new GeneratorAdapter(castFromObject, Opcodes.ACC_PUBLIC, "cast",
                "(" + tType + ")Ljava/util/Map;");

        int mapLab = castFromObjectAdapter.newLocal(HASH_MAP_TYPE);
        castFromObjectAdapter.newInstance(HASH_MAP_TYPE);
        castFromObjectAdapter.dup();
        castFromObjectAdapter.invokeConstructor(HASH_MAP_TYPE, new org.objectweb.asm.commons.Method("<init>", "()V"));
        castFromObjectAdapter.storeLocal(mapLab);
        /*gen Transformer Method*/
        MethodVisitor castFromMap = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC, "cast",
                "(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        GeneratorAdapter castFromMapAdapter = new GeneratorAdapter(castFromMap, Opcodes.ACC_PUBLIC, "cast",
                "(Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/Object;");
        castFromMapAdapter.loadThis();
        castFromMapAdapter.loadArg(0);
        castFromMapAdapter.loadArg(1);
        castFromMapAdapter.checkCast(tType);
        castFromMapAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("cast", castFromMapDescriptor));
        castFromMapAdapter.returnValue();
        castFromMapAdapter.endMethod();
        /*gen Transformer End*/

        castFromMap = cw.visitMethod(Opcodes.ACC_PUBLIC, "cast",
                castFromMapDescriptor, null, null);
        castFromMapAdapter = new GeneratorAdapter(castFromMap, Opcodes.ACC_PUBLIC, "cast", castFromMapDescriptor);
        castFromMapAdapter.loadArg(1);
        Label labelNull = new Label();
        castFromMapAdapter.ifNonNull(labelNull);
        castFromMapAdapter.loadThis();
        castFromMapAdapter.invokeVirtual(Type.getType("L" + name + ";"), new org.objectweb.asm.commons.Method("construct", "()" + Type.getDescriptor(clazz)));
        castFromMapAdapter.storeArg(1);
        castFromMapAdapter.visitLabel(labelNull);
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            try {
                Method get = propertyDescriptor.getReadMethod();
                Objects.requireNonNull(get);
                castFromObjectAdapter.loadLocal(mapLab);
                castFromObjectAdapter.push(propertyDescriptor.getName());
                castFromObjectAdapter.loadArg(0);
                castFromObjectAdapter.invokeVirtual(tType, org.objectweb.asm.commons.Method.getMethod(get));
                Class<?> rClass = get.getReturnType();
                if (rClass.isPrimitive()) {
                    castFromObjectAdapter.box(Type.getType(rClass));
                }
                castFromObjectAdapter.invokeVirtual(HASH_MAP_TYPE, new org.objectweb.asm.commons.Method("put",
                        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
                castFromObjectAdapter.pop();
                log.add("found get = " + propertyDescriptor.getName());
            } catch (Exception ignored) {
            }
            try {
                //TODO Election Method
                Method setter = propertyDescriptor.getWriteMethod();
                Objects.requireNonNull(setter);
                castFromMapAdapter.loadArg(0);
                castFromMapAdapter.push(propertyDescriptor.getName());
                castFromMapAdapter.invokeInterface(Type.getType(Map.class),
                        new org.objectweb.asm.commons.Method("containsKey", "(Ljava/lang/Object;)Z"));
                Label over = castFromMapAdapter.newLabel();
                castFromMapAdapter.ifZCmp(Opcodes.IFEQ, over);
                Label label = castFromMapAdapter.newLabel();
                castFromMapAdapter.visitLabel(label);
                castFromMapAdapter.loadArg(1);
                castFromMapAdapter.loadArg(0);
                castFromMapAdapter.push(propertyDescriptor.getName());
                castFromMapAdapter.invokeInterface(Type.getType(Map.class),
                        new org.objectweb.asm.commons.Method("get", "(Ljava/lang/Object;)Ljava/lang/Object;"));
                if (propertyDescriptor.getPropertyType().isPrimitive()) {
                    castFromMapAdapter.unbox(Type.getType(propertyDescriptor.getPropertyType()));
                } else {
                    castFromMapAdapter.checkCast(Type.getType(propertyDescriptor.getPropertyType()));
                }
                //TODO Type Cast
                castFromMapAdapter.invokeVirtual(tType, org.objectweb.asm.commons.Method.getMethod(setter));
                castFromMapAdapter.goTo(over);
                Label label1 = castFromMapAdapter.newLabel();
                castFromMapAdapter.visitLabel(label1);
                castFromMapAdapter.catchException(label, label1, Type.getType(Exception.class));
                castFromMapAdapter.pop();
                castFromMapAdapter.visitLabel(over);
                log.add("found set = " + propertyDescriptor.getName());
            } catch (Exception ignored) {

            }
        }
        castFromMapAdapter.loadArg(1);
        castFromMapAdapter.returnValue();
        castFromMapAdapter.endMethod();

        castFromObjectAdapter.loadLocal(mapLab);
        castFromObjectAdapter.endMethod();
        castFromObjectAdapter.returnValue();
    }

    private static AtomicInteger autoincrementId = new AtomicInteger();

    private static <T> IBeanHandler<?> genConvert(Class<T> tClass) {
        StringJoiner log = new StringJoiner("\n");
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String name = XBeanUtil.class.getName().replace('.', '/') + "$IBeanHandler"
                + autoincrementId.getAndAdd(1) + "/_" + tClass.getSimpleName();

        Type tType = Type.getType(tClass);
        cw.visit(52, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, name, null,
                Type.getInternalName(Object.class), new String[]{Type.getInternalName(IBeanHandler.class)});
        cw.visitSource("Dynamic Class Generator", null);
        List<PropertyDescriptor> descriptorList;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(tClass, Object.class);
            descriptorList = Arrays.asList(beanInfo.getPropertyDescriptors());
        } catch (IntrospectionException e) {
            e.printStackTrace();
            return null;
        }
        log.add("found descriptor = " + descriptorList);
        genConstructor(cw);
        genConstruct(cw, name, tType, tClass, log);
        genInit(cw, name, tType, tClass, log);
        genCast(cw, name, tType, tClass, descriptorList, log);
        genLog(cw, log.toString());
        cw.visitEnd();

        byte[] bytes = cw.toByteArray();

        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<IBeanHandler<?>>) () ->
                    (IBeanHandler<?>) CLASSLOADER.pDefineClass(name.replace("/", "."), bytes, 0, bytes.length)
                            .getConstructor().newInstance()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void genLog(ClassWriter cw, String log) {
        final GeneratorAdapter constructor = new GeneratorAdapter(cw.visitMethod(Opcodes.ACC_PUBLIC, "getLog",
                "()Ljava/lang/String;", null, null), Opcodes.ACC_PUBLIC, "getLog", "()Ljava/lang/String;");
        constructor.loadThis();
        constructor.push(log);
        constructor.returnValue();
        constructor.endMethod();
    }


    /**
     * 非静态方法注册器
     * 用于 初始化Bean的方法
     */
    @Getter
    @AllArgsConstructor
    private static class LoadSetMethodRegister<T> {
        String methodName;
        Class<T> tClass;
        Process<T> process;

        @Override
        public boolean equals(Object obj) {
            if (Objects.isNull(obj)) {
                return false;
            }
            if (obj instanceof LoadSetMethodRegister) {
                LoadSetMethodRegister<?> reg = (LoadSetMethodRegister<?>) obj;
                return reg.methodName.equals(methodName);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return methodName.hashCode() + 1;
        }
    }

    /**
     * 静态无参方法的处理器
     * 用于 初始化Bean的方法
     */
    @Getter
    @AllArgsConstructor
    private static class InvokeStaticNoArgsMethodProcess<T> implements Process<T> {
        Method method;

        @Override
        public void process(GeneratorAdapter adapter) {
            adapter.invokeStatic(Type.getType(method.getDeclaringClass()), org.objectweb.asm.commons.Method.getMethod(method));
        }
    }

    /**
     * 调用无参构造方法的处理器
     * 用于 初始化Bean的方法
     */
    @Getter
    @AllArgsConstructor
    private static class InvokeNoArgsConstructorProcess<T> implements Process<T> {
        Class<T> classes;

        @Override
        public void process(GeneratorAdapter adapter) {
            Type type = Type.getType(classes);
            adapter.newInstance(type);
            adapter.dup();
            adapter.invokeConstructor(type, OBJECT_CONSTRUCTOR);
        }
    }

    /**
     * 获取静态字段的处理器
     * 用于 初始化Bean的方法
     */
    @Getter
    @AllArgsConstructor
    private static class GetStaticFieldProcess<T> implements Process<T> {
        Field field;

        @Override
        public void process(GeneratorAdapter adapter) {
            adapter.getStatic(Type.getType(field.getDeclaringClass()), field.getName(), Type.getType(field.getType()));
        }
    }

    /**
     * 屏蔽异常的运行器
     */
    private interface ExceptionInterceptionRunnable extends Runnable {
        @Override
        default void run() {
            try {
                interceptionRun();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void interceptionRun() throws Exception;
    }

    /**
     * 带泛型的双键数据类
     */
    @AllArgsConstructor
    @EqualsAndHashCode
    private static class DoubleKey<K1, K2> {
        private K1 k1;
        private K2 k2;
    }

    private interface Process<T> {
        void process(GeneratorAdapter adapter);
    }

    static {
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Character.class, char.class), adapter -> adapter.unbox(Type.CHAR_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Byte.class, byte.class), adapter -> adapter.unbox(Type.BYTE_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Short.class, short.class), adapter -> adapter.unbox(Type.SHORT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Integer.class, int.class), adapter -> adapter.unbox(Type.INT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Long.class, long.class), adapter -> adapter.unbox(Type.LONG_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Float.class, float.class), adapter -> adapter.unbox(Type.FLOAT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Double.class, double.class), adapter -> adapter.unbox(Type.DOUBLE_TYPE));

        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(char.class, Character.class), adapter -> adapter.box(Type.CHAR_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(byte.class, Byte.class), adapter -> adapter.box(Type.BYTE_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(short.class, Short.class), adapter -> adapter.box(Type.SHORT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(int.class, Integer.class), adapter -> adapter.box(Type.INT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(long.class, Long.class), adapter -> adapter.box(Type.LONG_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(float.class, Float.class), adapter -> adapter.box(Type.FLOAT_TYPE));
        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(double.class, Double.class), adapter -> adapter.box(Type.DOUBLE_TYPE));

        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(Date.class, long.class), adapter -> adapter.invokeVirtual(Type.getType(Date.class),
                new org.objectweb.asm.commons.Method("getTime", "()J")));

        CAST_PROCESS_HASH_MAP.put(new DoubleKey<>(long.class, Date.class), adapter -> {
            int local1 = adapter.newLocal(Type.LONG_TYPE);
            adapter.storeLocal(local1);
            adapter.newInstance(Type.getType(Date.class));
            adapter.dup();
            adapter.loadLocal(local1);
            adapter.invokeConstructor(Type.getType(Date.class), new org.objectweb.asm.commons.Method("<init>", "(J)V"));
        });

        ArrayList<ExceptionInterceptionRunnable> runnables = new ArrayList<>();
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("isPublish"), int.class, new GetStaticFieldProcess<>(XBeanUtil.class.getField("TRUE")))));
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("isDelete"), int.class, new GetStaticFieldProcess<>(XBeanUtil.class.getField("FALSE")))));
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("uuid"), String.class, new InvokeStaticNoArgsMethodProcess<>(XBeanUtil.class.getDeclaredMethod("uuid")))));
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("orderNum"), int.class, new GetStaticFieldProcess<>(XBeanUtil.class.getField("ZERO")))));
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("createTime"), Date.class, new InvokeNoArgsConstructorProcess<>(Date.class))));
        runnables.add(() -> INIT_REGISTERS.add(new LoadSetMethodRegister<>(XBeanUtil.loadSetMethodName("updateTime"), Date.class, new InvokeNoArgsConstructorProcess<>(Date.class))));
        runnables.forEach(Runnable::run);
    }


    private static final ClassLoader CLASSLOADER = new ClassLoader();

    public static final class ClassLoader extends java.lang.ClassLoader {
        private ClassLoader() {
            super(ClassLoader.class.getClassLoader());
        }

        public Class<?> pDefineClass(String name, byte[] b, int off, int len) {
            return defineClass(name, b, off, len, XBeanUtil.class.getProtectionDomain());
        }
    }

    /**
     * from BeanUtil
     * package-private
     *
     * @param filedName
     * @return
     */
    static String loadSetMethodName(String filedName) {
        return "set" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1);
    }

    /**
     * from CoreUtil
     *
     * @return
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replaceAll("-", "").toUpperCase();
    }

}

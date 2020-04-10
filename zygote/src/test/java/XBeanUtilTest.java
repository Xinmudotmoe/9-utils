import lombok.Data;
import moe.xinmu.wt.util.beanutil.XBeanUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class XBeanUtilTest {
    @Test
    public void testGenConvert() throws Exception {
        Method genConvert = XBeanUtil.class.getDeclaredMethod("genConvert", Class.class);
        genConvert.setAccessible(true);
        Object o = genConvert.invoke(null, TestBean1.class);
        assertNotNull(o);
        assertTrue(o instanceof XBeanUtil.IBeanHandler);
    }

    @Test
    public void testInit() {
        TestBean1 bean1 = XBeanUtil.entityInit(new TestBean1());
        assertNull(bean1.id);
        assertNotNull(bean1.uuid);
        assertNotNull(bean1.createTime);
        assertNull(bean1.someData);
    }

    @Test
    public void testCast() {
        TestBean1 bean1 = XBeanUtil.entityInit(new TestBean1());
        TestBeanLike1 bean2 = XBeanUtil.cast(bean1, TestBeanLike1.class);
        assertSame(bean1.id, bean2.id);
        assertSame(bean1.uuid, bean2.uuid);
        assertSame(bean1.createTime, bean2.createTime);
        assertSame(bean1.someData, bean2.someData);
    }


    @Data
    public static final class TestBean1 implements Serializable {
        private Integer id;
        private String uuid;
        private Date createTime;
        private String someData;
    }

    @Data
    public static final class TestBeanLike1 implements Serializable {
        private Integer id;
        private String uuid;
        private Date createTime;
        private String someData;
    }
}

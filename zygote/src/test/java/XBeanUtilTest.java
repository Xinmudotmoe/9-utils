import lombok.AllArgsConstructor;
import lombok.Data;
import moe.xinmu.wt.util.beanutil.XBeanUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.Map;

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

    @Test
    public void TestUnconventional() {
        TestBean1 bean1 = XBeanUtil.entityInit(TestBean1.class);
        TestBeanUnconventionalField testBeanUnconventionalField = new TestBeanUnconventionalField(XBeanUtil.convertBean(bean1));
        TestBeanLike1 like1 = XBeanUtil.cast(testBeanUnconventionalField, TestBeanLike1.class);
        assertSame(bean1.id, like1.id);
        assertSame(bean1.createTime, like1.createTime);
        assertNotNull(like1.createTime);
        assertSame(bean1.uuid, like1.uuid);
        assertNotNull(like1.uuid);
        assertSame(bean1.someData, like1.someData);
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

    @AllArgsConstructor
    @SuppressWarnings("unused")
    public static final class TestBeanUnconventionalField implements Serializable {
        Map<String, ?> o;

        public Integer getId() {
            return (Integer) o.get("id");
        }

        public String getUuid() {
            return (String) o.get("uuid");
        }

        public Date getCreateTime() {
            return (Date) o.get("createTime");
        }

        public String getSomeData() {
            return (String) o.get("someData");
        }
    }
}

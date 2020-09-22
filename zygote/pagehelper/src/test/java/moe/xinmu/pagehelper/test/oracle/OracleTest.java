package moe.xinmu.pagehelper.test.oracle;

import lombok.SneakyThrows;
import moe.xinmu.pagehelper.PageHelper;
import moe.xinmu.pagehelper.enums.DriverType;
import moe.xinmu.pagehelper.io.PageIO;
import moe.xinmu.pagehelper.test.PrivateConfiguration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;

public class OracleTest {
    static Connection connection;

    static {
        init();
    }

    @SneakyThrows
    static void init() {
        connection = PrivateConfiguration.oracleTestConnection();
    }

    @Test
    @SneakyThrows
    public void test() {
        PageIO<EMP> pageIO = new PageIO<>(EMP.class, 1, 2);
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select * from SCOTT.EMP", Collections.emptyList(), DriverType.ORACLE));
    }

    @Test
    @SneakyThrows
    public void test1() {
        PageIO<EMP> pageIO = new PageIO<>(EMP.class, 2, 2, "DEPTNO desc");
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select t.*, NULL n from SCOTT.EMP t", Collections.emptyList(), DriverType.ORACLE));
    }
}

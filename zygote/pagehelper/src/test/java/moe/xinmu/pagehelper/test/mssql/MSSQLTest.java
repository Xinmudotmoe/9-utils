package moe.xinmu.pagehelper.test.mssql;

import lombok.SneakyThrows;
import moe.xinmu.pagehelper.PageHelper;
import moe.xinmu.pagehelper.enums.DriverType;
import moe.xinmu.pagehelper.io.PageIO;
import moe.xinmu.pagehelper.test.PrivateConfiguration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;

public class MSSQLTest {
    static Connection connection;

    static {
        init();
    }

    @SneakyThrows
    static void init() {
        connection = PrivateConfiguration.mssqlTestConnection();
    }

    @Test
    @SneakyThrows
    public void test() {
        PageIO<Schemata> pageIO = new PageIO<>(Schemata.class, 1, 2);
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select * from INFORMATION_SCHEMA.SCHEMATA", Collections.emptyList(), DriverType.MSSQL));
    }

    @Test
    @SneakyThrows
    public void test1() {
        PageIO<Schemata> pageIO = new PageIO<>(Schemata.class, 2, 2, "SCHEMA_OWNER desc");
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select *,null as n from INFORMATION_SCHEMA.SCHEMATA", Collections.emptyList(), DriverType.MSSQL));
    }

}

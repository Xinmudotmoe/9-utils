package moe.xinmu.pagehelper.test.mysql;

import lombok.SneakyThrows;
import moe.xinmu.pagehelper.PageHelper;
import moe.xinmu.pagehelper.enums.DriverType;
import moe.xinmu.pagehelper.io.PageIO;
import moe.xinmu.pagehelper.test.PrivateConfiguration;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.Collections;

public class MySqlTest {
    static Connection connection;

    static {
        init();
    }

    @SneakyThrows
    static void init() {
        connection = PrivateConfiguration.mysqlTestConnection();
    }

    @Test
    @SneakyThrows
    public void test() {
        PageIO<SystemBlacklistWord> pageIO = new PageIO<>(SystemBlacklistWord.class, 1, 2);
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select * from system_sensitive_thesaurus where is_delete = ?", Collections.singletonList(0), DriverType.MYSQL));
    }

    @Test
    @SneakyThrows
    public void test1() {
        PageIO<SystemBlacklistWord> pageIO = new PageIO<>(SystemBlacklistWord.class, 2, 2, "id desc");
        System.out.println(PageHelper.pagePrepareStatement(pageIO, connection, "select *,null as n from system_sensitive_thesaurus where is_delete = ?", Collections.singletonList(0), DriverType.MYSQL));
    }
}

package moe.xinmu.pagehelper;

import lombok.SneakyThrows;
import moe.xinmu.beanutils2.BeanFlags;
import moe.xinmu.beanutils2.BusyBeanUtils;
import moe.xinmu.pagehelper.enums.DriverType;
import moe.xinmu.pagehelper.io.PageIO;
import moe.xinmu.pagehelper.result.PageResult;

import java.sql.*;
import java.util.Date;
import java.util.*;

/**
 * JDBC 分页
 *
 * @author xinmu
 * @since 2020/09/18
 */
public class PageHelper {

    @SneakyThrows
    public static <T> PageResult<T> pagePrepareStatement(PageIO<T> page, Connection connection, String sql, List<Object> parameters, DriverType driverType) {
        int total;
        PageResult<T> pageResult = new PageResult<>();
        pageResult.setSize(page.getSize());
        pageResult.setPage(page.getPage());
        parameters = new ArrayList<>(parameters);
        try (PreparedStatement statement = connection.prepareStatement(genCountSql(sql, driverType), ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            putParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.first();
                total = resultSet.getInt(1);
            }
        }
        pageResult.setTotal(total);
        int rangeStart = (page.getPage() - 1) * page.getSize();
        int rangeEnd = page.getPage() * page.getSize();

        if (total < rangeStart) {
            pageResult.setList(Collections.emptyList());
            return pageResult;
        }
        String targetSql;
        if (page.getOrder() != null && !page.getOrder().isEmpty()) {
            targetSql = genRangeSqlWithOrderBy(sql, page.getOrder(), rangeStart, rangeEnd, driverType, parameters);
        } else {
            targetSql = genRangeSql(sql, rangeStart, rangeEnd, driverType, parameters);
        }
        List<T> list = new ArrayList<>(page.getSize());

        try (PreparedStatement statement = connection.prepareStatement(targetSql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
            putParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                pageSkip(resultSet, page.getOrder(), rangeStart, driverType);
                for (Map<String, Object> map : analyze(resultSet)) {
                    if (driverType == DriverType.ORACLE) {
                        list.add(BusyBeanUtils.convertMap(map, page.getTargetClass(), BeanFlags.IGNORE_CASE, BeanFlags.DISABLE_CASE));
                    } else {
                        list.add(BusyBeanUtils.convertMap(map, page.getTargetClass()));
                    }
                }
            }
        }
        pageResult.setList(list);
        return pageResult;
    }

    static List<Map<String, Object>> analyze(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<Integer> typeList = new ArrayList<>();
        List<String> typeName = new ArrayList<>();

        List<Map<String, Object>> mapList = new ArrayList<>();
        for (int i = 0; i < metaData.getColumnCount(); i++) {
            int type = metaData.getColumnType(i + 1);
            switch (type) {
                case Types.BIGINT:
                case Types.TINYINT:
                    typeList.add(Types.INTEGER);
                    break;
                case Types.NVARCHAR:
                    typeList.add(Types.VARCHAR);
                    break;
                case Types.VARBINARY:
                case Types.LONGVARBINARY:
                    typeList.add(Types.BINARY);
                    break;
                case Types.INTEGER:
                case Types.VARCHAR:
                case Types.TIMESTAMP:
                case Types.NUMERIC:
                case Types.BINARY:
                case Types.NULL:
                    typeList.add(type);
                    break;
                default:
                    throw new RuntimeException("暂不支持的JDBC数据类型 " + type);
            }
            typeName.add(metaData.getColumnName(i + 1));
        }

        while (resultSet.next()) {
            Map<String, Object> map = new HashMap<>();
            for (int j = 0; j < typeList.size(); j++) {

                if (Objects.nonNull(resultSet.getObject(j + 1))) {
                    switch (typeList.get(j)) {
                        case Types.INTEGER:
                            map.put(typeName.get(j), resultSet.getInt(j + 1));
                            break;
                        case Types.VARCHAR:
                            map.put(typeName.get(j), resultSet.getString(j + 1));
                            break;
                        case Types.TIMESTAMP:
                            map.put(typeName.get(j), new Date(resultSet.getTimestamp(j + 1).getTime()));
                            break;
                        case Types.NUMERIC:
                            map.put(typeName.get(j), resultSet.getBigDecimal(j + 1));
                            break;
                        case Types.BINARY:
                            map.put(typeName.get(j), resultSet.getBytes(j + 1));
                            break;
                    }
                } else {
                    map.put(typeName.get(j - 1), null);
                }
            }
            mapList.add(map);
        }
        return mapList;
    }

    static void putParameters(PreparedStatement statement, List<Object> parameters) throws SQLException {
        int i = 0;
        for (Object o : parameters) {
            if (o == null) {
                statement.setNull(++i, 0);
            } else if (o instanceof Number) {
                statement.setInt(++i, ((Number) o).intValue());
            } else if (o instanceof String) {
                statement.setString(++i, o.toString());
            } else if (o instanceof Date) {
                statement.setTime(++i, new Time(((Date) o).getTime()));
            } else {
                throw new RuntimeException("暂不支持的Java数据类型 " + o.getClass());
            }
        }
    }

    static String genCountSql(String sql, DriverType driverType) {
        return String.format("select count(0) from(%s) pageHelper", sql);
    }

    static String genRangeSql(String sql, Integer rangeStart, Integer rangeEnd, DriverType driverType, List<Object> parameters) {
        switch (driverType) {
            case ORACLE:
                return String.format("select * from (select pageHelper1.*, ROWNUM pageHelperNum from(%s) pageHelper1  where ROWNUM<=%d ) pageHelper where ROWNUM>=%d", sql, rangeEnd, rangeStart + 1);
            case MSSQL:
                return String.format("select TOP %d * from (%s) pageHelper", rangeEnd, sql);
            default:
                return String.format("select * from (%s) pageHelper limit %d, %d", sql, rangeStart, rangeEnd - rangeStart);
        }
    }

    static String genRangeSqlWithOrderBy(String sql, String orderBy, Integer rangeStart, Integer rangeEnd, DriverType driverType, List<Object> parameters) {
        switch (driverType) {
            case ORACLE:
                return String.format("select * from (select pageHelper1.*, ROWNUM pageHelperNum from (%s) pageHelper1 where ROWNUM<=%d order by %s ) pageHelper where pageHelper.pageHelperNum>=%d", sql, rangeEnd, orderBy, rangeStart + 1);
            case MSSQL:
                return String.format("select * from (%s) pageHelper order by pageHelper.%s OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", sql, orderBy, rangeStart, rangeEnd - rangeStart);
            default:
                return String.format("select * from (%s) pageHelper order by %s limit %d, %d", sql, orderBy, rangeStart, rangeEnd - rangeStart);
        }
    }

    public static void pageSkip(ResultSet resultSet, String orderBy, Integer rangeStart, DriverType driverType) throws SQLException {
        switch (driverType) {
            case MSSQL:
                if (orderBy == null || orderBy.equals("")) {
                    resultSet.absolute(rangeStart);
                }
                break;
        }
    }
}

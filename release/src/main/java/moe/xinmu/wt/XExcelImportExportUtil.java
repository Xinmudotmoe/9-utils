package moe.xinmu.wt;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.io.IOUtils;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.beans.*;
import java.util.stream.Collectors;

/**
 * Excel读取、导出与模板生成器
 * 限制颇多，代码质量不高，等待重写
 *
 * @author ZhangYuhao
 */
public final class XExcelImportExportUtil<T> {
    /**
     * Bean的类对象
     * 最终会生成此类创建的对象
     */
    private final Class<T> beanType;
    /**
     * 空构造方法
     */
    private final Constructor<T> constructor;
    /**
     * 表名
     */
    private String sheetName = "Sheet1";
    /**
     * Bean的属性数组
     */
    private PropertyDescriptor[] propertyDescriptor;
    /**
     * 别名、名称映射 与propertyDescriptor的顺序一致
     */
    private String[] names;
    /**
     * 类型T的`空`数组
     */
    T[] emptyArray;

    private XExcelImportExportUtil(Class<T> beanType) throws Exception {
        this.beanType = beanType;

        BeanInfo beanInfo = Introspector.getBeanInfo(beanType);
        // 获取所有可以写入字段的写入方法
        propertyDescriptor = Arrays.stream(beanInfo.getPropertyDescriptors())
                .filter(d -> Objects.nonNull(d.getWriteMethod()))
                .filter(d -> Objects.nonNull(d.getReadMethod()))
                .filter(this::notHasIgnore)
                .toArray(PropertyDescriptor[]::new);
        // 获取`空`构造方法
        constructor = beanType.getConstructor();
        @SuppressWarnings("all")
        T[] ts = (T[]) Array.newInstance(beanType, 0);
        emptyArray = ts;
    }
    private boolean notHasIgnore(PropertyDescriptor descriptor){
        try {
            Field field = beanType.getDeclaredField(descriptor.getName());
            JsonIgnore ignore = field.getDeclaredAnnotation(JsonIgnore.class);
            return !ignore.value();
        } catch (Exception ignored) {
        }
        return true;
    }
    /**
     * 生成ExcelImportUtil
     * @param beanType 数据类型
     * @return Utils对象
     * @throws Exception
     */
    public static <T> XExcelImportExportUtil<T> custom(Class<T> beanType) throws Exception {
        Objects.requireNonNull(beanType);
        return new XExcelImportExportUtil<>(beanType).descriptorCheck();
    }

    /**
     * 对所有的写入字段方法进行检查与顺序调整
     */
    private XExcelImportExportUtil<T> descriptorCheck() {
        boolean hasList = false;
        ArrayList<PropertyDescriptor> descriptors = new ArrayList<>();
        PropertyDescriptor list = null;
        for (PropertyDescriptor descriptor : propertyDescriptor) {
            Method writeMethod = descriptor.getWriteMethod();
            // 如果出现多个传入值 则抛弃此字段/方法
            if (writeMethod.getParameterCount() != 1) {
                continue;
            }
            // 获取唯一参数属性
            Parameter parameter = writeMethod.getParameters()[0];
            Class<?> clazz = parameter.getType();
            if (clazz.isArray()) {
                // 本质上 数组类型为`可变参数类型`
                // 需要将其放置在最后一个参数位置
                // 如果出现多个数组类型 则认定此类型异常
                if (hasList) {
                    throw new RuntimeException("过多的Array类型");
                }
                hasList = true;
                checkType(clazz.getComponentType());
                list = descriptor;
            } else {
                checkType(clazz);
                // 如果不为数组类型 直接添加到参数列表内
                descriptors.add(descriptor);
            }
        }
        // 如果发现了数组类型 则将其放置在最后一个位置
        if(Objects.nonNull(list)) {
            descriptors.add(list);
        }
        propertyDescriptor = descriptors.toArray(new PropertyDescriptor[0]);
        names = new String[descriptors.size()];
        for (int i = 0; i < descriptors.size(); i++) {
            try {
                // 获取字段的别名 无论出现什么错误 直接放弃
                Field field = beanType.getDeclaredField(descriptors.get(i).getName());
                ApiModelProperty property = field.getDeclaredAnnotation(ApiModelProperty.class);
                // 获取别名 同时抛弃首尾的空格
                String nikeName=property.value().trim();
                if(nikeName.isEmpty()) {
                    throw new Exception("别名为空");
                }
                names[i] = property.value();
            } catch (Exception ignored) {
                // 获取名称 不验证字段是否存在
                names[i] = descriptors.get(i).getDisplayName();
            }
        }
        if (Sets.newHashSet(names).size() != names.length) {
            // 如果names中存在重复 则抛出异常 否则在之后的处理会出现歧义
            throw new RuntimeException("字段重名");
        }
        return this;
    }

    private static void checkType(Class<?> clazz) {
        if (!Arrays.asList(Integer.class, Double.class, String.class).contains(clazz)) {
            throw new RuntimeException("不支持的类型");
        }
    }

    /**
     * 生成2003的模板文件
     *
     * @param outputStream 数据输出流
     * @throws IOException
     */
    public void exportTemplate2003(OutputStream outputStream) throws Exception {
        export(ExcelType.EXCEL2003, outputStream, emptyArray);
    }

    /**
     * 生成2007的模板文件
     *
     * @param outputStream 数据输出流
     * @throws IOException
     */
    public void exportTemplate2007(OutputStream outputStream) throws Exception {
        export(ExcelType.EXCEL2007, outputStream, emptyArray);
    }

    /**
     * 根据data生成Excel2003格式的文件
     *
     * @param outputStream 数据输出流
     * @param data         数据源
     * @throws IOException
     */
    public void export2003(OutputStream outputStream, T[] data) throws Exception {
        export(ExcelType.EXCEL2003, outputStream, data);
    }

    /**
     * 根据data生成Excel2007格式的文件
     *
     * @param outputStream 数据输出流
     * @param data         数据源
     * @throws IOException
     */
    public void export2007(OutputStream outputStream, T[] data) throws Exception {
        export(ExcelType.EXCEL2007, outputStream, data);
    }

    /**
     * 设置表名
     *
     * @param sheetName 表明
     * @return
     */
    public XExcelImportExportUtil<T> setSheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * 生成模板文件
     *
     * @param type         Excel类型 2003或2007
     * @param outputStream 数据输出流
     * @throws IOException
     */
    private void export(ExcelType type, OutputStream outputStream, T[] data) throws Exception {
        Workbook workbook;
        switch (type) {
            case EXCEL2003:
                workbook = new HSSFWorkbook();
                break;
            case EXCEL2007:
                workbook = new XSSFWorkbook();
                break;
            default:
                throw new RuntimeException();
        }
        Sheet sheet = workbook.createSheet(sheetName);
        createHeadRow(workbook, sheet);
        writeAllData(workbook, sheet, data);
        workbook.write(outputStream);
    }

    private void createHeadRow(Workbook workbook, Sheet sheet) {
        Row headRow = sheet.createRow(0);
        CellStyle cellStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        cellStyle.setFont(font);
        headRow.setRowStyle(cellStyle);
        for (int i = 0; i < names.length; i++) {
            Cell cell = headRow.createCell(i);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(names[i]);
        }
        if (propertyDescriptor[propertyDescriptor.length - 1].getWriteMethod().getParameterTypes()[0].isArray()) {
            headRow.createCell(propertyDescriptor.length).setCellValue("...");
        }
    }

    private void writeAllData(Workbook workbook, Sheet sheet, T[] data) throws Exception {
        if (Objects.isNull(data) || data.length == 0) {
            return;
        }
        List<Method> readMethods = Arrays.stream(propertyDescriptor)
                .map(PropertyDescriptor::getReadMethod)
                .collect(Collectors.toList());
        for (int i = 0; i < data.length; i++) {
            T t = data[i];
            if (Objects.isNull(t)) {
                continue;
            }
            Row headRow = sheet.createRow(i + 1);
            for (int j = 0; j < readMethods.size(); j++) {
                Object o = readMethods.get(j).invoke(t);
                if (Objects.nonNull(o)) {
                    if (propertyDescriptor[j].getPropertyType().isArray()) {
                        int length = Array.getLength(o);
                        for (int k = 0; k < length; k++) {
                            headRow.createCell(k + j).setCellValue(Array.get(o, k).toString());
                        }
                    } else {
                        headRow.createCell(j).setCellValue(o.toString());
                    }
                }
            }
        }
    }

    /**
     * 读取Excel特定表的所有数据
     *
     * @param inputStream Excel数据流
     * @return 包装好的类型数组
     * @throws Exception
     */
    public T[] readAllData(InputStream inputStream) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            IOUtils.copy(inputStream, outputStream);
            try (ByteArrayInputStream inputStream1 = new ByteArrayInputStream(outputStream.toByteArray())) {
                Workbook workbook;
                ExcelType type = null;
                if (POIFSFileSystem.hasPOIFSHeader(inputStream1)) {
                    type = ExcelType.EXCEL2003;
                }
                if (POIXMLDocument.hasOOXMLHeader(inputStream1)) {
                    type = ExcelType.EXCEL2007;
                }
                if (Objects.isNull(type)) {
                    throw new RuntimeException("不支持的文件类型");
                }
                switch (type) {
                    case EXCEL2003:
                        workbook = new HSSFWorkbook(inputStream1);
                        break;
                    case EXCEL2007:
                        workbook = new XSSFWorkbook(inputStream1);
                        break;
                    default:
                        throw new RuntimeException();
                }
                Sheet sheet = workbook.getSheet(sheetName);
                Objects.requireNonNull(sheet,"未找到此表");
                Row headRow = sheet.getRow(0);
                // 名称索引映射
                Integer[] indexMap = new Integer[headRow.getLastCellNum()];
                List<String> names = Arrays.asList(this.names);
                // 读取头部数据 产生映射关系
                for (int i = 0; i < indexMap.length; i++) {
                    int index = names.lastIndexOf(readDate(String.class, headRow.getCell(i)));
                    indexMap[i] = index;
                }
                if (propertyDescriptor[propertyDescriptor.length - 1].getPropertyType().isArray()) {
                    int index1 = Arrays.asList(indexMap).indexOf(propertyDescriptor.length - 1);
                    // 检查最后一个列是否为数组列 如果没有数组列，则忽略
                    if (index1 != -1 && index1 != indexMap.length - 1) {
                        // 如果数组列不是最后一个有效列 则Excel头部错误
                        for (int i = index1 + 1; i < indexMap.length; i++) {
                            if (!indexMap[i].equals(-1)) {
                                throw new RuntimeException("Excel头部格式错误");
                            }
                        }
                    }
                }
                // 读取行数据
                List<T> datas = new ArrayList<>();
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    if (Objects.isNull(row)) {
                        continue;
                    }
                    T t = constructor.newInstance();
                    for (int j = 0; j < indexMap.length; j++) {
                        int index = indexMap[j];
                        if (index == -1) {
                            // 关联映射关系代码 如果未找到映射 则映射值为-1 放弃本元件的数据
                            continue;
                        }
                        PropertyDescriptor descriptors = propertyDescriptor[index];
                        if (!descriptors.getPropertyType().isArray()) {
                            Cell cell = row.getCell(j);
                            descriptors.getWriteMethod().invoke(t, readDate(descriptors.getPropertyType(), cell));
                        } else {
                            // 如果出现数组类型 无论执行情况如何 均结束本行的处理
                            int len = row.getLastCellNum() - j;
                            if (len < 0) {
                                break;
                            }
                            Class<?> componentType = descriptors.getPropertyType().getComponentType();
                            Object list = Array.newInstance(componentType, len);
                            for (int k = 0; k < len; k++) {
                                Cell cell = row.getCell(j + k);
                                Array.set(list, k, readDate(componentType, cell));
                            }
                            descriptors.getWriteMethod().invoke(t, list);
                            break;
                        }
                    }
                    datas.add(t);
                }
                return datas.toArray(emptyArray);
            }
        }
    }

    @SuppressWarnings("all")
    private static <T> T readDate(Class<T> type, Cell cell) {
        if (Objects.isNull(cell)) {
            return null;
        }
        String value;
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC:
                value = String.valueOf(cell.getNumericCellValue());
                break;
            case Cell.CELL_TYPE_STRING:
            case Cell.CELL_TYPE_FORMULA:
                value = cell.getStringCellValue();
                break;
            case Cell.CELL_TYPE_BLANK:
            case Cell.CELL_TYPE_ERROR:
                value = "";
                break;
            case Cell.CELL_TYPE_BOOLEAN:
                value = String.valueOf(cell.getBooleanCellValue());
                break;
            default:
                throw new RuntimeException(String.format("未知类型 %d", cell.getCellType()));
        }

        switch (type.getTypeName()) {
            case "java.lang.String":
                return (T) value;
            case "java.lang.Double":
                return (T) Double.valueOf(Double.parseDouble(value));
            case "java.lang.Integer":
                return (T) (Integer.valueOf(Double.valueOf(Double.parseDouble(value)).intValue()));
            default:
                throw new RuntimeException();
        }
    }

    private enum ExcelType {
        /**
         * Excel2003及以前
         */
        EXCEL2003,
        /**
         * Excel2007及以后
         */
        EXCEL2007
    }
}


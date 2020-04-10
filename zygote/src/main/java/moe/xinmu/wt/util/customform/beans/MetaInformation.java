package moe.xinmu.wt.util.customform.beans;

import lombok.Data;

import java.util.List;

/**
 * 元信息数据类
 */
@Data
public class MetaInformation {
    /**
     * 元信息版本
     */
    byte version;
    /**
     * 元信息数据块
     */
    List<MetaConstant> constantList;
}

package moe.xinmu.wt.util.customform.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 元信息常量块，用于描述信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetaConstant {
    /**
     * 元信息类型
     */
    MetaType type;
    /**
     * 标题
     */
    String name;
    /**
     * 宽度
     */
    Integer width;
}

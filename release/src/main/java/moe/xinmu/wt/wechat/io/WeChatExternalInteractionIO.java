package moe.xinmu.wt.wechat.io;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class WeChatExternalInteractionIO {
    @ApiModelProperty("用户unionId")
    String unionId;
    @ApiModelProperty("用户OpenId")
    String openid;
    @ApiModelProperty("用户昵称")
    String nickname;
    @ApiModelProperty(value = "用户性别", example = "1")
    String sex;
    @ApiModelProperty(value = "用户省份信息", example = "河北")
    String province;
    @ApiModelProperty(value = "用户城市信息", example = "廊坊")
    String city;
    @ApiModelProperty(value = "用户所在国家信息", example = "中国")
    String country;
    @ApiModelProperty("用户头像地址")
    String headimgurl;

}

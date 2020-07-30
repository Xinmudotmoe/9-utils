package moe.xinmu.wt.wechat.io;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Objects;
@Data
public class WeChatUserInfoIO {
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

    private Integer errcode;
    private String errmsg;

    public boolean isErr() {
        if (Objects.isNull(errcode)) {
            return false;
        }
        if (errcode <= 40000) {
            return false;
        }
        return true;
    }

}

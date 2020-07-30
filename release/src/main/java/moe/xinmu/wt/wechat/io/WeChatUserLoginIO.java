package moe.xinmu.wt.wechat.io;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Objects;

@Data
public class WeChatUserLoginIO {
    @ApiModelProperty("接口调用凭证")
    String accessToken;
    @ApiModelProperty("凭证超时时间")
    Integer expiresIn;
    @ApiModelProperty("用户刷新access_token")
    String refreshToken;
    @ApiModelProperty("授权用户唯一标识")
    String openid;
    @ApiModelProperty("用户授权的作用域，使用逗号（,）分隔")
    String scope;
    @ApiModelProperty("unionid")
    String unionid;
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

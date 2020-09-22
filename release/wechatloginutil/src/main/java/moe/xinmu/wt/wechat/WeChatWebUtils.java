package moe.xinmu.wt.wechat;

import moe.xinmu.wt.XBeanUtil;
import moe.xinmu.wt.wechat.io.WeChatExternalInteractionIO;
import moe.xinmu.wt.wechat.io.WeChatUserInfoIO;
import moe.xinmu.wt.wechat.io.WeChatUserLoginIO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信 Web登录相关
 *
 * @author Xinmu
 */
@Component
@ConfigurationProperties(prefix = "wechat.web")
public class WeChatWebUtils  extends WeChatConfiguration {

    public WeChatExternalInteractionIO getUserInfo(String code) {
        WeChatUserLoginIO io = WeChatRequestUtils.getUserToken(this, code);
        if (io.isErr()) {
            throw new RuntimeException("登录、绑定失败");
        }
        WeChatUserInfoIO io1 = WeChatRequestUtils.getUserInfo(io.getAccessToken(), io.getOpenid());
        if (io1.isErr()) {
            throw new RuntimeException("登录、绑定失败");
        }
        WeChatExternalInteractionIO io2 = XBeanUtil.cast(io1, WeChatExternalInteractionIO.class);
        io2.setUnionId(io.getUnionid());
        return io2;
    }
}

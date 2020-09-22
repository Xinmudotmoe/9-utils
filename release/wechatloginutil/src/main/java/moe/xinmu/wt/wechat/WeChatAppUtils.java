package moe.xinmu.wt.wechat;

import moe.xinmu.wt.XBeanUtil;
import moe.xinmu.wt.wechat.io.WeChatExternalInteractionIO;
import moe.xinmu.wt.wechat.io.WeChatUserInfoIO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "wechat.app")
public class WeChatAppUtils extends WeChatConfiguration {

    public WeChatExternalInteractionIO getUserInfo(String accessToken, String openId, String unionId) {
        WeChatUserInfoIO io1 = WeChatRequestUtils.getUserInfo(accessToken, openId);
        if (io1.isErr()) {
            throw new RuntimeException("登录、绑定失败");
        }
        WeChatExternalInteractionIO io2 = XBeanUtil.cast(io1, WeChatExternalInteractionIO.class);
        io2.setUnionId(unionId);
        return io2;
    }

}

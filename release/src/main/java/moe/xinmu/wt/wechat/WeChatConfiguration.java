package moe.xinmu.wt.wechat;

import lombok.Data;
import lombok.Setter;

@Data
public abstract class WeChatConfiguration {
    protected String appId;
    protected String appSecret;
}

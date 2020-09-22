package moe.xinmu.wt.wechat;

import lombok.Data;

@Data
public abstract class WeChatConfiguration {
    protected String appId;
    protected String appSecret;
}

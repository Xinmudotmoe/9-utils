package moe.xinmu.wt.wechat;


import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import moe.xinmu.wt.wechat.io.WeChatUserInfoIO;
import moe.xinmu.wt.wechat.io.WeChatUserLoginIO;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Http Request 自动填装方法
 *
 * @author Xinmu
 */
public class WeChatRequestUtils {

    /**
     * 微信，令牌获取URL
     * 通过AppID Code 来获取用户OpenId
     */
    public static final String WECHAT_ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    /**
     * 微信 用户信息获取
     * 通过AppID OpenId 来获取用户信息
     */
    public static final String WECHAT_USERINFO_URL = "https://api.weixin.qq.com/sns/userinfo";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WeChatRequestUtils.class);

    public static WeChatUserInfoIO getUserInfo(String accessToken, String openId) {
        Map<String, String> params = Maps.newLinkedHashMap();
        params.put("access_token", accessToken);
        params.put("openid", openId);
        params.put("lang", "zh_CN");
        return urlGetRequest(WECHAT_USERINFO_URL, params, WeChatUserInfoIO.class);
    }

    public static WeChatUserLoginIO getUserToken(WeChatConfiguration configuration, String code) {
        Map<String, String> params = Maps.newLinkedHashMap();
        params.put("appid", configuration.getAppId());
        params.put("secret", configuration.getAppSecret());
        params.put("code", code);
        params.put("grant_type", "authorization_code");
        return urlGetRequest(WECHAT_ACCESS_TOKEN_URL, params, WeChatUserLoginIO.class);
    }

    public static <T> T urlGetRequest(String url, Map<String, String> params, Class<T> tClass) {
        try {
            StringJoiner joiner = new StringJoiner("&");
            for (String key : params.keySet()) {
                joiner.add(key + "=" + URLEncoder.encode(params.get(key), "UTF-8"));
            }
            StringBuilder param = new StringBuilder(joiner.toString());
            param.insert(0, '?');
            param.insert(0, url);
            HttpURLConnection connection = (HttpURLConnection) new URL(param.toString()).openConnection();
            connection.setRequestMethod("GET");
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                String response = new String(IOUtils.toByteArray(connection.getInputStream()), StandardCharsets.UTF_8);
                log.info(response);
                return JSONObject.parseObject(response, tClass);
            } else {
                throw new RuntimeException("请求失败" + connection.getResponseCode());
            }
        } catch (Exception e) {
            log.error("ConnectionError", e);
            throw new RuntimeException();
        }
    }
}

package com.atguigu.gmall.gateway.filter;

import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 自定义局部过滤器(做到定点拦截):
 * 1. 需要编写过滤器工厂类继承AbstractGatewayFilterFactory抽象类
 * 2. 在需要过滤的微服务路由中配置该过滤器
 */
@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    // 一定要重写构造方法
    // 告诉父类，这里使用PathConfig对象接收配置内容
    public AuthGatewayFilterFactory(){
        super(PathConfig.class);
    }

    @Override
    public GatewayFilter apply(PathConfig config) {
        // 实现GatewaFilter接口
        return (exchange, chain) -> {
            System.out.println("自定义过滤器！" + config);

            // ServerHttpRequest 与 HttpServletRequest 都实现了http协议，数据模型相同
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // 1.判断当前请求路径在不在名单中，不在直接放行
            String path = request.getURI().getPath();
            if (config.authPaths.stream().allMatch(authPath -> path.indexOf(authPath) == -1)){
                return chain.filter(exchange); // 放行
            }

            // 2.获取token信息：同步请求cookie中获取，异步请求头信息中获取
            String token = request.getHeaders().getFirst("token");
            // 头信息没有，就从cookie中尝试获取
            if (StringUtils.isEmpty(token)) {
                MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                    token = cookies.getFirst(jwtProperties.getCookieName()).getValue();
                }
            }

            // 3.判断token是否为空，为空直接拦截
            if (StringUtils.isEmpty(token)) {
                // 重定向到登录
                // 303状态码表示由于请求对应的资源存在着另一个URI，应使用重定向获取请求的资源
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,
                        "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                return response.setComplete();
            }

            try {
                // 4.解析jwt，有异常直接拦截
                Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());

                // 5.判断ip
                String ip = map.get("ip").toString();
                String curIp = IpUtil.getIpAddressAtGateway(request);
                if (!StringUtils.equals(ip, curIp)) {
                    // 重定向到登录
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }

                // 6.传递登录信息给后续的服务，不需要再次解析jwt
                // 将userId转变成request对象。mutate：转变的意思
                request.mutate().header("userId", map.get("userId").toString()).build();
                // 将新的request对象转变成exchange对象
                exchange.mutate().request(request).build();

            } catch (Exception e) {
                e.printStackTrace();
                // 重定向登录
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                return response.setComplete();
            }

            // 7.放行
            return chain.filter(exchange);
        };
    }

    /**
     * 指定字段顺序
     * 可以通过不同的字段分别读取：/toLogin.html,/login
     * 在这里希望通过一个集合字段读取所有的路径
     * @return
     */
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("authPaths");
    }

    /**
     * 指定读取字段的结果集类型
     * 默认通过map的方式，把配置读取到不同字段
     *  例如：/toLogin.html,/login
     *      由于只指定了一个字段，只能接收/toLogin.html
     * @return
     */
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    /**
     * 读取配置的内部类
     */
    @Data
    public static class PathConfig{
        private List<String> authPaths;
    }
}

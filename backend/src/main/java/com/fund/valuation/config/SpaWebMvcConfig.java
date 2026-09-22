package com.fund.valuation.config;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * SPA (单页面应用) HTML5 History 路由转发支持:
 * 当浏览器刷新 /login、/fund/110022 等非真实静态文件与非 /api 路由时，
 * 自动统一 fallback 转发至 index.html，由 React Router 接管前端路由，彻底杜绝 404 与“内部错误”。
 */
@Component
public class SpaWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        // 如果真实静态资源存在且可读（如 .js, .css, .svg），直接返回
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // API 路径不拦截，交给 Controller 正常处理或报错
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("api")) {
                            return null;
                        }
                        // 其余所有前端自留页面路由全部返回 index.html
                        return location.createRelative("index.html");
                    }
                });
    }
}

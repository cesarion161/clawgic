package com.clawgic.clawgic.web;

import com.clawgic.clawgic.config.ClawgicRuntimeProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers a warn-only interceptor for legacy APIs so Clawgic demos
 * remain on the intended path without disabling existing endpoints yet.
 */
@Configuration
@ConditionalOnBean(ClawgicRuntimeProperties.class)
public class LegacyRouteWarningConfig implements WebMvcConfigurer {

    private final ClawgicRuntimeProperties clawgicRuntimeProperties;

    public LegacyRouteWarningConfig(ClawgicRuntimeProperties clawgicRuntimeProperties) {
        this.clawgicRuntimeProperties = clawgicRuntimeProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LegacyRouteWarningInterceptor(clawgicRuntimeProperties))
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/clawgic/**");
    }
}

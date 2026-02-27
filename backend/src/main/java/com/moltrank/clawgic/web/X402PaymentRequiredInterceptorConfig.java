package com.moltrank.clawgic.web;

import com.moltrank.clawgic.config.X402Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers x402 challenge enforcement for Clawgic tournament entry routes.
 */
@Configuration
@ConditionalOnBean(X402Properties.class)
public class X402PaymentRequiredInterceptorConfig implements WebMvcConfigurer {

    private final X402Properties x402Properties;

    public X402PaymentRequiredInterceptorConfig(X402Properties x402Properties) {
        this.x402Properties = x402Properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new X402PaymentRequiredInterceptor(x402Properties))
                .addPathPatterns("/api/clawgic/tournaments/*/enter");
    }
}

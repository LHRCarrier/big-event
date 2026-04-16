package com.bubbles.server.config;

import com.bubbles.common.json.JacksonObjectMapper;
import com.bubbles.server.interceptor.JwtTokenAdminInterceptor;
import com.bubbles.server.interceptor.JwtTokenUserInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



/**
 * 配置类，注册web层相关组件
 */
@Configuration
@Slf4j
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor;
    @Autowired
    private JwtTokenUserInterceptor jwtTokenUserInterceptor;

    /**
     * 注册自定义拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器...");
        registry.addInterceptor(jwtTokenAdminInterceptor)
                .addPathPatterns("/admin/**")
                .excludePathPatterns("/admin/employee/login");
        registry.addInterceptor(jwtTokenUserInterceptor)
                .addPathPatterns("/user/**", "/article/**")
                .excludePathPatterns("/user/user/register", "/user/user/login");
    }


    /**
     * 静态资源配置（如果使用 springdoc，通常不需要手动配置）
     * 但若需要其他静态资源，可在此添加
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        log.info("开始设置静态资源映射...");
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }

    /**
     * 全局 OpenAPI 文档信息（标题、版本、描述等）
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // 定义管理员认证方案
        SecurityScheme adminSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("token")
                .in(SecurityScheme.In.HEADER);
        
        // 定义用户认证方案
        SecurityScheme userSecurityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .name("Authentication")
                .in(SecurityScheme.In.HEADER);
        
        // 添加认证要求
        SecurityRequirement adminSecurityRequirement = new SecurityRequirement().addList("adminAuth");
        SecurityRequirement userSecurityRequirement = new SecurityRequirement().addList("userAuth");
        
        return new OpenAPI()
                .info(new Info()
                        .title("大事件项目接口文档")
                        .version("2.0")
                        .description("大事件项目接口文档"))
                .addSecurityItem(adminSecurityRequirement)
                .addSecurityItem(userSecurityRequirement)
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("adminAuth", adminSecurityScheme)
                        .addSecuritySchemes("userAuth", userSecurityScheme));
    }

    /**
     * 用户端接口分组
     */
    @Bean
    public GroupedOpenApi userApi() {
        return GroupedOpenApi.builder()
                .group("用户端接口")
                .packagesToScan("com.bubbles.server.controller.user")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 管理端接口分组
     */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("管理端接口")
                .packagesToScan("com.bubbles.server.controller.admin")
                .pathsToMatch("/**")
                .build();
    }

    /**
     * 全局自定义 ObjectMapper，Spring Boot 会自动将其注入到消息转换器中
     * 无需手动 extendMessageConverters，避免直接操作可能弃用的 MappingJackson2HttpMessageConverter
     */
    @Bean
    public ObjectMapper objectMapper() {
        log.info("创建自定义 ObjectMapper...");
        return new JacksonObjectMapper();
    }

    /** TODO
     * 配置 CORS 跨域请求支持
     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        log.info("开始配置 CORS 跨域请求支持...");
//        registry.addMapping("/**")
//
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }
}

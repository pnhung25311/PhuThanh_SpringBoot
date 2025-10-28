package com.example.apiServer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // URL public
        registry.addResourceHandler("/images/**")
                // Đường dẫn thật trên ổ đĩa (phải có "file:" + dấu / ở cuối)
                .addResourceLocations("file:D:/Data/Storage/images/");
    }
}

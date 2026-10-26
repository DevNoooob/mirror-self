package com.mirrorself;

import com.mirrorself.config.MirrorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({MirrorProperties.class, com.mirrorself.config.ExtractionProperties.class})
public class MirrorSelfApplication {
    public static void main(String[] args) {
        SpringApplication.run(MirrorSelfApplication.class, args);
    }
}

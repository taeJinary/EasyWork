package com.taskflow.backend.global.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
public class S3AttachmentStorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.attachment.storage.type", havingValue = "s3")
    public S3Client s3Client(
            @Value("${app.attachment.storage.s3.region}") String region,
            @Value("${app.attachment.storage.s3.endpoint:}") String endpoint,
            @Value("${app.attachment.storage.s3.path-style-access:false}") boolean pathStyleAccess
    ) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(region))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(pathStyleAccess)
                        .build());

        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}

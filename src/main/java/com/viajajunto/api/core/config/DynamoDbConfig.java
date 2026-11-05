package com.viajajunto.api.core.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    // Vazio = DynamoDB real da AWS. O emulador local (localhost:4566) só é configurado no perfil "dev".
    @Value("${aws.dynamodb.endpoint:}")
    private String endpoint;

    // Credenciais obrigatórias, sem valor padrão: vêm de AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY.
    @Value("${aws.dynamodb.access-key-id}")
    private String accessKeyId;

    @Value("${aws.dynamodb.secret-access-key}")
    private String secretAccessKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)));

        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}

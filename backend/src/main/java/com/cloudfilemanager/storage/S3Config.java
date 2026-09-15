package com.cloudfilemanager.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class S3Config {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.s3.mock:true}")
    private boolean mock;

    // Internal endpoint the backend itself uses to reach S3/MinIO (e.g. http://minio:9000
    // on the docker-compose network). Empty means "use real AWS's default endpoint".
    @Value("${aws.s3.endpoint:}")
    private String endpoint;

    // The endpoint embedded into presigned URLs, which must be reachable by whoever
    // redeems the URL (typically a browser on the host, e.g. http://localhost:9000) -
    // this is deliberately different from the internal endpoint above when running
    // against a containerized S3-compatible store like MinIO.
    @Value("${aws.s3.public-endpoint:}")
    private String publicEndpoint;

    @Value("${aws.s3.path-style:false}")
    private boolean pathStyleAccess;

    @Bean
    public S3Client s3Client() {
        if (mock) {
            return new FakeS3();
        }
        var builder = S3Client.builder().region(Region.of(awsRegion));
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }
        if (pathStyleAccess) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder().region(Region.of(awsRegion));
        String effectiveEndpoint = !publicEndpoint.isBlank() ? publicEndpoint : endpoint;
        if (!effectiveEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(effectiveEndpoint));
        }
        if (pathStyleAccess) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }
        return builder.build();
    }
}

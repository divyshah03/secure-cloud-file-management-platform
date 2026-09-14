package com.cloudfilemanager.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Service
public class S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public void putObject(String bucketName, String key, byte[] file) {
        try {
            PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.putObject(objectRequest, RequestBody.fromBytes(file));
            logger.debug("Successfully uploaded file to S3: {}/{}", bucketName, key);
        } catch (Exception e) {
            logger.error("Failed to upload file to S3: {}/{}", bucketName, key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public byte[] getObject(String bucketName, String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            byte[] fileBytes = response.readAllBytes();
            response.close();
            
            logger.debug("Successfully retrieved file from S3: {}/{}", bucketName, key);
            return fileBytes;
        } catch (NoSuchKeyException e) {
            logger.warn("File not found in S3: {}/{}", bucketName, key);
            throw new com.cloudfilemanager.common.exception.ResourceNotFoundException(
                    "File not found: " + key);
        } catch (IOException e) {
            logger.error("Failed to read file from S3: {}/{}", bucketName, key, e);
            throw new RuntimeException("Failed to read file from S3", e);
        } catch (Exception e) {
            logger.error("Unexpected error retrieving file from S3: {}/{}", bucketName, key, e);
            throw new RuntimeException("Failed to retrieve file from S3", e);
        }
    }

    public void deleteObject(String bucketName, String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            logger.debug("Successfully deleted file from S3: {}/{}", bucketName, key);
        } catch (Exception e) {
            logger.error("Failed to delete file from S3: {}/{}", bucketName, key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }
}

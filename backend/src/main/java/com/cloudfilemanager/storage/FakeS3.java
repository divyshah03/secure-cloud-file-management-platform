package com.cloudfilemanager.storage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FakeS3 implements S3Client {

    private static final Logger logger = LoggerFactory.getLogger(FakeS3.class);
    private static final String PATH = System.getProperty("user.home") + "/.filemanager/s3";

    static {
        try {
            Files.createDirectories(Paths.get(PATH));
            logger.info("Created FakeS3 storage directory: {}", PATH);
        } catch (IOException e) {
            logger.error("Failed to create FakeS3 storage directory: {}", PATH, e);
        }
    }

    @Override
    public String serviceName() {
        return "FakeS3";
    }

    @Override
    public void close() {
        // No-op for fake implementation
    }

    @Override
    public PutObjectResponse putObject(PutObjectRequest putObjectRequest, RequestBody requestBody)
            throws AwsServiceException, SdkClientException {
        try {
            InputStream inputStream = requestBody.contentStreamProvider().newStream();
            byte[] bytes = IOUtils.toByteArray(inputStream);
            
            File targetFile = new File(buildObjectFullPath(putObjectRequest.bucket(), putObjectRequest.key()));
            File parentDir = targetFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            
            FileUtils.writeByteArrayToFile(targetFile, bytes);
            logger.debug("FakeS3: Stored file at {}", targetFile.getAbsolutePath());
            return PutObjectResponse.builder().build();
        } catch (IOException e) {
            logger.error("FakeS3: Failed to store file", e);
            throw new RuntimeException("Failed to store file in FakeS3", e);
        }
    }

    @Override
    public ResponseInputStream<GetObjectResponse> getObject(GetObjectRequest getObjectRequest)
            throws AwsServiceException, SdkClientException {
        try {
            String fullPath = buildObjectFullPath(getObjectRequest.bucket(), getObjectRequest.key());
            File file = new File(fullPath);
            
            if (!file.exists()) {
                throw NoSuchKeyException.builder()
                        .message("File not found: " + getObjectRequest.key())
                        .build();
            }
            
            FileInputStream fileInputStream = new FileInputStream(file);
            logger.debug("FakeS3: Retrieved file from {}", fullPath);
            return new ResponseInputStream<>(GetObjectResponse.builder().build(), fileInputStream);
        } catch (FileNotFoundException e) {
            logger.warn("FakeS3: File not found: {}", getObjectRequest.key());
            throw NoSuchKeyException.builder()
                    .message("File not found: " + getObjectRequest.key())
                    .build();
        }
    }

    @Override
    public DeleteObjectResponse deleteObject(DeleteObjectRequest deleteObjectRequest)
            throws AwsServiceException, SdkClientException {
        try {
            String fullPath = buildObjectFullPath(deleteObjectRequest.bucket(), deleteObjectRequest.key());
            File file = new File(fullPath);
            
            if (file.exists()) {
                boolean deleted = file.delete();
                logger.debug("FakeS3: Deleted file at {}: {}", fullPath, deleted ? "success" : "failed");
            } else {
                logger.warn("FakeS3: File does not exist: {}", fullPath);
            }
            
            return DeleteObjectResponse.builder().build();
        } catch (Exception e) {
            logger.error("FakeS3: Failed to delete file", e);
            throw new RuntimeException("Failed to delete file from FakeS3", e);
        }
    }

    private String buildObjectFullPath(String bucketName, String key) {
        return PATH + "/" + bucketName + "/" + key;
    }
}

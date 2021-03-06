package com.sit.cloudnative.MaterialService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AmazonService {

    private AmazonS3 s3client;

    @Value("${amazon.bucketName}")
    private String bucketName;

    @Value("${amazon.accessKey}")
    private String accessKey;

    @Value("${amazon.secretKey}")
    private String secretKey;

    @Value("${amazon.regions}")
    private String region;

    @Value("${amazon.endpointUrl}")
    private String endpointUrl;

    Logger logger = LoggerFactory.getLogger(MaterialController.class);

    public AmazonService() {
    }

    @PostConstruct
    private void initializeAmazon() {
        AWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
        this.s3client = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(region)
                .build();
    }

    public void uploadFile(String fileKey, File file) {
        s3client.putObject(new PutObjectRequest(bucketName, fileKey, file));
        file.delete();
    }

    public ByteArrayOutputStream downloadFile(String fileKey) {
        try {
            S3Object s3object = s3client.getObject(new GetObjectRequest(bucketName, fileKey));

            InputStream is = s3object.getObjectContent();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int len;
            byte[] buffer = new byte[4096];
            while ((len = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos;
        } catch (IOException ioe) {
            logger.error("IOException: " + ioe.getMessage());
        } catch (AmazonServiceException ase) {
            logger.info("sCaught an AmazonServiceException from GET requests, rejected reasons:");
            logger.info("Error Message:    " + ase.getMessage());
            logger.info("HTTP Status Code: " + ase.getStatusCode());
            logger.info("AWS Error Code:   " + ase.getErrorCode());
            logger.info("Error Type:       " + ase.getErrorType());
            logger.info("Request ID:       " + ase.getRequestId());
            throw ase;
        } catch (AmazonClientException ace) {
            logger.info("Caught an AmazonClientException: ");
            logger.info("Error Message: " + ace.getMessage());
            throw ace;
        }
        return null;
    }

    public void deleteFile(String fileKey) {
        s3client.deleteObject(new DeleteObjectRequest(bucketName, fileKey));
    }

    public List<String> listFiles() {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest()
                .withBucketName(bucketName);
        List<String> keys = new ArrayList<>();
        ObjectListing objects = s3client.listObjects(listObjectsRequest);
        while (true) {
            List<S3ObjectSummary> summaries = objects.getObjectSummaries();
            if (summaries.size() < 1) {
                break;
            }
            for (S3ObjectSummary item : summaries) {
                if (!item.getKey().endsWith("/")) {
                    keys.add(item.getKey());
                }
            }
            objects = s3client.listNextBatchOfObjects(objects);
        }
        return keys;
    }

}

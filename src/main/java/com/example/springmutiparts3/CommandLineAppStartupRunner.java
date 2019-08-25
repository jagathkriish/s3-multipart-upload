package com.example.springmutiparts3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.apigateway.model.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.websocket.EndpointConfig;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

@Component
@Slf4j
public class CommandLineAppStartupRunner implements CommandLineRunner {
    @Override
    public void run(String...args) throws Exception {
        log.info("Application started running with command-line arguments: {} . \n To kill this application, press Ctrl + C.", Arrays.toString(args));
        String existingBucketName = args[1];
        String keyName = args[2];
        String filePath = args[3];

        AWSCredentials credentials;

        credentials = new BasicAWSCredentials(
                args[4],
                args[5]
        );

        AmazonS3 amazonS3 = AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withRegion(Regions.US_EAST_2)
                .build();

        amazonS3.setEndpoint(args[0]);

        int maxUploadThreads = 5;

        TransferManager tm = TransferManagerBuilder
                .standard()
                .withS3Client(amazonS3)
                .withMultipartUploadThreshold((long) (5 * 1024 * 1024))
                .withExecutorFactory(() -> Executors.newFixedThreadPool(maxUploadThreads))
                .build();

        ProgressListener progressListener =
                progressEvent -> System.out.println("Transferred bytes: " + progressEvent.getBytesTransferred());

        PutObjectRequest request = new PutObjectRequest(existingBucketName, keyName, new File(filePath));

        request.setGeneralProgressListener(progressListener);

        Upload upload = tm.upload(request);

        try {
            upload.waitForCompletion();
            System.out.println("Upload complete.");
        } catch (AmazonClientException e) {
            System.out.println("Error occurred while uploading file");
            e.printStackTrace();
        }
    }
}
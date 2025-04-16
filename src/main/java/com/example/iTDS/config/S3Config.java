package com.example.iTDS.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class S3Config {

    @Bean
    public AmazonS3 amazonS3() {
        // Replace these with your actual AWS credentials
        String accessKey = "AKIAXZEFIEVLRE2GPXH3";
        String secretKey = "egfgZraJ9CyPUe8UqEGkkWsB+Cwm8UUut66HA7bT";
        String region = "eu-north-1"; // e.g., "us-east-1"

        BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        return AmazonS3ClientBuilder
                .standard()
                .withRegion(region)
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }
}


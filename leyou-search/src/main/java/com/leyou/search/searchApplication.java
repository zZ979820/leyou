package com.leyou.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Author Administrator
 * @Date 2020/3/8
 **/
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class searchApplication {
    public static void main(String[] args) {
        SpringApplication.run(searchApplication.class);
    }
}

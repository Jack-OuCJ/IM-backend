package com.tencent.im.backend.user;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;
import org.mybatis.spring.annotation.MapperScan;

/**
 * 用户服务启动类
 *
 * @author IM Backend Team
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.tencent.im.backend.user.mapper")
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
    
}

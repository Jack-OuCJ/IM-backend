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

/**
 * 用户服务启动类
 *
 * @author IM Backend Team
 */
@SpringBootApplication
@EnableDiscoveryClient
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    /**
     * 修复MyBatis-Plus在Spring Boot 3.x中的兼容性问题
     */
    @Bean
    public static BeanFactoryPostProcessor mybatisFixBeanFactoryPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
                String[] beanNames = beanFactory.getBeanDefinitionNames();
                for (String beanName : beanNames) {
                    BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                    if (beanDefinition.hasAttribute("factoryBeanObjectType")) {
                        Object factoryBeanObjectType = beanDefinition.getAttribute("factoryBeanObjectType");
                        if (factoryBeanObjectType instanceof String) {
                            try {
                                Class<?> clazz = Class.forName((String) factoryBeanObjectType);
                                beanDefinition.setAttribute("factoryBeanObjectType", ResolvableType.forClass(clazz));
                            } catch (ClassNotFoundException e) {
                                beanDefinition.removeAttribute("factoryBeanObjectType");
                            }
                        }
                    }
                }
            }
        };
    }
}

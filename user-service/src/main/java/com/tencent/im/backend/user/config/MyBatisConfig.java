package com.tencent.im.backend.user.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.lang.NonNull;

/**
 * MyBatis配置类
 * 修复Spring Boot 3.x与MyBatis-Plus的兼容性问题
 *
 * @author IM Backend Team
 */
@Configuration
public class MyBatisConfig implements BeanFactoryPostProcessor, Ordered {
    
    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 修复MyBatis-Plus在Spring Boot 3.x中的factoryBeanObjectType问题
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
                        // 如果找不到类，移除这个属性
                        beanDefinition.removeAttribute("factoryBeanObjectType");
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        // 确保这个后处理器在MyBatis-Plus的后处理器之前执行
        return Ordered.HIGHEST_PRECEDENCE;
    }
}

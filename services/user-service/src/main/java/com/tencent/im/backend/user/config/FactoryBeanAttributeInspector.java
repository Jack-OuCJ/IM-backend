package com.tencent.im.backend.user.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
// import org.springframework.stereotype.Component; - 临时注释掉

// @Component - 临时注释掉以避免启动问题
public class FactoryBeanAttributeInspector implements BeanFactoryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(FactoryBeanAttributeInspector.class);

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        for (String name : beanFactory.getBeanDefinitionNames()) {
            BeanDefinition bd = beanFactory.getBeanDefinition(name);
            Object attr = bd.getAttribute("factoryBeanObjectType");
            if (attr != null && !(attr instanceof ResolvableType) && !(attr instanceof Class<?>)) {
                log.error("Bean '{}' has invalid factoryBeanObjectType: {} ({}) source={}",
                        name, attr, attr.getClass().getName(), bd.getSource());
            }
        }
    }
}
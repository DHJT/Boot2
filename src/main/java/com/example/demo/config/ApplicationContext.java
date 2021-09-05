package com.example.demo.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.AbstractApplicationContext;

public class ApplicationContext extends AbstractApplicationContext {

    @Override
    protected void refreshBeanFactory() throws BeansException, IllegalStateException {
        // TODO Auto-generated method stub

    }

    @Override
    protected void closeBeanFactory() {
        // TODO Auto-generated method stub

    }

    @Override
    public ConfigurableListableBeanFactory getBeanFactory() throws IllegalStateException {
        // TODO Auto-generated method stub
        return null;
    }

}

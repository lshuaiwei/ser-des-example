package com.git.lee.serde.example;

import com.git.lee.serde.example.guice.GuiceInstanceFactory;

/**
 * @author LISHUAIWEI
 * @date 2018/2/8 19:52
 */
public class Bootstrap {
    public static void main(String[] args) {
        GuiceInstanceFactory.getInjector();
    }
}

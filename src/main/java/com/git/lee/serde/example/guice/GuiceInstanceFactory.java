package com.git.lee.serde.example.guice;

import com.git.lee.serde.example.util.ReflectionUtil;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;

import java.util.HashSet;
import java.util.Set;

/**
 * @author LISHUAIWEI
 * @date 2018/2/8 19:43
 */
public class GuiceInstanceFactory {
    private static GuiceInstanceFactory INSTANCE = new GuiceInstanceFactory();
    private Injector injector;
    private Set<AbstractModule> modules = new HashSet<>();
    private final Object lock = new Object();

    private GuiceInstanceFactory () {}

    public static Injector getInjector() {
        if (INSTANCE.injector == null) {
            synchronized (INSTANCE.lock) {
                if (INSTANCE.injector == null) {
                    if (INSTANCE.modules.isEmpty()) {
                        INSTANCE.injector = Guice.createInjector(Stage.PRODUCTION, ReflectionUtil.newInstanceFromPackage(AbstractModule.class));
                    } else {
                        INSTANCE.injector = Guice.createInjector(Stage.PRODUCTION, INSTANCE.modules);
                    }
                }
            }
        }
        return INSTANCE.injector;
    }

    public static <T> T getInstance(Class<T> clazz) {
        return getInjector().getInstance(clazz);
    }
}

package com.git.lee.serde.example.util;

import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * @author LISHUAIWEI
 * @date 2018/2/8 19:31
 */
public class ReflectionUtil {
    private static Reflections reflections = new Reflections("com.git.lee");

    public static <T> Set<T> newInstanceFromPackage(Class<T> subType) {
        Set<Class<? extends T>> classSet = scannerSubType(subType);
        Set<T> instanceSet = new HashSet<>();
        if (classSet == null || classSet.size() <= 0) {
            return instanceSet;
        }
        for (Class<? extends T> cls : classSet) {
            if (Modifier.isAbstract(cls.getModifiers())) continue;
            try {
                instanceSet.add(cls.newInstance());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return instanceSet;
    }

    public static <T> Set<Class<? extends T>> scannerSubType(Class<T> subType) {
        return reflections.getSubTypesOf(subType);
    }
}

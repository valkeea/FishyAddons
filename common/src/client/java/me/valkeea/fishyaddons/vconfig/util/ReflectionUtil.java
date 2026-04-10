package me.valkeea.fishyaddons.vconfig.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("java:S3011") // Allow for keeping members private
public class ReflectionUtil {
    /**
     * Set field value, trying direct access first before forcing with setAccessible.
     */
    public static void setOrForceAccess(Field field, Object instance, Object value) throws IllegalAccessException {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            field.setAccessible(true);
            field.set(instance, value);
        }
    }

    /**
     * Try to create a MethodHandle for a method, first attempting normal access and then
     * trying with setAccessible if needed.
     */
    public static MethodHandle tryFirst(Method method) throws IllegalAccessException {
        try {
            return MethodHandles.lookup().unreflect(method);
        } catch (IllegalAccessException e) {
            method.setAccessible(true);
            return MethodHandles.lookup().unreflect(method);
        }
    }

    private ReflectionUtil() {}
}

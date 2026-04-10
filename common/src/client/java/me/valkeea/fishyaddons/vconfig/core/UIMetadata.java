package me.valkeea.fishyaddons.vconfig.core;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry.FieldInfo;

public class UIMetadata {
    
    private final Annotation annotation;
    private final ConfigKey<?> key;
    private final String name;
    private final String[] description;
    private final int order;
    private final String parent;
    private final String[] tooltip;
    private final UICategory category;
    private final String subcategory;
    private final String provider;
    private final String buttonText;
    private final String[] labels;
    private final int[] labelColors;
    private final Class<?> fieldType;
    private final boolean autoSync;

    private UIMetadata(@NotNull Annotation annotation, FieldInfo fieldInfo) {
        this.annotation = annotation;
        this.key = extractConfigKey(annotation);
        this.name = extractString(annotation, "name");
        this.description = extractArray(annotation, "description");
        this.order = extractInt(annotation, "order", 100);
        this.parent = extractString(annotation, "parent");
        this.tooltip = extractArray(annotation, "tooltip");
        this.category = extractEnum(annotation, "category", UICategory.class);
        this.subcategory = extractString(annotation, "subcategory");
        this.provider = extractString(annotation, "provider");
        this.buttonText = extractString(annotation, "buttonText");        
        this.labels = extractArray(annotation, "labels");
        this.labelColors = extractIntArray(annotation, "labelColors");
        this.fieldType = fieldInfo.getField().getType();
        this.autoSync = extractBoolean(annotation, "autoSync", false);
    }
    
    public static UIMetadata from(@NotNull Annotation annotation, FieldInfo fieldInfo) {
        return new UIMetadata(annotation, fieldInfo);
    }
    
    public static boolean isUIAnnotation(@NotNull Annotation annotation) {
        String className = annotation.annotationType().getSimpleName();
        return className.startsWith("UI");
    }
    
    public Annotation getAnnotation() { return annotation; }
    public String name() { return name; }
    public String[] description() { return description; }
    public int order() { return order; }
    public String parent() { return parent; }
    public String[] tooltip() { return tooltip; }
    public UICategory category() { return category; }
    public String subcategory() { return subcategory; }
    public String provider() { return provider; }
    public String buttonText() { return buttonText; }
    public String[] labels() { return labels; }
    public int[] labelColors() { return labelColors; }
    public Class<?> fieldType() { return fieldType; }
    public boolean autoSync() { return autoSync; }

    /** Incorrect annotation usage will be logged and and sync will be skipped {@link ConfigRegistry#autoSyncField} */
    @SuppressWarnings("squid:S1452")
    ConfigKey<?> key() { return key; }

    public @Nullable BooleanKey tryBooleanKey() {
        return (key instanceof BooleanKey bk && bk != BooleanKey.NONE) ? bk : null;
    }

    public @Nullable StringKey tryStringKey() {
        return (key instanceof StringKey sk && sk != StringKey.NONE) ? sk : null;
    }    

    public BooleanKey booleanKey() {
        if (key instanceof BooleanKey bk) return bk;
        throw new IllegalStateException("Expected BooleanKey, got: " + key.getClass().getSimpleName());
    }
    
    public IntKey intKey() {
        if (key instanceof IntKey ik) return ik;
        throw new IllegalStateException("Expected IntKey, got: " + key.getClass().getSimpleName());
    }
    
    public StringKey stringKey() {
        if (key instanceof StringKey sk) return sk;
        throw new IllegalStateException("Expected StringKey, got: " + key.getClass().getSimpleName());
    }
    
    public DoubleKey doubleKey() {
        if (key instanceof DoubleKey dk) return dk;
        throw new IllegalStateException("Expected DoubleKey, got: " + key.getClass().getSimpleName());
    }
    
    // Reflection helpers

    private static ConfigKey<?> extractConfigKey(@NotNull Annotation annotation) {
        String[] targets = {"key", "altKey"};
        for (String methodName : targets) {
            try {
                Method m = annotation.annotationType().getMethod(methodName);
                Object v = m.invoke(annotation);
                if (v instanceof ConfigKey<?> ck && !ck.getString().equals("NaN")) {
                    return ck;
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                // Ignore and try next
            }
        }
        return BooleanKey.NONE;
    }

    private static String[] extractArray(@NotNull Annotation annotation, String methodName) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            return v instanceof String[] s ? s : new String[0];
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return new String[0];
        }
    }

    private static int[] extractIntArray(@NotNull Annotation annotation, String methodName) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            return v instanceof int[] arr ? arr : new int[0];
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return new int[0];
        }
    } 

    private static String extractString(@NotNull Annotation annotation, String methodName) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            return v instanceof String s ? s : "";
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return "";
        }
    }   
    
    private static <E extends Enum<E>> E extractEnum(
        Annotation annotation, String methodName, Class<E> enumClass
    ) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            if (enumClass.isInstance(v)) {
                return enumClass.cast(v);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // Ignore and return default
        }
        // Return first enum constant as default
        return enumClass.getEnumConstants()[0];
    }
    
    private static int extractInt(@NotNull Annotation annotation, String methodName, int defaultValue) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            return v instanceof Integer i ? i : defaultValue;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return defaultValue;
        }
    }
    
    private static boolean extractBoolean(@NotNull Annotation annotation, String methodName, boolean defaultValue) {
        try {
            Method m = annotation.annotationType().getMethod(methodName);
            Object v = m.invoke(annotation);
            return v instanceof Boolean b ? b : defaultValue;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return defaultValue;
        }
    }
}

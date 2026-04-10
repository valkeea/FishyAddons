package me.valkeea.fishyaddons.vconfig.core;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.valkeea.fishyaddons.vconfig.annotation.VCInit;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener;
import me.valkeea.fishyaddons.vconfig.annotation.VCListener.Phase;
import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import me.valkeea.fishyaddons.vconfig.util.ReflectionUtil;

/**
 * Central registry for managing modules, entries, and listeners.
 * 
 * <h4>Responsibilities:</h4>
 * <ul>
 * <li>Register classes annotated with {@link VCModule}</li>
 * <li>Scan for {@link VCListener}, {@link VCInit} and any UI annotations within registered modules</li>
 * <li>Store metadata about modules, entries, and listeners for UI generation and runtime invocation</li>
 * <li>Handle initialization order and listener invocation during config changes</li>
 * </ul>
 * <h4>Design Notes:</h4>
 * <ul>
 * <li>Supports both static and instance methods/fields, but instance members require a
 * <br>singleton pattern with a public getInstance() method or a public no-arg constructor</li>
 * <li>Config changes can either be automatically synchronized with fields or handled via listeners</li>
 * </ul>
 * <h4>Reflection Usage:</h4>
 * <ul>
 * <li>By default used to scan for annotations, reading fields, and creating MethodHandles for listeners</li>
 * <li>Opt-in: If autoSync is enabled, setAccessible(true) is used to bypass access checks for fields</li>
 * </ul>
 */
public class ConfigRegistry {
    private ConfigRegistry() {}
    
    private static final List<ModuleInfo> modules = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRegistry.class);

    public static class ModuleInfo {
        private final Class<?> moduleClass;
        private final VCModule annotation;
        private final @Nullable MethodHandle initMethod;
        private final List<ListenerHandle> listeners;
        private final List<FieldInfo> uiFields;
        
        ModuleInfo(@NotNull Class<?> moduleClass, VCModule annotation, 
                   @Nullable MethodHandle initMethod, List<ListenerHandle> listeners,
                   List<FieldInfo> uiFields
        ) {
            this.moduleClass = moduleClass;
            this.annotation = annotation;
            this.initMethod = initMethod;
            this.listeners = listeners;
            this.uiFields = uiFields;
        }
        
        public UICategory getCategory() {
            return annotation.value();
        }
        
        public Class<?> getModuleClass() { return moduleClass; }
        public List<FieldInfo> getUiFields() { return uiFields; }
        public int getPriority() { return annotation.value().ordinal(); }
        
        void callInit() throws Throwable {
            if (initMethod != null) {
                initMethod.invoke();
            }
        }

        boolean initOnlyModule() {
            return uiFields.isEmpty() && listeners.isEmpty();
        }
    }
    
    public static class ListenerHandle {
        private final MethodHandle methodHandle;
        private final @Nullable Object instance;
        private final VCListener annotation;
        private final VCListener.Phase phase;
        private final int priority;
        private final String displayName;
        
        ListenerHandle(@NotNull Method method, @Nullable Object instance, 
                      VCListener annotation) {
            this.instance = instance;
            this.annotation = annotation;
            this.phase = annotation.phase();
            this.priority = annotation.priority();
            this.displayName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
            
            MethodHandle handle;
            try {
                handle = ReflectionUtil.tryFirst(method);
            } catch (IllegalAccessException e) {
                throw new ConfigModuleException("Failed to create MethodHandle for listener: " + displayName, e);
            }
            this.methodHandle = handle;
        }
        
        void invoke(Object... args) {
            try {
                int expectedParams = methodHandle.type().parameterCount();
                if (instance != null) {
                    if (expectedParams == 1) {
                        methodHandle.invoke(instance);
                    } else {
                        // Method expects instance + args
                        Object[] fullArgs = new Object[Math.min(expectedParams, args.length + 1)];
                        fullArgs[0] = instance;
                        int argsToCopy = Math.min(expectedParams - 1, args.length);
                        System.arraycopy(args, 0, fullArgs, 1, argsToCopy);
                        methodHandle.invokeWithArguments(fullArgs);
                    }

                } else {
                    // Static method
                    if (expectedParams == 0) {
                        methodHandle.invoke();
                    } else {
                        Object[] actualArgs = new Object[Math.min(expectedParams, args.length)];
                        System.arraycopy(args, 0, actualArgs, 0, actualArgs.length);
                        methodHandle.invokeWithArguments(actualArgs);
                    }
                }
            } catch (Throwable e) {
                LOGGER.error("Error invoking listener {}", displayName);
                e.printStackTrace();
            }
        }
        
        public Phase getPhase() { return phase; }
        public int getPriority() { return priority; }
        public VCListener getAnnotation() { return annotation; }
    }
    
    public static class FieldInfo {
        private final Field field;
        private final @Nullable Object instance;
        private final @Nullable Annotation primaryAnnotation;
        private final @Nullable Annotation secondaryAnnotation;
        
        /**
         * Constructor for FieldInfo.
         * @param field The field representing the config entry
         * @param instance The instance to access the field on (null for static fields)
         * @param primaryAnnotation The primary UI annotation (1st declared, provides metadata)
         * @param secondaryAnnotation The secondary UI annotation (2nd declared, optional metadata)
         */
        FieldInfo(@NotNull Field field, @Nullable Object instance, 
                  @Nullable Annotation primaryAnnotation, @Nullable Annotation secondaryAnnotation) {
            this.field = field;
            this.instance = instance;
            this.primaryAnnotation = primaryAnnotation;
            this.secondaryAnnotation = secondaryAnnotation;
        }
        
        public Field getField() { return field; }
        public @Nullable Object getInstance() { return instance; }
        public @Nullable Annotation getPrimaryAnnotation() { return primaryAnnotation; }
        public @Nullable Annotation getSecondaryAnnotation() { return secondaryAnnotation; }
        
        /**
         * Get metadata from the primary annotation.
         * @return AnnotationMetadata extracted from primary annotation
         */
        public UIMetadata getMetadata() {
            if (primaryAnnotation == null) {
                throw new IllegalStateException("Field has no UI annotation: " + field.getName());
            }
            return UIMetadata.from(primaryAnnotation, this);
        }

        public UIMetadata getSecondaryMetadata() {
            if (secondaryAnnotation == null) {
                throw new IllegalStateException("Field has no secondary UI annotation: " + field.getName());
            }
            return UIMetadata.from(secondaryAnnotation, this);
        }
        
        public boolean hasUIAnnotation() { return primaryAnnotation != null; }
    }
    
    public static void register(@NotNull Class<?> moduleClass) {
        Objects.requireNonNull(moduleClass, "moduleClass must not be null");

        var a = moduleClass.getAnnotation(VCModule.class);
        if (a == null) {
            throw new IllegalArgumentException("Class must be annotated with @VCModule: " + moduleClass.getName());
        }

        if (isRegistered(moduleClass)) {
            LOGGER.info("Module already registered, skipping: {}", moduleClass.getSimpleName());
            return;
        }

        Object instance = createInstanceIfNeeded(moduleClass);
        MethodHandle initHandle = null;
        Method vcInitMethod = null;

        for (Method m : moduleClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(VCInit.class)) {
                vcInitMethod = m;
                break;
            }
        }
        if (vcInitMethod != null) {
            try {
                initHandle = ReflectionUtil.tryFirst(vcInitMethod);
                if (!Modifier.isStatic(vcInitMethod.getModifiers()) && instance != null) {
                    initHandle = initHandle.bindTo(instance);
                }
            } catch (IllegalAccessException e) {
                throw new ConfigModuleException("Failed to access @VCInit method in " + moduleClass.getSimpleName(), e);
            }
        }

        var listeners = scanListeners(moduleClass, instance);
        var uiFields = scanUiFields(moduleClass, instance);
        var moduleInfo = new ModuleInfo(
            moduleClass, a, initHandle, listeners, uiFields
        );

        modules.add(moduleInfo);
    }
    
    private static @Nullable Object createInstanceIfNeeded(@NotNull Class<?> moduleClass) {

        boolean needed = false;
        
        for (Method m : moduleClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(VCListener.class) && !Modifier.isStatic(m.getModifiers())) {
                needed = true;
                break;
            }
        }
        
        if (!needed && !hasInstanceField(moduleClass)) {
            return null;
        }
        
        try {
            Method getInstance = moduleClass.getDeclaredMethod("getInstance");
            return getInstance.invoke(null);
        } catch (Exception ignored) {
            // Not a singleton or getter is not accessible
        }

        try {
            var struct = moduleClass.getDeclaredConstructor();
            return struct.newInstance();
        } catch (Exception e) {
            throw new ConfigModuleException("Failed to create instance of module: " + moduleClass.getSimpleName(), e);
        }
    }

    private static boolean hasInstanceField(@NotNull Class<?> moduleClass) {
        for (Field f : moduleClass.getDeclaredFields()) {

            boolean found = false;
            for (Annotation a : f.getAnnotations()) {
                if (a != null && UIMetadata.isUIAnnotation(a)) {
                    found = true;
                    break;
                }
            }
            if (found && !Modifier.isStatic(f.getModifiers())) {
                return true;
            }
        }
        return false;
    }
    
    private static List<ListenerHandle> scanListeners(
            Class<?> moduleClass, @Nullable Object instance) {
        
        List<ListenerHandle> listeners = new ArrayList<>();
        
        for (var method : moduleClass.getDeclaredMethods()) {
            var a = method.getAnnotation(VCListener.class);
            if (a == null) continue;
            
            if (!Modifier.isStatic(method.getModifiers()) && instance == null) {
                throw new IllegalStateException(
                    "Non-static listener method but no instance: " + method.getName()
                );
            }
            
            Object methodInstance = Modifier.isStatic(method.getModifiers()) ? null : instance;
            var handle = new ListenerHandle(method, methodInstance, a);
            listeners.add(handle);
        }
        
        return listeners;
    }
    
    private static List<FieldInfo> scanUiFields(
            Class<?> moduleClass, @Nullable Object instance) {
        
        List<FieldInfo> fields = new ArrayList<>();
        
        for (var field : moduleClass.getDeclaredFields()) {
            List<Annotation> uiAnnotations = new ArrayList<>();
            for (Annotation a : field.getAnnotations()) {
                if (a != null && UIMetadata.isUIAnnotation(a)) {
                    uiAnnotations.add(a);
                }
            }
            
            if (uiAnnotations.isEmpty()) continue;
            
            if (!Modifier.isStatic(field.getModifiers()) && instance == null) {
                throw new IllegalStateException(
                    "Non-static config field but no instance: " + field.getName()
                );
            }
            
            // Primary = 1st declared, Secondary = 2nd declared (if exists)
            var primary = uiAnnotations.get(0);
            var secondary = uiAnnotations.size() > 1 ? uiAnnotations.get(1) : null;
            
            fields.add(new FieldInfo(field, instance, primary, secondary));
        }
        
        return fields;
    }
    
    /**
    * Initialize all registered modules by calling any annotated init methods
    * and invoking listeners in the correct order.
    * 
    * Listeners are invoked in order of their priority (lower first).
    */
    public static void initializeAll() {
        modules.sort(Comparator.comparingInt(ModuleInfo::getPriority));

        List<ModuleInfo> toRemove = new ArrayList<>();

        for (var module : modules) {
            try {
                module.callInit();
                if (module.initOnlyModule()) {
                    toRemove.add(module);
                }
            } catch (Throwable e) {
                LOGGER.error("Error initializing module: {}", module.getModuleClass().getName());
            }
        }
        if (!toRemove.isEmpty()) {
            modules.removeAll(toRemove);
        }

        registerFieldAutoSync();
        registerAndInitListeners();
    }
    
    /**
     * If autoSync is enabled, keeps field values synchronized with their config keys
     */
    private static void registerFieldAutoSync() {
        
        for (var module : modules) {
            for (var fieldInfo : module.getUiFields()) {
                try {
                    var meta = fieldInfo.getMetadata();
                    if (!meta.autoSync()) continue;
                    autoSyncField(fieldInfo, meta.key());
                } catch (Exception e) {
                    LOGGER.error("Failed to auto-sync field: {}", fieldInfo.getField().getName(), e);
                }
            }
        }
    }
    
    /**
     * Setup auto-sync for a single field.
     * Loads the initial value from Config and registers a listener to keep it updated.
     */
    private static <T> void autoSyncField(FieldInfo fieldInfo, ConfigKey<T> key) {

        Field field = fieldInfo.getField();
        Object instance = fieldInfo.getInstance();
        T initialValue = Config.getValue(key);

        try {
            ReflectionUtil.setOrForceAccess(field, instance, initialValue);
        } catch (IllegalAccessException e) {
            LOGGER.error("Failed to initialize field: {}", field.getName());
            return; // Don't register if initial set failed
        }
        
        key.addListener(newValue -> {
            try {
                ReflectionUtil.setOrForceAccess(field, instance, newValue);
            } catch (IllegalAccessException e) {
                LOGGER.error("Failed to auto-sync field: {}", field.getName());
            }
        });
    }
    
    private static void registerAndInitListeners() {
        var listenersByKey = new HashMap<ConfigKey<?>, List<ListenerHandle>>();
        for (var module : modules) {
            for (var handle : module.listeners) {
                var phase = handle.getPhase();
                addPerKey(listenersByKey, handle, phase);
            }
        }
        initRuntimeListeners(listenersByKey);
    }

    private static Map<ConfigKey<?>, List<ListenerHandle>> addPerKey(
        Map<ConfigKey<?>, List<ListenerHandle>> listenersByKey, ListenerHandle handle, Phase phase
    ) {

        ConfigKey<?>[] keys = keysFrom(handle);
        if (keys.length == 0) {
            LOGGER.warn("{} listener has no keys specified: {}", phase, handle.displayName);
            return listenersByKey;
        }
        
        for (ConfigKey<?> key : keys) {
            key.addListener(handle::invoke);
            listenersByKey.computeIfAbsent(key, k -> new ArrayList<>()).add(handle);
        }
        return listenersByKey;
    }

    private static void initRuntimeListeners(Map<ConfigKey<?>, List<ListenerHandle>> listenersByKey) {

        for (var entry : listenersByKey.entrySet()) {
            var key = entry.getKey();
            var listeners = entry.getValue();
            Object currentValue = Config.getValue(key);
            listeners.sort(Comparator.comparingInt(ListenerHandle::getPriority));

            for (var listener : listeners) {
                if (listener.getPhase() == Phase.SYNC) {
                    listener.invoke(currentValue);
                }
            }
        }
    }

    /**
     * Extract ConfigKeys from a listener's annotation.
     * Merges all typed parameter arrays from the VCListener annotation.
     * @param handle The listener handle
     * @return Array of ConfigKeys this listener watches (may be empty)
     */
    private static ConfigKey<?>[] keysFrom(@NotNull ListenerHandle handle) {
        var listener = handle.getAnnotation();

        List<ConfigKey<?>> keys = new ArrayList<>();
        Collections.addAll(keys, listener.value());
        Collections.addAll(keys, listener.ints());
        Collections.addAll(keys, listener.doubles());
        Collections.addAll(keys, listener.strings());
        
        return keys.toArray(new ConfigKey<?>[0]);
    }
    
    // Query Methods for UI generation
    
    public static List<ModuleInfo> getModules() {
        return Collections.unmodifiableList(modules);
    }
    
    public static List<FieldInfo> getAllUiFields() {
        List<FieldInfo> allFields = new ArrayList<>();
        for (var module : modules) {
            allFields.addAll(module.getUiFields());
        }
        return allFields;
    }
    
    public static List<FieldInfo> getUiFieldsByCategory(@NotNull UICategory category) {
        Objects.requireNonNull(category, "category must not be null");
        
        return modules.stream()
            .filter(m -> m.getCategory().equals(category))
            .flatMap(m -> m.getUiFields().stream())
            .sorted(Comparator.comparingInt(f -> 
                f.hasUIAnnotation() ? f.getMetadata().order() : 999))
            .toList();
    }
    
    public static boolean isRegistered(@NotNull Class<?> moduleClass) {
        return modules.stream()
            .anyMatch(m -> m.getModuleClass().equals(moduleClass));
    }
    
    public static int getModuleCount() {
        return modules.size();
    }
    
    // Integration with package scanning
    
    public static void scanAndRegister(@NotNull String... packageNames) {
        ConfigScanner.scanPackages(packageNames);
        initializeAll();
    }
    
    public static void clear() {
        modules.clear();
    }
}

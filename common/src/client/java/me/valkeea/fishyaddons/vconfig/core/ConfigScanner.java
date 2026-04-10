package me.valkeea.fishyaddons.vconfig.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import me.valkeea.fishyaddons.vconfig.annotation.VCModule;
import me.valkeea.fishyaddons.vconfig.util.VCLogger;

/**
 * Package scanner for {@link VCModule} annotated classes.
 */
public class ConfigScanner {
    private ConfigScanner() {}
    
    /**
     * Scan one or more packages for {@link VCModule} classes and register them.
     * 
     * @param packageNames Package names to scan ("ie.some.package", ...)
     */
    public static void scanPackages(String... packageNames) {
        for (String packageName : packageNames) {
            List<Class<?>> classes = findClassesInPackage(packageName);
            
            for (Class<?> clazz : classes) {
                if (clazz.isAnnotationPresent(VCModule.class)) {
                    ConfigRegistry.register(clazz);
                }
            }
        }
    }
    
    /**
     * Find all classes in a package (recursively).
     */
    private static List<Class<?>> findClassesInPackage(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String packagePath = packageName.replace('.', '/');
        
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = loader.getResources(packagePath);
            
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                
                if ("file".equals(protocol)) { // testing
                    File directory = new File(resource.getFile());
                    classes.addAll(scanDir(directory, packageName));
                    
                } else if ("jar".equals(protocol)) {
                    String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));
                    jarPath = URLDecoder.decode(jarPath, StandardCharsets.UTF_8);
                    classes.addAll(scanJar(jarPath, packagePath));
                }
            }

        } catch (Exception e) {
            VCLogger.error(ConfigScanner.class, e, "Failed to scan package: " + packageName);
        }
        
        return classes;
    }
    
    private static List<Class<?>> scanDir(File directory, String packageName) {

        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) return classes;
        
        var files = directory.listFiles();
        if (files == null) return classes;
        
        for (var file : files) {
            var fileName = file.getName();
            
            if (file.isDirectory()) { // Recurse into subdirectories
                classes.addAll(scanDir(file, packageName + "." + fileName));
                
            } else if (fileName.endsWith(".class")) {
                var className = packageName + '.' + fileName.substring(0, fileName.length() - 6);
                
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    VCLogger.warn(ConfigScanner.class, "Could not load class " + className);
                }
            }
        }
        
        return classes;
    }
    
    private static List<Class<?>> scanJar(String jarPath, String packagePath) {
        List<Class<?>> classes = new ArrayList<>();
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                var entryName = entry.getName();
                
                if (entryName.startsWith(packagePath) && entryName.endsWith(".class")) {
                    var className = entryName.substring(0, entryName.length() - 6).replace('/', '.');
                    tryAddClass(classes, className);
                }
            }
        } catch (IOException e) {
            VCLogger.error(ConfigScanner.class, e, "Failed to read JAR: " + jarPath);
        }
        
        return classes;
    }

    private static void tryAddClass(List<Class<?>> classes, String className) {
        try {
            Class<?> clazz = Class.forName(className);
            classes.add(clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            VCLogger.warn(ConfigScanner.class, "Could not load class " + className);
        }
    }
    
    public static void scanDefaultPackages() {
        scanPackages(
            "me.valkeea.fishyaddons.feature"
        );
    }
}

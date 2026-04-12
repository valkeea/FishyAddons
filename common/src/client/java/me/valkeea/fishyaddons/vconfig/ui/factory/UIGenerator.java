package me.valkeea.fishyaddons.vconfig.ui.factory;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

import me.valkeea.fishyaddons.vconfig.annotation.*;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding;
import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.DummyBinding;
import me.valkeea.fishyaddons.vconfig.binding.ConfigBinding.NumberBinding;
import me.valkeea.fishyaddons.vconfig.core.ConfigRegistry.FieldInfo;
import me.valkeea.fishyaddons.vconfig.core.UIMetadata;
import me.valkeea.fishyaddons.vconfig.ui.control.*;
import me.valkeea.fishyaddons.vconfig.ui.factory.UIFactory.ExpandableStateProvider;
import me.valkeea.fishyaddons.vconfig.ui.manager.ScreenManager;
import me.valkeea.fishyaddons.vconfig.ui.screen.HudEditScreen;
import me.valkeea.fishyaddons.vconfig.ui.widget.dropdown.item.ToggleMenuItem;
import me.valkeea.fishyaddons.vconfig.util.ReflectionUtil;
import net.minecraft.client.MinecraftClient;

public final class UIGenerator {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(UIGenerator.class);

    @FunctionalInterface
    protected interface AnnotationHandler {
        List<UIControl> handle(FieldInfo fieldInfo, UIMetadata meta, 
                              Consumer<ColorControl> colorOpener, 
                              ExpandableStateProvider stateProvider);
    }
    
    protected static final Map<Class<?>, AnnotationHandler> PRIMARY = new HashMap<>();
    
    static {
        PRIMARY.put(UIToggle.class,
            (fi, meta, co, sp) -> createBoolean(fi, meta, co));
        
        PRIMARY.put(UISlider.class, 
            (fi, meta, co, sp) -> {
                if (isNumericType(meta.fieldType())) {
                    return createSlider(fi, (UISlider) meta.getAnnotation(), co);
                }
                return null;
            });
        
        PRIMARY.put(UIDropdown.class,
            (fi, meta, co, sp) -> createDropdown(fi, meta, (UIDropdown) meta.getAnnotation()));

        PRIMARY.put(UISearch.class, 
            (fi, meta, co, sp) -> createSearch(fi, meta, (UISearch) meta.getAnnotation()));
        
        PRIMARY.put(UIKeybind.class,
            (fi, meta, co, sp) -> List.of(createKeybind(meta)));

        PRIMARY.put(UIContainer.class,
            (fi, meta, co, sp) -> createExpandable(fi, meta, sp));

        PRIMARY.put(UIColorPicker.class,
            (fi, meta, co, sp) -> List.of(createColor(meta, co)));
        
        PRIMARY.put(UIRedirect.class,
            (fi, meta, co, sp) -> List.of(createRedirect(fi, meta, (UIRedirect) meta.getAnnotation())));
    }
    
    @FunctionalInterface
    private interface SecondaryAnnotationHandler {
        UIControl handle(UIMetadata meta, FieldInfo fi,
                              Consumer<ColorControl> colorOpener);
    }
    
    private static final Map<Class<?>, SecondaryAnnotationHandler> SECONDARY = new HashMap<>();
    
    static {
        SECONDARY.put(UIHudRedirect.class,
            (meta, fi, co) -> createHudRedirect(meta));
        
        SECONDARY.put(UIColorPicker.class,
            (meta, fi, co) -> createColor(meta, co));
        
        SECONDARY.put(UISlider.class,
            (meta, fi, co) -> createSlider((UISlider) meta.getAnnotation(), true));
        
        SECONDARY.put(UIRedirect.class,
            (meta, fi, co) -> createRedirect(fi, meta, (UIRedirect) meta.getAnnotation()));
        
        SECONDARY.put(UIExtraToggle.class,
            (meta, fi, co) -> createExtraToggle((UIExtraToggle) meta.getAnnotation()));

        SECONDARY.put(UIKeybind.class,
            (meta, fi, co) -> createKeybind(meta));
    }

    private static List<UIControl> createBoolean(FieldInfo fieldInfo, UIMetadata meta,
                 Consumer<ColorControl> colorOpenerCallback) {

        var key = meta.booleanKey();
        var toggleBinding = ConfigBinding.of(key);
        var tooltip = meta.tooltip();
        var secondary = fieldInfo.getSecondaryAnnotation();

        List<UIControl> controls = new ArrayList<>();
        controls.add(new ToggleControl(toggleBinding, tooltip));

        if (secondary == null) return controls;
        if (secondary instanceof UIHudRedirect) {
            controls.add(createHudRedirect(meta));
            return controls;
        }

        var meta2 = UIMetadata.from(secondary, fieldInfo);
        if (secondary instanceof UIRedirect redirect) {
            controls.add(createRedirect(fieldInfo, meta2, redirect));

        } else {
            var handler = SECONDARY.get(secondary.annotationType());
            if (handler != null) {
                controls.add(handler.handle(meta2, fieldInfo, colorOpenerCallback));
            }
        }
        
        return controls;
    }

    private static UIControl createKeybind(UIMetadata meta) {
        var binding = ConfigBinding.of(meta.stringKey());
        return new KeybindControl(binding, meta.tooltip());
    }
    
    private static List<UIControl> createDropdown(FieldInfo fieldInfo, UIMetadata meta, UIDropdown dropdown) {
        var type = fieldInfo.getField().getType();
        if (type != boolean.class && type != Boolean.class && type != String.class) {
            throw new IllegalArgumentException("Unsupported field type for dropdown: " + type);
        }

        List<UIControl> controls = new ArrayList<>();

        var key = meta.tryBooleanKey();
        if (key != null) {
            var toggleBinding = ConfigBinding.of(key);
            controls.add(new ToggleControl(toggleBinding, meta.tooltip()));
        }

        var menuProvider = createMenuProvider(fieldInfo, dropdown.provider());
        var button = dropdown.buttonText();
        var buttonText = button.equals("Configure") ? button : button + " ▼";
        controls.add(new MultiToggleControl(menuProvider, buttonText, meta.tooltip()));

        return controls;
    }

    private static List<UIControl> createSearch(FieldInfo fieldInfo, UIMetadata meta, UISearch search) {
        var menuProvider = createMenuProvider(fieldInfo, search.provider());
        List<UIControl> controls = new ArrayList<>();

        var key = meta.tryStringKey();
        if (key != null) {
            controls.add(new SearchControl(
                ConfigBinding.of(key),
                menuProvider,
                meta.tooltip()
            ));

        } else throw new IllegalArgumentException("Unsupported field type for search: " + fieldInfo.getField().getType());

        return controls;
    }    
    
    private static UIControl createRedirect(FieldInfo fieldInfo, UIMetadata meta, UIRedirect redirect) {
        MethodHandle redirectHandle;
        Object instance = fieldInfo.getInstance();
        
        try {
            Class<?> moduleClass = fieldInfo.getField().getDeclaringClass();
            Method redirectMethod = moduleClass.getDeclaredMethod(redirect.method());
            redirectHandle = ReflectionUtil.tryFirst(redirectMethod);
        } catch (NoSuchMethodException e) {
            LOGGER.error("Redirect method not found: {} in {}", redirect.method(), fieldInfo.getField().getDeclaringClass().getSimpleName());
            return null;
        } catch (IllegalAccessException e) {
            LOGGER.error("Cannot access redirect method: {} in {}", redirect.method(), fieldInfo.getField().getDeclaringClass().getSimpleName());
            return null;
        }
        
        String buttonText = redirect.buttonText().isEmpty() ? meta.name() : redirect.buttonText();
        
        return new ButtonControl(
            buttonText,
            () -> {
                try {
                    if (instance != null) {
                        redirectHandle.invoke(instance);
                    } else {
                        redirectHandle.invoke();
                    }
                    ScreenManager.preserveCurrentState();
                } catch (Throwable e) {
                    LOGGER.error("Error invoking redirect method: {} in {}", redirect.method(), fieldInfo.getField().getDeclaringClass().getSimpleName(), e);
                }
            },
            meta.tooltip()
        );
    }

    private static UIControl createHudRedirect(UIMetadata meta) {
        var mc = MinecraftClient.getInstance();
        return new ButtonControl("HUD", () -> 
            ScreenManager.navigateConfigScreen(new HudEditScreen(meta.booleanKey(), mc.currentScreen))
        , new String[0]
        );
    }
    
    private static List<UIControl> createExpandable(FieldInfo fieldInfo, UIMetadata meta, ExpandableStateProvider stateProvider) {
        boolean secondary = false;
        List<UIControl> controls = new ArrayList<>();
        var key = meta.tryBooleanKey();
        if (key != null) {
            // Assume toggle + dropdown combo if there's a config key
            var toggleBinding = ConfigBinding.of(key);
            controls.add(new ToggleControl(toggleBinding, meta.tooltip()));
            secondary = true;
        }

        var field = fieldInfo.getField();
        String entryName = meta.name().isEmpty() ? 
            formatFieldName(field.getName()) : 
            meta.name();

        var expandedBinding = new DummyBinding() {
            @Override
            public boolean get() {
                return stateProvider.isExpanded(entryName);
            }
            
            @Override
            public void set(boolean value) {
                if (value != get()) {
                    stateProvider.toggleExpanded(entryName);
                }
            }
            
            @Override
            public boolean getDefault() {
                return false;
            }
            
            @Override
            public String getKey() {
                return entryName + ".expanded";
            }
        };
        
        controls.add(new ExpandableControl(expandedBinding, meta.tooltip(), secondary));
        return controls;
    }
    
    // Composite helpers

    private static List<UIControl> createSlider(FieldInfo fieldInfo, UISlider slider, Consumer<ColorControl> colorOpenerCallback) {
        List<UIControl> controls = new ArrayList<>();
        ColorControl colorControl = null;

        if (fieldInfo.getSecondaryAnnotation() instanceof UIColorPicker) {
            var meta2 = UIMetadata.from(fieldInfo.getSecondaryAnnotation(), fieldInfo);
            colorControl = (ColorControl)createColor(meta2, colorOpenerCallback);
        }

        controls.add(createSlider(slider, colorControl != null));
        if (colorControl != null) controls.add(colorControl);
        return controls;
    }

    private static UIControl createSlider(UISlider slider, boolean composite) {
        if (slider == null) {
            throw new IllegalArgumentException("UISlider annotation is required for slider control");
        }

        var min = slider.min();
        var max = slider.max();
        var format = slider.format();
        var labels = slider.labels();
        var tooltip = slider.tooltip();
        var labelColors = slider.labelColors(); 

        NumberBinding<?> binding;
        var altKey = slider.altKey();
        
        if (altKey != IntKey.NONE) {
            binding = ConfigBinding.ofNumber(altKey);
        } else {
            binding = ConfigBinding.ofNumber(slider.key());
        }

        double[] minMax = new double[] { min, max };
        return new SliderControl(binding, minMax, format, labels, labelColors, tooltip, composite);
    }    

    private static UIControl createColor(UIMetadata meta, Consumer<ColorControl> colorOpenerCallback) {
        return new ColorControl(ConfigBinding.of(meta.intKey()), colorOpenerCallback, meta.tooltip());
    }

    private static UIControl createExtraToggle(UIExtraToggle extraToggle) {
        var b2 = ConfigBinding.of(extraToggle.key());
        return  new ToggleControl(b2, extraToggle.buttonText(), new String[0]);
    }

    /**
     * Create a menu provider by looking up the specified provider method on the module instance.
     */
    private static Supplier<List<ToggleMenuItem>> createMenuProvider(
        FieldInfo fi, String provider) {
        
        if (provider.isEmpty()) return List::of;
        
        try {
            Class<?> module = fi.getField().getDeclaringClass();
            Object instance = fi.getInstance();
            Method method = module.getDeclaredMethod(provider);
            MethodHandle handle = ReflectionUtil.tryFirst(method);
            
            return () -> {
                try {
                    @SuppressWarnings("unchecked")
                    var items = (List<ToggleMenuItem>) (instance != null 
                        ? handle.invoke(instance) 
                        : handle.invoke());
                    return items != null ? items : List.of();
                } catch (Throwable e) {
                    LOGGER.error("Error invoking menu provider method: {} in {}", provider, module.getSimpleName(), e);
                    return List.of();
                }
            };
            
        } catch (NoSuchMethodException | IllegalAccessException e) {
            LOGGER.error(
                "Menu provider method not found or inaccessible: {} in {}",
                provider,
                fi.getField().getDeclaringClass().getSimpleName(), e
            );
            return List::of;
        }
    }

    private static String formatFieldName(String fieldName) {
        return fieldName.replaceAll("([A-Z])", " $1")
            .substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    private static boolean isNumericType(Class<?> type) {
        return type == int.class || type == Integer.class ||
               type == double.class || type == Double.class;
    }
}

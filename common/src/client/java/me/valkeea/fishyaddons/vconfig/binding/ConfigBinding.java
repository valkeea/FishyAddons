package me.valkeea.fishyaddons.vconfig.binding;

import me.valkeea.fishyaddons.vconfig.api.BooleanKey;
import me.valkeea.fishyaddons.vconfig.api.Config;
import me.valkeea.fishyaddons.vconfig.api.ConfigKey;
import me.valkeea.fishyaddons.vconfig.api.DoubleKey;
import me.valkeea.fishyaddons.vconfig.api.IntKey;
import me.valkeea.fishyaddons.vconfig.api.StringKey;

/**
 * Config link for UI elements
 */
public final class ConfigBinding {
    private ConfigBinding() {}
    
    public interface BooleanBinding {
        boolean get();
        void set(boolean value);
        boolean getDefault();
        BooleanKey getKey();
    }
    
    public static BooleanBinding ofBoolean(BooleanKey key) {
        return new BooleanBinding() {
            @Override
            public boolean get() {
                return Config.get(key);
            }
            
            @Override
            public void set(boolean value) {
                Config.set(key, value);
            }
            
            @Override
            public boolean getDefault() {
                return key.getDefault();
            }
            
            @Override
            public BooleanKey getKey() {
                return key;
            }
        };
    }
    
    public interface IntBinding {
        int get();
        void set(int value);
        int getDefault();
        IntKey getKey();
    }
    
    public static IntBinding ofInt(IntKey key) {
        return new IntBinding() {
            @Override
            public int get() {
                return Config.get(key);
            }
            
            @Override
            public void set(int value) {
                Config.set(key, value);
            }
            
            @Override
            public int getDefault() {
                return key.getDefault();
            }
            
            @Override
            public IntKey getKey() {
                return key;
            }
        };
    }
    
    public interface DoubleBinding {
        double get();
        void set(double value);
        double getDefault();
        DoubleKey getKey();
    }
    
    public static DoubleBinding ofDouble(DoubleKey key) {
        return new DoubleBinding() {
            @Override
            public double get() {
                return Config.get(key);
            }
            
            @Override
            public void set(double value) {
                Config.set(key, value);
            }
            
            @Override
            public double getDefault() {
                return key.getDefault();
            }
            
            @Override
            public DoubleKey getKey() {
                return key;
            }
        };
    }
    
    public interface StringBinding {
        String get();
        void set(String value);
        String getDefault();
        StringKey getKey();
    }
    
    public static StringBinding ofString(StringKey key) {
        return new StringBinding() {
            @Override
            public String get() {
                return Config.get(key);
            }
            
            @Override
            public void set(String value) {
                Config.set(key, value);
            }
            
            @Override
            public String getDefault() {
                return key.getDefault();
            }
            
            @Override
            public StringKey getKey() {
                return key;
            }
        };
    }

    public static BooleanBinding of(BooleanKey key) {
        return ofBoolean(key);
    }    
    
    public static IntBinding of(IntKey key) {
        return ofInt(key);
    }    
    
    public static DoubleBinding of(DoubleKey key) {
        return ofDouble(key);
    }    
    
    public static StringBinding of(StringKey key) {
        return ofString(key);
    }    

    /**
     * A binding for int and double values.
     */
    public interface NumberBinding<T extends Number> {
        double get();
        void set(double value);
        double getDefault();
        ConfigKey<T> getKey();
    }

    /**
     * Create a binding for either an IntKey or DoubleKey. The returned binding will delegate to the appropriate type based on the key provided.
     * @param <T> The type of the key (IntKey or DoubleKey)
     * @param key The config key to bind to
     * @return A NumberBinding that delegates to the correct type based on the key provided
     */
    public static <T extends Number> NumberBinding<T> ofNumber(ConfigKey<T> key) {
        switch(key) {
            case IntKey intKey -> {
                IntBinding intBinding = ofInt(intKey);
                return new NumberBinding<>() {
                    @Override
                    public double get() {
                        return intBinding.get();
                    }

                    @Override
                    public void set(double value) {
                        intBinding.set((int) value);
                    }

                    @Override
                    public double getDefault() {
                        return intBinding.getDefault();
                    }

                    @Override
                    public ConfigKey<T> getKey() {
                        return key;
                    }
                };
            }
            case DoubleKey doubleKey -> {
                DoubleBinding doubleBinding = ofDouble(doubleKey);
                return new NumberBinding<>() {
                    @Override
                    public double get() {
                        return doubleBinding.get();
                    }

                    @Override
                    public void set(double value) {
                        doubleBinding.set(value);
                    }

                    @Override
                    public double getDefault() {
                        return doubleBinding.getDefault();
                    }

                    @Override
                    public ConfigKey<T> getKey() {
                        return key;
                    }
                };
            }
            default -> throw new IllegalArgumentException("Unsupported key type for number binding: " + key.getClass());
        }
    }
    
    /**
    * Placeholder, does nothing.
    */
    public interface DummyBinding {
        boolean get();
        void set(boolean value);
        boolean getDefault();
        String getKey();
    }
}

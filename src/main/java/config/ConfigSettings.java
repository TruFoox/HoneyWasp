package config;

import java.util.List;

public interface ConfigSettings {
    Object get(String setting);
    void set(String setting, String newValue);
}
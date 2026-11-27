// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.repository;

import java.util.Map;

/**
 * Persistence contract for local application preferences and settings.
 */
public interface SettingsRepository {

    String get(String key, String defaultValue);

    int getInt(String key, int defaultValue);

    boolean getBoolean(String key, boolean defaultValue);

    void set(String key, String value);

    Map<String, String> getAll();
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

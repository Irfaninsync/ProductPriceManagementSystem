// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.repository;

import com.ppms.model.ActivityEntry;
import java.util.List;

/**
 * Persistence contract for real business activity log records.
 */
public interface ActivityRepository {

    void record(String eventType, String entityType, String entityId, String summary);

    List<ActivityEntry> findRecent(int limit);

    int count();
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

// GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem
package com.ppms.model;

import java.time.LocalDateTime;

/**
 * Represents a real recorded business action for operational visibility.
 */
public class ActivityEntry {

    private final long activityId;
    private final String eventType;
    private final String entityType;
    private final String entityId;
    private final String summary;
    private final LocalDateTime createdAt;

    public ActivityEntry(long activityId,
                         String eventType,
                         String entityType,
                         String entityId,
                         String summary,
                         LocalDateTime createdAt) {
        this.activityId = activityId;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public long getActivityId() { return activityId; }
    public String getEventType() { return eventType; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getSummary() { return summary; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
// End of file — GitHub Repository: https://github.com/Irfaninsync/ProductPriceManagementSystem

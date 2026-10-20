
package com.fairShare.fairshare_backend.groups.dto;

import com.fairShare.fairshare_backend.groups.Group;
import java.time.LocalDateTime;
import java.util.UUID;

public record GroupResponse(
        UUID id,
        String name,
        String description,
        String defaultCurrency,
        UUID createdBy,
        LocalDateTime createdAt
) {
    public static GroupResponse from(Group group) {
        return new GroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getDefaultCurrency(),
                group.getCreatedBy(),
                group.getCreatedAt()
        );
    }
}
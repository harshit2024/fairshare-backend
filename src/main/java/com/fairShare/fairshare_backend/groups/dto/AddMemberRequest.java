
package com.fairShare.fairshare_backend.groups.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AddMemberRequest(
        @NotNull UUID userId
) {}
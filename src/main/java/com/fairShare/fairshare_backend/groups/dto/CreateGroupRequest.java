
package com.fairShare.fairshare_backend.groups.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGroupRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @Size(max = 500)
        String description,

        @Size(min = 3, max = 3)
        String defaultCurrency
) {}
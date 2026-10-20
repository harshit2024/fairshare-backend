
package com.fairShare.fairshare_backend.groups;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface GroupRepository
        extends JpaRepository<Group, UUID> {

    List<Group> findByCreatedBy(UUID userId);
}
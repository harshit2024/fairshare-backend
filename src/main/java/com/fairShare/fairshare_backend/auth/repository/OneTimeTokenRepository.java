
package com.fairShare.fairshare_backend.auth.repository;

import com.fairShare.fairshare_backend.auth.entity.OneTimeToken;
import com.fairShare.fairshare_backend.auth.entity.TokenPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OneTimeTokenRepository
        extends JpaRepository<OneTimeToken, UUID> {

    Optional<OneTimeToken> findByTokenHashAndPurpose(
            String tokenHash,
            TokenPurpose purpose
    );

    List<OneTimeToken> findAllByUser_IdAndPurposeAndUsedAtIsNull(
            UUID userId,
            TokenPurpose purpose
    );
}

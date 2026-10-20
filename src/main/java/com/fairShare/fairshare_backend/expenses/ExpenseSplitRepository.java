
package com.fairShare.fairshare_backend.expenses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExpenseSplitRepository
        extends JpaRepository<ExpenseSplit, UUID> {

    List<ExpenseSplit> findByExpenseId(UUID expenseId);

    List<ExpenseSplit> findByUserId(UUID userId);

    void deleteByExpenseId(UUID expenseId);
}
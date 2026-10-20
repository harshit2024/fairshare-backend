
package com.fairShare.fairshare_backend.expenses;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository
        extends JpaRepository<Expense, UUID> {

    List<Expense> findByGroupIdOrderByExpenseDateDesc(UUID groupId);

    List<Expense> findByCreatedByOrderByExpenseDateDesc(UUID userId);

    List<Expense> findByPaidByOrderByExpenseDateDesc(UUID userId);
}
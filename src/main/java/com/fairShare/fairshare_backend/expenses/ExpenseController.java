
package com.fairShare.fairshare_backend.expenses;

import com.fairShare.fairshare_backend.expenses.dto.*;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/groups/{groupId}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(groupId, request);
    }

    @GetMapping("/groups/{groupId}/expenses")
    public List<ExpenseResponse> getGroupExpenses(
            @PathVariable UUID groupId) {
        return expenseService.getGroupExpenses(groupId);
    }

    @GetMapping("/expenses/{expenseId}")
    public ExpenseResponse getById(
            @PathVariable UUID expenseId) {
        return expenseService.getById(expenseId);
    }

    @GetMapping("/expenses/{expenseId}/splits")
    public List<ExpenseSplitResponse> getSplits(
            @PathVariable UUID expenseId) {
        return expenseService.getSplits(expenseId);
    }

    @DeleteMapping("/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID expenseId) {
        expenseService.delete(expenseId);
    }
}
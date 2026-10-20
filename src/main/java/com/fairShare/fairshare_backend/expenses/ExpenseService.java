
package com.fairShare.fairshare_backend.expenses;

import com.fairShare.fairshare_backend.expenses.dto.*;
import com.fairShare.fairshare_backend.groups.GroupMemberRepository;
import com.fairShare.fairshare_backend.security.CurrentUserService;
import com.fairShare.fairshare_backend.users.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ExpenseService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitRepository splitRepository;
    private final GroupMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            ExpenseSplitRepository splitRepository,
            GroupMemberRepository memberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService) {
        this.expenseRepository = expenseRepository;
        this.splitRepository = splitRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ExpenseResponse create(
            UUID groupId, CreateExpenseRequest request) {

        UUID actorId = currentUserService.getCurrentUserId();
        requireGroupMember(groupId, actorId);

        if (!memberRepository.existsByGroupIdAndUserId(
                groupId, request.paidBy())) {
            throw badRequest("Payer must be a member of the group");
        }

        Set<UUID> participantIds = new HashSet<>();

        for (SplitRequest split : request.splits()) {
            if (!participantIds.add(split.userId())) {
                throw badRequest("Duplicate split participant");
            }

            if (!memberRepository.existsByGroupIdAndUserId(
                    groupId, split.userId())) {
                throw badRequest(
                        "Every split participant must belong to the group");
            }

            if (!userRepository.existsById(split.userId())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Split participant not found");
            }
        }

        BigDecimal amount = request.amount()
                .setScale(2, RoundingMode.UNNECESSARY);

        Expense expense = new Expense();
        expense.setGroupId(groupId);
        expense.setDescription(request.description().trim());
        expense.setAmount(amount);
        expense.setCurrency(normalizeCurrency(request.currency()));
        expense.setPaidBy(request.paidBy());
        expense.setCreatedBy(actorId);
        expense.setCategory(request.category());
        expense.setSplitType(request.splitType());
        expense.setExpenseDate(request.expenseDate());
        expense.setNotes(request.notes());

        Expense saved = expenseRepository.save(expense);

        List<ExpenseSplit> splits = calculateSplits(saved, request.splits());

        splitRepository.saveAll(splits);

        return ExpenseResponse.from(saved, splits);
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(UUID groupId) {
        UUID actorId = currentUserService.getCurrentUserId();
        requireGroupMember(groupId, actorId);

        return expenseRepository
                .findByGroupIdOrderByExpenseDateDesc(groupId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getById(UUID expenseId) {
        Expense expense = findExpense(expenseId);
        requireGroupMember(expense.getGroupId(),
                currentUserService.getCurrentUserId());

        return toResponse(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseSplitResponse> getSplits(UUID expenseId) {
        Expense expense = findExpense(expenseId);
        requireGroupMember(expense.getGroupId(),
                currentUserService.getCurrentUserId());

        return splitRepository.findByExpenseId(expenseId)
                .stream()
                .map(ExpenseSplitResponse::from)
                .toList();
    }

    @Transactional
    public void delete(UUID expenseId) {
        UUID actorId = currentUserService.getCurrentUserId();
        Expense expense = findExpense(expenseId);

        requireGroupMember(expense.getGroupId(), actorId);

        if (!expense.getCreatedBy().equals(actorId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the expense creator can delete this expense");
        }

        // Child rows must be deleted before the parent row because
        // expense_splits uses ON DELETE RESTRICT.
        splitRepository.deleteByExpenseId(expenseId);
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense expense) {
        return ExpenseResponse.from(
                expense,
                splitRepository.findByExpenseId(expense.getId())
        );
    }

    private Expense findExpense(UUID id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private void requireGroupMember(UUID groupId, UUID userId) {
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a member of this group");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }

        String normalized = currency.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[A-Z]{3}")) {
            throw badRequest("Currency must be a three-letter code");
        }

        return normalized;
    }

    private List<ExpenseSplit> calculateSplits(
            Expense expense, List<SplitRequest> requests) {

        List<BigDecimal> amounts = switch (expense.getSplitType()) {
            case EQUAL -> equalAmounts(expense.getAmount(), requests.size());
            case EXACT -> exactAmounts(expense.getAmount(), requests);
            case PERCENTAGE -> percentageAmounts(
                    expense.getAmount(), requests);
            case SHARES -> shareAmounts(expense.getAmount(), requests);
        };

        List<ExpenseSplit> result = new ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            SplitRequest request = requests.get(i);

            ExpenseSplit split = new ExpenseSplit();
            split.setExpenseId(expense.getId());
            split.setUserId(request.userId());
            split.setShareAmount(amounts.get(i));

            if (expense.getSplitType() == SplitType.PERCENTAGE) {
                split.setPercentage(request.value());
            }

            if (expense.getSplitType() == SplitType.SHARES) {
                split.setShares(request.value());
            }

            result.add(split);
        }

        return result;
    }

    private List<BigDecimal> equalAmounts(
            BigDecimal total, int count) {

        long totalCents = total
                .movePointRight(2)
                .longValueExact();

        long baseCents = totalCents / count;
        long remainder = totalCents % count;

        List<BigDecimal> result = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            long cents = baseCents + (i < remainder ? 1 : 0);
            result.add(BigDecimal.valueOf(cents, 2));
        }

        return result;
    }

    private List<BigDecimal> exactAmounts(
            BigDecimal total, List<SplitRequest> requests) {

        List<BigDecimal> amounts = requests.stream()
                .map(SplitRequest::value)
                .toList();

        if (amounts.stream().anyMatch(
                value -> value == null || value.signum() < 0)) {
            throw badRequest(
                    "Every EXACT split needs a non-negative amount");
        }

        List<BigDecimal> normalized = amounts.stream()
                .map(value -> value.setScale(2, RoundingMode.UNNECESSARY))
                .toList();

        BigDecimal sum = normalized.stream()
                .reduce(ZERO, BigDecimal::add);

        if (sum.compareTo(total) != 0) {
            throw badRequest(
                    "EXACT split amounts must equal the expense amount");
        }

        return normalized;
    }

    private List<BigDecimal> percentageAmounts(
            BigDecimal total, List<SplitRequest> requests) {

        List<BigDecimal> percentages = requests.stream()
                .map(SplitRequest::value)
                .toList();

        if (percentages.stream().anyMatch(
                value -> value == null ||
                        value.signum() < 0 ||
                        value.compareTo(HUNDRED) > 0)) {
            throw badRequest(
                    "Percentages must be between 0 and 100");
        }

        BigDecimal percentageTotal = percentages.stream()
                .reduce(ZERO, BigDecimal::add);

        if (percentageTotal.compareTo(HUNDRED) != 0) {
            throw badRequest("Percentages must total exactly 100");
        }

        List<BigDecimal> result = new ArrayList<>();
        BigDecimal allocated = ZERO;

        for (int i = 0; i < percentages.size(); i++) {
            BigDecimal share;

            if (i == percentages.size() - 1) {
                // Give the final participant the rounding remainder.
                share = total.subtract(allocated);
            } else {
                share = total.multiply(percentages.get(i))
                        .divide(HUNDRED, 2, RoundingMode.HALF_UP);

                allocated = allocated.add(share);
            }

            result.add(share);
        }

        return result;
    }

    private List<BigDecimal> shareAmounts(
            BigDecimal total, List<SplitRequest> requests) {

        List<BigDecimal> weights = requests.stream()
                .map(SplitRequest::value)
                .toList();

        if (weights.stream().anyMatch(
                value -> value == null || value.signum() < 0)) {
            throw badRequest(
                    "SHARES split needs non-negative share values");
        }

        BigDecimal totalWeight = weights.stream()
                .reduce(ZERO, BigDecimal::add);

        if (totalWeight.signum() <= 0) {
            throw badRequest(
                    "Total shares must be greater than zero");
        }

        List<BigDecimal> result = new ArrayList<>();
        BigDecimal allocated = ZERO;

        for (int i = 0; i < weights.size(); i++) {
            BigDecimal share;

            if (i == weights.size() - 1) {
                share = total.subtract(allocated);
            } else {
                share = total.multiply(weights.get(i))
                        .divide(totalWeight, 2, RoundingMode.HALF_UP);

                allocated = allocated.add(share);
            }

            result.add(share);
        }

        return result;
    }
}
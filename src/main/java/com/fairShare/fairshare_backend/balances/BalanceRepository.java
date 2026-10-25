package com.fairShare.fairshare_backend.balances;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository
public class BalanceRepository {

    private final JdbcTemplate jdbcTemplate;

    public BalanceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record GroupMemberRow(
            UUID userId,
            String name,
            String email
    ) {}

    public record ExpenseRow(
            UUID id,
            UUID paidBy,
            BigDecimal amount,
            String currency
    ) {}

    public record SplitRow(
            UUID expenseId,
            UUID userId,
            BigDecimal shareAmount,
            String currency
    ) {}

    public record SettlementRow(
            UUID paidByUserId,
            UUID paidToUserId,
            BigDecimal amount,
            String currency
    ) {}

    public List<GroupMemberRow> findGroupMembers(UUID groupId) {
        String sql = """
            SELECT u.id AS user_id, u.name, u.email
            FROM group_members gm
            JOIN users u ON u.id = gm.user_id
            WHERE gm.group_id = ?
            ORDER BY u.name, u.email
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new GroupMemberRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("name"),
                        rs.getString("email")
                ),
                groupId
        );
    }

    public List<ExpenseRow> findGroupExpenses(UUID groupId) {
        String sql = """
            SELECT e.id, e.paid_by, e.amount, e.currency
            FROM expenses e
            WHERE e.group_id = ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new ExpenseRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("paid_by", UUID.class),
                        rs.getBigDecimal("amount"),
                        normalizeCurrency(rs.getString("currency"))
                ),
                groupId
        );
    }

    public List<SplitRow> findGroupSplits(UUID groupId) {
        String sql = """
            SELECT es.expense_id, es.user_id, es.share_amount, e.currency
            FROM expense_splits es
            JOIN expenses e ON e.id = es.expense_id
            WHERE e.group_id = ?
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SplitRow(
                        rs.getObject("expense_id", UUID.class),
                        rs.getObject("user_id", UUID.class),
                        rs.getBigDecimal("share_amount"),
                        normalizeCurrency(rs.getString("currency"))
                ),
                groupId
        );
    }

    /*
     * Assumed existing settlement schema:
     * group_id, paid_by_user_id, paid_to_user_id,
     * amount, currency, status.
     *
     * Only completed repayments affect the outstanding balance.
     */
    public List<SettlementRow> findCompletedSettlements(UUID groupId) {
        String sql = """
            SELECT paid_by, paid_to, amount, currency
            FROM settlements
            WHERE group_id = ?
              AND status = 'COMPLETED'
            """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SettlementRow(
                        rs.getObject("paid_by", UUID.class),
                        rs.getObject("paid_to", UUID.class),
                        rs.getBigDecimal("amount"),
                        normalizeCurrency(rs.getString("currency"))
                ),
                groupId
        );
    }

    private static String normalizeCurrency(String currency) {
        return currency == null
                ? "INR"
                : currency.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
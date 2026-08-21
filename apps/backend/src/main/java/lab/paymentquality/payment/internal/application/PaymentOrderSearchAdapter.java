package lab.paymentquality.payment.internal.application;

import lab.paymentquality.merchant.PaymentOrderSearch;
import lab.paymentquality.merchant.PaymentOrderSearchHit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentOrderSearchAdapter implements PaymentOrderSearch {

    private final JdbcTemplate jdbcTemplate;

    public PaymentOrderSearchAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderSearchHit> searchByClientOrderReference(
            String query,
            UUID tenantId,
            UUID merchantId,
            int limit) {
        String pattern = "%" + query.toLowerCase() + "%";
        int max = Math.max(1, Math.min(limit, 10));
        return jdbcTemplate.query(
                """
                SELECT po.payment_order_id, po.merchant_id, po.client_order_reference
                FROM payment_orders po
                INNER JOIN merchants m ON m.merchant_id = po.merchant_id
                WHERE lower(po.client_order_reference) LIKE ?
                  AND (?::uuid IS NULL OR m.tenant_id = ?::uuid)
                  AND (?::uuid IS NULL OR po.merchant_id = ?::uuid)
                ORDER BY po.created_at DESC
                LIMIT ?
                """,
                ps -> {
                    ps.setString(1, pattern);
                    setUuidOrNull(ps, 2, tenantId);
                    setUuidOrNull(ps, 3, tenantId);
                    setUuidOrNull(ps, 4, merchantId);
                    setUuidOrNull(ps, 5, merchantId);
                    ps.setInt(6, max);
                },
                (rs, rowNum) -> new PaymentOrderSearchHit(
                        rs.getObject("payment_order_id", UUID.class),
                        rs.getObject("merchant_id", UUID.class),
                        rs.getString("client_order_reference")));
    }

    private static void setUuidOrNull(java.sql.PreparedStatement ps, int index, UUID value)
            throws java.sql.SQLException {
        if (value == null) {
            ps.setNull(index, Types.OTHER);
        } else {
            ps.setObject(index, value);
        }
    }
}

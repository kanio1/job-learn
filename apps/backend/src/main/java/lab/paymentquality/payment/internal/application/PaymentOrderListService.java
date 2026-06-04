package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.infrastructure.PaymentOrderSpecification;
import lab.paymentquality.payment.internal.web.PaymentOrderListRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PaymentOrderListService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_PAGE_SIZE = 1;

    private final JpaPaymentOrderRepository paymentOrderRepository;

    public PaymentOrderListService(JpaPaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    public Page<PaymentOrder> findAll(UUID merchantId, PaymentOrderListRequest request) {
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;
        String sortParam = request.sort() != null ? request.sort() : "createdAt,desc";

        String[] sortParts = sortParam.split(",");
        Sort.Direction direction = sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, sortParts[0]);

        LocalDate from = parseDateSafely(request.fromDate());
        LocalDate to = parseDateSafely(request.toDate());

        Specification<PaymentOrder> spec = PaymentOrderSpecification.hasMerchantId(merchantId);
        spec = addIfNotNull(spec, PaymentOrderSpecification.hasStatus(request.status()));
        spec = addIfNotNull(spec, PaymentOrderSpecification.hasCurrency(request.currency()));
        spec = addIfNotNull(spec, PaymentOrderSpecification.createdBetween(from, to));
        spec = addIfNotNull(spec, PaymentOrderSpecification.amountBetween(
                request.minAmount(), request.maxAmount()));
        spec = addIfNotNull(spec, PaymentOrderSpecification.clientOrderReferenceContains(
                request.clientOrderReference()));

        return paymentOrderRepository.findAll(spec, PageRequest.of(page, size, sort));
    }

    private LocalDate parseDateSafely(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected ISO date (YYYY-MM-DD)");
        }
    }

    private Specification<PaymentOrder> addIfNotNull(Specification<PaymentOrder> base, Specification<PaymentOrder> additional) {
        return additional != null ? base.and(additional) : base;
    }
}

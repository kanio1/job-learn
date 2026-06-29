package lab.paymentquality.payment.internal.application;

import lab.paymentquality.payment.internal.domain.PaymentOrder;
import lab.paymentquality.payment.internal.domain.PaymentOrderNote;
import lab.paymentquality.payment.internal.domain.PaymentOrderNotFoundException;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderNoteRepository;
import lab.paymentquality.payment.internal.infrastructure.JpaPaymentOrderRepository;
import lab.paymentquality.payment.internal.web.PaymentOrderNoteDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentOrderNoteService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrderNoteService.class);

    private final JpaPaymentOrderNoteRepository noteRepository;
    private final JpaPaymentOrderRepository paymentOrderRepository;

    public PaymentOrderNoteService(JpaPaymentOrderNoteRepository noteRepository,
                                   JpaPaymentOrderRepository paymentOrderRepository) {
        this.noteRepository = noteRepository;
        this.paymentOrderRepository = paymentOrderRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentOrderNoteDto> listNotes(UUID merchantId, UUID paymentOrderId) {
        requirePaymentOrderExists(merchantId, paymentOrderId);
        return noteRepository.findAllByPaymentOrderIdOrderByCreatedAtAsc(paymentOrderId)
                .stream()
                .map(n -> new PaymentOrderNoteDto(n.getId(), n.getBody(), n.getAuthorDisplay(), n.getCreatedAt()))
                .toList();
    }

    public PaymentOrderNoteDto addNote(UUID merchantId, UUID paymentOrderId, String body, String authorDisplay) {
        requirePaymentOrderExists(merchantId, paymentOrderId);
        String trimmed = body.trim();
        PaymentOrderNote note = PaymentOrderNote.create(paymentOrderId, trimmed, authorDisplay);
        noteRepository.saveAndFlush(note);
        log.info("payment.note.added paymentOrderId={} noteId={} correlationId={}",
                paymentOrderId, note.getId(), MDC.get("correlationId"));
        return new PaymentOrderNoteDto(note.getId(), note.getBody(), note.getAuthorDisplay(), note.getCreatedAt());
    }

    private PaymentOrder requirePaymentOrderExists(UUID merchantId, UUID paymentOrderId) {
        return paymentOrderRepository.findByMerchantIdAndPaymentOrderId(merchantId, paymentOrderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(paymentOrderId));
    }
}

package lab.paymentquality.iam.internal.application;

import lab.paymentquality.iam.internal.domain.PaymentViewFilterWhitelist;
import lab.paymentquality.iam.internal.domain.UserSavedView;
import lab.paymentquality.iam.internal.domain.exception.DuplicateSavedViewException;
import lab.paymentquality.iam.internal.domain.exception.SavedViewNotFoundException;
import lab.paymentquality.iam.internal.domain.exception.SavedViewQuotaExceededException;
import lab.paymentquality.iam.internal.infrastructure.JpaUserSavedViewRepository;
import lab.paymentquality.iam.internal.web.CreateSavedPaymentViewRequest;
import lab.paymentquality.iam.internal.web.UpdateSavedPaymentViewRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SavedPaymentViewService {

    static final int QUOTA = 20;

    private final JpaUserSavedViewRepository repository;

    public SavedPaymentViewService(JpaUserSavedViewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserSavedView> list(String ownerSubject) {
        return repository.findByOwnerSubjectAndResourceOrderByCreatedAtAsc(
                ownerSubject, UserSavedView.PAYMENT_ORDERS);
    }

    @Transactional
    public UserSavedView create(String ownerSubject, CreateSavedPaymentViewRequest request) {
        String name = request.name().strip();
        if (repository.existsByOwnerSubjectAndResourceAndName(ownerSubject, UserSavedView.PAYMENT_ORDERS, name)) {
            throw new DuplicateSavedViewException();
        }
        if (repository.countByOwnerSubjectAndResource(ownerSubject, UserSavedView.PAYMENT_ORDERS) >= QUOTA) {
            throw new SavedViewQuotaExceededException();
        }
        Map<String, Object> filters = PaymentViewFilterWhitelist.validated(request.filters());
        List<String> columns = request.columns() == null ? List.of() : List.copyOf(request.columns());
        boolean isDefault = Boolean.TRUE.equals(request.isDefault());
        if (isDefault) {
            repository.clearDefault(ownerSubject, UserSavedView.PAYMENT_ORDERS);
        }
        UserSavedView created = repository.save(UserSavedView.create(ownerSubject, name, filters, columns, isDefault));
        if (isDefault) {
            repository.flush();
        }
        return created;
    }

    @Transactional
    public UserSavedView update(String ownerSubject, UUID viewId, UpdateSavedPaymentViewRequest request) {
        UserSavedView view = requireOwned(ownerSubject, viewId);
        String name = request.name().strip();
        if (!view.getName().equals(name)
                && repository.existsByOwnerSubjectAndResourceAndName(ownerSubject, UserSavedView.PAYMENT_ORDERS, name)) {
            throw new DuplicateSavedViewException();
        }
        view.rename(name);
        view.replaceFilters(PaymentViewFilterWhitelist.validated(request.filters()));
        view.replaceColumns(request.columns() == null ? List.of() : List.copyOf(request.columns()));
        return view;
    }

    @Transactional
    public void delete(String ownerSubject, UUID viewId) {
        UserSavedView view = requireOwned(ownerSubject, viewId);
        repository.delete(view);
    }

    @Transactional
    public UserSavedView setDefault(String ownerSubject, UUID viewId) {
        UserSavedView view = requireOwned(ownerSubject, viewId);
        repository.clearDefault(ownerSubject, view.getResource());
        repository.flush();
        UserSavedView owned = requireOwned(ownerSubject, viewId);
        owned.markDefault(true);
        return owned;
    }

    private UserSavedView requireOwned(String ownerSubject, UUID viewId) {
        return repository.findByViewIdAndOwnerSubject(viewId, ownerSubject)
                .orElseThrow(SavedViewNotFoundException::new);
    }
}

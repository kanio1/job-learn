package lab.paymentquality.iam.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_saved_views")
public class UserSavedView {

    public static final String PAYMENT_ORDERS = "PAYMENT_ORDERS";

    @Id
    @Column(name = "view_id", nullable = false, updatable = false)
    private UUID viewId;

    @Column(name = "owner_subject", nullable = false, length = 255, updatable = false)
    private String ownerSubject;

    @Column(name = "resource", nullable = false, length = 32, updatable = false)
    private String resource;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "filters", nullable = false)
    private Map<String, Object> filters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "columns", nullable = false)
    private List<String> columns;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserSavedView() {
    }

    public static UserSavedView create(
            String ownerSubject,
            String name,
            Map<String, Object> filters,
            List<String> columns,
            boolean isDefault) {
        Instant now = Instant.now();
        UserSavedView view = new UserSavedView();
        view.viewId = UUID.randomUUID();
        view.ownerSubject = ownerSubject;
        view.resource = PAYMENT_ORDERS;
        view.name = name;
        view.filters = new LinkedHashMap<>(filters);
        view.columns = new ArrayList<>(columns);
        view.isDefault = isDefault;
        view.createdAt = now;
        view.updatedAt = now;
        return view;
    }

    public void rename(String nextName) {
        this.name = nextName;
        touch();
    }

    public void replaceFilters(Map<String, Object> nextFilters) {
        this.filters = new LinkedHashMap<>(nextFilters);
        touch();
    }

    public void replaceColumns(List<String> nextColumns) {
        this.columns = new ArrayList<>(nextColumns);
        touch();
    }

    public void markDefault(boolean nextDefault) {
        this.isDefault = nextDefault;
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getViewId() {
        return viewId;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }

    public String getResource() {
        return resource;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public List<String> getColumns() {
        return columns;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

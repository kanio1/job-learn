package lab.paymentquality.iam.internal.infrastructure;

import lab.paymentquality.iam.internal.domain.UserSavedView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaUserSavedViewRepository extends JpaRepository<UserSavedView, UUID> {

    List<UserSavedView> findByOwnerSubjectAndResourceOrderByCreatedAtAsc(String ownerSubject, String resource);

    Optional<UserSavedView> findByViewIdAndOwnerSubject(UUID viewId, String ownerSubject);

    boolean existsByOwnerSubjectAndResourceAndName(String ownerSubject, String resource, String name);

    long countByOwnerSubjectAndResource(String ownerSubject, String resource);

    @Modifying(flushAutomatically = true)
    @Query("""
            update UserSavedView v
               set v.isDefault = false, v.updatedAt = CURRENT_TIMESTAMP
             where v.ownerSubject = :owner
               and v.resource = :resource
               and v.isDefault = true
            """)
    int clearDefault(@Param("owner") String owner, @Param("resource") String resource);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update user_saved_views
               set is_default = (view_id = :id),
                   updated_at = now()
             where owner_subject = :owner
               and resource = :resource
            """, nativeQuery = true)
    int assignDefault(
            @Param("owner") String owner,
            @Param("resource") String resource,
            @Param("id") UUID id);
}

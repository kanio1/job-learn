package lab.paymentquality.mirrorlab.internal.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mrl_consents")
public class MirrorLabConsent {

    @Id
    @Column(name = "consent_id")
    private UUID consentId;

    @Column(name = "access_token", nullable = false, length = 64)
    private String accessToken;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "owner_subject", nullable = false, length = 128)
    private String ownerSubject;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected MirrorLabConsent() {
    }

    public static MirrorLabConsent grant(String owner, String token, Instant now) {
        MirrorLabConsent consent = new MirrorLabConsent();
        consent.consentId = UUID.randomUUID();
        consent.accessToken = token;
        consent.status = "GRANTED";
        consent.ownerSubject = owner;
        consent.createdAt = now;
        return consent;
    }

    public void revoke(Instant now) {
        this.status = "REVOKED";
        this.revokedAt = now;
    }

    public UUID getConsentId() {
        return consentId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getStatus() {
        return status;
    }

    public String getOwnerSubject() {
        return ownerSubject;
    }
}

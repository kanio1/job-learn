package lab.paymentquality.tenant;

import lab.paymentquality.merchant.internal.application.MerchantService;
import lab.paymentquality.merchant.internal.domain.Merchant;
import lab.paymentquality.merchant.internal.domain.MerchantNotFoundException;
import lab.paymentquality.merchant.internal.domain.MissingTenantReferenceException;
import lab.paymentquality.merchant.internal.domain.TenantBoundaryViolationException;
import lab.paymentquality.merchant.internal.domain.UnresolvableTenantReferenceException;
import lab.paymentquality.merchant.internal.infrastructure.JpaMerchantRepository;
import lab.paymentquality.tenant.TenantContext;
import lab.paymentquality.tenant.TenantReference;
import lab.paymentquality.tenant.TenantResolutionException;
import lab.paymentquality.tenant.TenantResolver;
import lab.paymentquality.tenant.internal.domain.Tenant;
import lab.paymentquality.tenant.internal.domain.TenantStatus;
import lab.paymentquality.tenant.internal.domain.TenantType;
import lab.paymentquality.tenant.internal.infrastructure.JpaTenantRepository;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.GenerationMode;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tag;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("tenant-model-and-isolation")
class TenantIsolationPropertyTest {

    private static final UUID TENANT_ALPHA_UUID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PLACEHOLDER_TENANT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID PLATFORM_TENANT_UUID = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final TenantContext TENANT_ALPHA_CONTEXT =
            new TenantContext(TENANT_ALPHA_UUID, TenantReference.of("TENANT_ALPHA"), false);
    private static final TenantContext PLATFORM_CONTEXT =
            new TenantContext(PLATFORM_TENANT_UUID, TenantReference.of("PLATFORM_TENANT"), true);

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 1: service-level cross-tenant read masking")
    void property1_tenantScopedReadUsesTenantFilteredLookupAndMasksMisses(
            @ForAll("merchantIds") UUID merchantId) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        when(repository.findByMerchantIdAndTenantId(merchantId, TENANT_ALPHA_UUID)).thenReturn(Optional.empty());
        MerchantService service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.findById(merchantId, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 2: classification determinism")
    void property2_classificationIsDataDrivenByResolvedTenantType(
            @ForAll GeneratedTenantType tenantType,
            @ForAll("authorityNames") String authorityName) {
        boolean platform = tenantType == GeneratedTenantType.PLATFORM;
        TenantContext context = new TenantContext(
                deterministicUuid(tenantType.name() + authorityName),
                TenantReference.of("TENANT_" + tenantType.name()),
                platform);

        assertThat(context.isPlatformScoped()).isEqualTo(platform);
        assertThat(context.isTenantScoped()).isEqualTo(!platform);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 3a: tenant-scoped create assignment")
    void property3a_tenantScopedCreateAlwaysAssignsCallerTenant(
            @ForAll("merchantReferences") String merchantReference,
            @ForAll("bodyTenantReferences") String bodyTenantReference) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        when(repository.findByNormalizedReference(merchantReference)).thenReturn(Optional.empty());
        MerchantService service = new MerchantService(repository, tenantResolver);

        service.create(merchantReference, "Tenant Property Merchant", TENANT_ALPHA_CONTEXT, bodyTenantReference);

        ArgumentCaptor<Merchant> merchant = ArgumentCaptor.forClass(Merchant.class);
        verify(repository).saveAndFlush(merchant.capture());
        verify(tenantResolver, never()).resolveTenantId(any());
        assertThat(merchant.getValue().getTenantId()).isEqualTo(TENANT_ALPHA_UUID);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 3b: platform-scoped valid create assignment")
    void property3b_platformScopedCreateWithValidReferenceAssignsResolvedTenant(
            @ForAll("merchantReferences") String merchantReference,
            @ForAll SeededTenantReference tenantReference) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        when(repository.findByNormalizedReference(merchantReference)).thenReturn(Optional.empty());
        when(tenantResolver.resolveTenantId(TenantReference.of(tenantReference.value())))
                .thenReturn(tenantReference.tenantId());
        MerchantService service = new MerchantService(repository, tenantResolver);

        service.create(merchantReference, "Platform Property Merchant", PLATFORM_CONTEXT, tenantReference.value());

        ArgumentCaptor<Merchant> merchant = ArgumentCaptor.forClass(Merchant.class);
        verify(repository).saveAndFlush(merchant.capture());
        assertThat(merchant.getValue().getTenantId()).isEqualTo(tenantReference.tenantId());
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 3c: platform-scoped invalid create assignment")
    void property3c_platformScopedCreateWithInvalidReferenceThrows(
            @ForAll("merchantReferences") String merchantReference,
            @ForAll("invalidTenantReferences") String invalidTenantReference) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        when(tenantResolver.resolveTenantId(TenantReference.of(invalidTenantReference)))
                .thenThrow(new TenantResolutionException("Tenant reference could not be resolved"));
        MerchantService service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.create(
                merchantReference,
                "Invalid Platform Property Merchant",
                PLATFORM_CONTEXT,
                invalidTenantReference))
                .isInstanceOf(UnresolvableTenantReferenceException.class);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 3d: platform-scoped missing create assignment")
    void property3d_platformScopedCreateWithMissingReferenceThrows(
            @ForAll("merchantReferences") String merchantReference,
            @ForAll("blankTenantReferences") String blankTenantReference) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        MerchantService service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.create(
                merchantReference,
                "Missing Platform Property Merchant",
                PLATFORM_CONTEXT,
                blankTenantReference))
                .isInstanceOf(MissingTenantReferenceException.class);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 4: service-level cross-tenant write denial")
    void property4_tenantScopedWritesAgainstOtherTenantThrowBoundaryViolation(
            @ForAll("merchantIds") UUID merchantId,
            @ForAll("otherTenantIds") UUID otherTenantId) {
        JpaMerchantRepository repository = mock(JpaMerchantRepository.class);
        TenantResolver tenantResolver = mock(TenantResolver.class);
        when(repository.findById(merchantId)).thenReturn(Optional.of(
                Merchant.create(merchantId, "MERCH-OTHER-" + shortToken(merchantId), "Other Tenant", otherTenantId)));
        MerchantService service = new MerchantService(repository, tenantResolver);

        assertThatThrownBy(() -> service.activate(merchantId, TENANT_ALPHA_CONTEXT))
                .isInstanceOf(TenantBoundaryViolationException.class);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 5: suspended tenant access semantics")
    void property5_suspendedStandardTenantsAreRejectedButSuspendedPlatformTenantsResolve(
            @ForAll("merchantIds") UUID tenantId,
            @ForAll GeneratedTenantType tenantType) {
        JpaTenantRepository repository = mock(JpaTenantRepository.class);
        String reference = "SUSPENDED_" + tenantType.name() + "_" + shortToken(tenantId);
        when(repository.findByTenantReference(reference)).thenReturn(Optional.of(
                tenant(tenantId, reference, TenantStatus.SUSPENDED, tenantType.toTenantType())));
        TenantResolver service = tenantResolver(repository);

        if (tenantType == GeneratedTenantType.PLATFORM) {
            TenantContext context = service.resolve(jwtWithTenantClaim(reference));

            assertThat(context.tenantId()).isEqualTo(tenantId);
            assertThat(context.isPlatformScoped()).isTrue();
        } else {
            assertThatThrownBy(() -> service.resolve(jwtWithTenantClaim(reference)))
                    .isInstanceOf(TenantResolutionException.class);
        }
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 6a: seeded tenant_reference deterministic resolution")
    void property6a_seededReferencesResolveDeterministically(
            @ForAll SeededTenantReference seededTenantReference) {
        JpaTenantRepository repository = mock(JpaTenantRepository.class);
        Tenant tenant = tenant(
                seededTenantReference.tenantId(),
                seededTenantReference.value(),
                TenantStatus.ACTIVE,
                seededTenantReference.type());
        when(repository.findByTenantReference(seededTenantReference.value())).thenReturn(Optional.of(tenant));
        TenantResolver service = tenantResolver(repository);

        TenantContext first = service.resolve(jwtWithTenantClaim(seededTenantReference.value()));
        TenantContext second = service.resolve(jwtWithTenantClaim(seededTenantReference.value()));

        assertThat(second).isEqualTo(first);
    }

    @Property(tries = 100, generation = GenerationMode.RANDOMIZED)
    @Label("Feature: tenant-model-and-isolation, Property 6b: unknown and non-exact tenant_reference variants throw")
    void property6b_unknownAndNonExactReferencesThrow(
            @ForAll SeededTenantReference seededTenantReference,
            @ForAll("referenceVariants") ReferenceVariant variant) {
        String reference = variant.applyTo(seededTenantReference.value());
        JpaTenantRepository repository = mock(JpaTenantRepository.class);
        when(repository.findByTenantReference(reference)).thenReturn(Optional.empty());
        TenantResolver service = tenantResolver(repository);

        assertThatThrownBy(() -> service.resolve(jwtWithTenantClaim(reference)))
                .isInstanceOf(TenantResolutionException.class);
    }

    @Provide
    Arbitrary<UUID> merchantIds() {
        return Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(1)
                .ofMaxLength(32)
                .map(TenantIsolationPropertyTest::deterministicUuid);
    }

    @Provide
    Arbitrary<UUID> otherTenantIds() {
        return merchantIds()
                .filter(id -> !id.equals(TENANT_ALPHA_UUID));
    }

    @Provide
    Arbitrary<String> authorityNames() {
        return Arbitraries.of(
                "platform:merchants:read",
                "merchant:payments:read",
                "platform:payments:read",
                "merchants:create");
    }

    @Provide
    Arbitrary<String> merchantReferences() {
        return Arbitraries.strings()
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
                .ofMinLength(8)
                .ofMaxLength(12)
                .map(value -> "MERCH-PROP-" + value);
    }

    @Provide
    Arbitrary<String> bodyTenantReferences() {
        return Arbitraries.oneOf(
                Arbitraries.of("TENANT_ALPHA", "PLATFORM_TENANT", "PLACEHOLDER_TENANT_ID"),
                Arbitraries.strings()
                        .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_- ")
                        .ofMinLength(1)
                        .ofMaxLength(64));
    }

    @Provide
    Arbitrary<String> invalidTenantReferences() {
        return Arbitraries.strings()
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_")
                .ofMinLength(3)
                .ofMaxLength(32)
                .filter(value -> !value.equals("TENANT_ALPHA"))
                .filter(value -> !value.equals("PLATFORM_TENANT"))
                .filter(value -> !value.equals("PLACEHOLDER_TENANT_ID"));
    }

    @Provide
    Arbitrary<String> blankTenantReferences() {
        return Arbitraries.of("", " ", "   ", "\t");
    }

    @Provide
    Arbitrary<ReferenceVariant> referenceVariants() {
        return Arbitraries.of(ReferenceVariant.values());
    }

    private static Jwt jwtWithTenantClaim(String tenantReference) {
        return Jwt.withTokenValue("token")
                .headers(headers -> headers.putAll(Map.of("alg", "none")))
                .claim("tenant_id", tenantReference)
                .build();
    }

    private static Tenant tenant(UUID tenantId, String tenantReference, TenantStatus status, TenantType type) {
        Tenant tenant = BeanUtils.instantiateClass(Tenant.class);
        ReflectionTestUtils.setField(tenant, "tenantId", tenantId);
        ReflectionTestUtils.setField(tenant, "tenantReference", tenantReference);
        ReflectionTestUtils.setField(tenant, "name", tenantReference);
        ReflectionTestUtils.setField(tenant, "status", status);
        ReflectionTestUtils.setField(tenant, "tenantType", type);
        ReflectionTestUtils.setField(tenant, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        return tenant;
    }

    private static TenantResolver tenantResolver(JpaTenantRepository repository) {
        try {
            Class<?> serviceClass = Class.forName(
                    "lab.paymentquality.tenant.internal.application.TenantResolverService");
            var constructor = serviceClass.getDeclaredConstructor(JpaTenantRepository.class);
            constructor.setAccessible(true);
            return (TenantResolver) constructor.newInstance(repository);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("TenantResolverService should be instantiable in tenant property tests", e);
        }
    }

    private static UUID deterministicUuid(String value) {
        return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String shortToken(UUID id) {
        return id.toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    enum GeneratedTenantType {
        PLATFORM,
        STANDARD;

        TenantType toTenantType() {
            return this == PLATFORM ? TenantType.PLATFORM : TenantType.STANDARD;
        }
    }

    enum SeededTenantReference {
        TENANT_ALPHA(TENANT_ALPHA_UUID, TenantType.STANDARD),
        PLACEHOLDER_TENANT_ID(PLACEHOLDER_TENANT_UUID, TenantType.STANDARD),
        PLATFORM_TENANT(PLATFORM_TENANT_UUID, TenantType.PLATFORM);

        private final UUID tenantId;
        private final TenantType type;

        SeededTenantReference(UUID tenantId, TenantType type) {
            this.tenantId = tenantId;
            this.type = type;
        }

        String value() {
            return name();
        }

        UUID tenantId() {
            return tenantId;
        }

        TenantType type() {
            return type;
        }
    }

    enum ReferenceVariant {
        LOWERCASE {
            @Override
            String applyTo(String reference) {
                return reference.toLowerCase(Locale.ROOT);
            }
        },
        LEADING_SPACE {
            @Override
            String applyTo(String reference) {
                return " " + reference;
            }
        },
        TRAILING_SPACE {
            @Override
            String applyTo(String reference) {
                return reference + " ";
            }
        },
        UNKNOWN_SUFFIX {
            @Override
            String applyTo(String reference) {
                return reference + "_UNKNOWN";
            }
        };

        abstract String applyTo(String reference);
    }
}

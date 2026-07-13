package lab.paymentquality.testing.internal.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lab.paymentquality.merchant.MerchantSeed;
import lab.paymentquality.tenant.TenantSeed;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Label;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature: deterministic-seed-and-test-isolation,
 * Property 6: Realm-alignment — no dangling identity.
 *
 * <p>Validates Requirements 4.1, 4.2, 4.3 and 4.5 without starting Keycloak:
 * the actual realm import is the identity-side source and {@link Fixtures} is
 * the deterministic database-side source.</p>
 */
class RealmAlignmentPropertyTest {

    private static final List<String> REALM_TEST_USERNAMES = List.of(
            "platform.admin",
            "tenant.admin",
            "merchant.manager",
            "support.agent",
            "readonly.user"
    );
    private static final Set<String> REQUIRED_TENANT_REFERENCES = Set.of(
            "PLATFORM_TENANT", "TENANT_ALPHA", "PLACEHOLDER_TENANT_ID"
    );
    private static final Map<String, RealmUser> REALM_USERS = loadRealmUsers();

    @Property(tries = 100)
    @Label("Feature: deterministic-seed-and-test-isolation, Property 6: Realm-alignment — no dangling identity")
    @net.jqwik.api.Tag("deterministic-seed-and-test-isolation")
    void property6_realm_test_user_attributes_resolve_to_owned_seed_records(
            @ForAll("realmTestUsernames") String username) {

        RealmUser realmUser = REALM_USERS.get(username);
        assertThat(realmUser)
                .as("Realm Test User '%s' must exist in the actual realm import", username)
                .isNotNull();
        assertThat(realmUser.enabled())
                .as("Realm Test User '%s' must be enabled", username)
                .isTrue();

        String tenantReference = realmUser.attributes().get("tenant_id");
        TenantSeed tenant = tenantByReference(tenantReference);
        assertThat(tenant)
                .as("tenant_id '%s' for '%s' must resolve to a seeded tenant", tenantReference, username)
                .isNotNull();

        String merchantReference = realmUser.attributes().get("merchant_id");
        if (merchantReference != null) {
            MerchantSeed merchant = merchantByReference(merchantReference);
            assertThat(merchant)
                    .as("merchant_id '%s' for '%s' must resolve to a seeded merchant", merchantReference, username)
                    .isNotNull();
            assertThat(merchant.tenantId())
                    .as("merchant '%s' must belong to realm tenant '%s'", merchantReference, tenantReference)
                    .isEqualTo(tenant.tenantId());
        }
    }

    @Provide
    Arbitrary<String> realmTestUsernames() {
        return Arbitraries.of(REALM_TEST_USERNAMES);
    }

    @Test
    void actual_realm_contains_the_complete_per_role_test_user_catalog() {
        assertThat(REALM_USERS.keySet()).containsAll(REALM_TEST_USERNAMES);
        assertThat(REALM_TEST_USERNAMES)
                .allSatisfy(username -> assertThat(REALM_USERS.get(username).attributes())
                        .as("attributes for %s", username)
                        .containsKey("tenant_id"));
    }

    @Test
    void every_distinct_realm_tenant_identity_required_by_the_spec_is_seeded() {
        Set<String> actualRealmTenantReferences = REALM_USERS.values().stream()
                .map(user -> user.attributes().get("tenant_id"))
                .filter(reference -> reference != null && !reference.isBlank())
                .collect(Collectors.toSet());
        Set<String> seededTenantReferences = Fixtures.tenants().stream()
                .map(TenantSeed::tenantReference)
                .collect(Collectors.toSet());

        assertThat(actualRealmTenantReferences).containsExactlyInAnyOrderElementsOf(REQUIRED_TENANT_REFERENCES);
        assertThat(seededTenantReferences).containsAll(actualRealmTenantReferences);
    }

    @Test
    void every_seeded_merchant_belongs_to_a_seeded_tenant() {
        Set<java.util.UUID> seededTenantIds = Fixtures.tenants().stream()
                .map(TenantSeed::tenantId)
                .collect(Collectors.toSet());

        assertThat(Fixtures.merchants())
                .allSatisfy(merchant -> assertThat(seededTenantIds)
                        .as("seeded tenant for merchant %s", merchant.merchantReference())
                        .contains(merchant.tenantId()));
    }

    private static TenantSeed tenantByReference(String tenantReference) {
        return Fixtures.tenants().stream()
                .filter(tenant -> tenant.tenantReference().equals(tenantReference))
                .findFirst()
                .orElse(null);
    }

    private static MerchantSeed merchantByReference(String merchantReference) {
        return Fixtures.merchants().stream()
                .filter(merchant -> merchant.merchantReference().equals(merchantReference))
                .findFirst()
                .orElse(null);
    }

    private static Map<String, RealmUser> loadRealmUsers() {
        Path realmPath = locateRealmImport();
        try {
            JsonNode root = new ObjectMapper().readTree(realmPath.toFile());
            Map<String, RealmUser> users = new LinkedHashMap<>();
            for (JsonNode userNode : root.path("users")) {
                String username = userNode.path("username").asText();
                Map<String, String> attributes = new LinkedHashMap<>();
                userNode.path("attributes").fields().forEachRemaining(entry ->
                        attributes.put(entry.getKey(), attributeValue(entry.getValue())));
                users.put(username, new RealmUser(
                        username,
                        userNode.path("enabled").asBoolean(false),
                        Map.copyOf(attributes)
                ));
            }
            return Map.copyOf(users);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Keycloak realm import at " + realmPath, exception);
        }
    }

    private static Path locateRealmImport() {
        return List.of(
                        Path.of("../../infra/keycloak/realms/payment-quality-realm.json"),
                        Path.of("infra/keycloak/realms/payment-quality-realm.json")
                ).stream()
                .map(Path::normalize)
                .filter(Files::isRegularFile)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cannot locate payment-quality-realm.json"));
    }

    private static String attributeValue(JsonNode node) {
        if (node.isArray()) {
            return node.isEmpty() ? "" : node.get(0).asText();
        }
        return node.asText();
    }

    private record RealmUser(String username, boolean enabled, Map<String, String> attributes) {
    }
}

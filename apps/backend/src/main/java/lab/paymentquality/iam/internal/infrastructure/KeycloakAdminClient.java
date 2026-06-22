package lab.paymentquality.iam.internal.infrastructure;

import lab.paymentquality.iam.internal.domain.CompositeRole;
import lab.paymentquality.iam.internal.domain.ManagedUser;
import lab.paymentquality.iam.internal.domain.exception.DuplicateUserException;
import lab.paymentquality.iam.internal.domain.exception.InvalidRoleException;
import lab.paymentquality.iam.internal.domain.exception.KeycloakAdminUnavailableException;
import lab.paymentquality.iam.internal.domain.exception.UserNotFoundException;
import lab.paymentquality.iam.internal.web.dto.CreateUserRequest;
import lab.paymentquality.iam.internal.web.dto.UpdateUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class KeycloakAdminClient {

    private static final String TENANT_ID_ATTRIBUTE = "tenant_id";
    private static final String MERCHANT_ID_ATTRIBUTE = "merchant_id";

    private final KeycloakAdminProperties properties;
    private final KeycloakAdminTokenProvider tokenProvider;
    private final RestClient restClient;

    @Autowired
    public KeycloakAdminClient(
            KeycloakAdminProperties properties,
            KeycloakAdminTokenProvider tokenProvider) {
        this(properties, tokenProvider, RestClient.create());
    }

    KeycloakAdminClient(
            KeycloakAdminProperties properties,
            KeycloakAdminTokenProvider tokenProvider,
            RestClient restClient) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restClient = restClient;
    }

    public Optional<ManagedUser> getUser(String id) {
        KeycloakUserRepresentation user = execute(
                token -> restClient.get()
                        .uri(adminUri("/users/{id}"), id)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .body(KeycloakUserRepresentation.class),
                FailureMode.OPTIONAL_NOT_FOUND);

        if (user == null) {
            return Optional.empty();
        }
        return Optional.of(toManagedUser(user));
    }

    public List<ManagedUser> listUsers(int first, int max, String search) {
        URI uri = UriComponentsBuilder.fromUriString(adminUri("/users"))
                .queryParam("first", first)
                .queryParam("max", max)
                .queryParamIfPresent("search", optionalText(search))
                .build()
                .encode()
                .toUri();

        KeycloakUserRepresentation[] users = execute(
                token -> restClient.get()
                        .uri(uri)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .body(KeycloakUserRepresentation[].class),
                FailureMode.ADMIN_UNAVAILABLE);

        if (users == null || users.length == 0) {
            return List.of();
        }

        List<ManagedUser> managedUsers = new ArrayList<>(users.length);
        for (KeycloakUserRepresentation user : users) {
            managedUsers.add(toManagedUser(user));
        }
        return List.copyOf(managedUsers);
    }

    public List<String> getRealmCompositeRoleNames(String id) {
        RoleRepresentation[] roles = execute(
                token -> restClient.get()
                        .uri(adminUri("/users/{id}/role-mappings/realm"), id)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .body(RoleRepresentation[].class),
                FailureMode.USER_NOT_FOUND);

        if (roles == null || roles.length == 0) {
            return List.of();
        }

        List<String> roleNames = new ArrayList<>();
        for (RoleRepresentation role : roles) {
            if (role != null && role.name() != null && CompositeRole.isAssignable(role.name())) {
                roleNames.add(role.name());
            }
        }
        roleNames.sort(String::compareTo);
        return List.copyOf(roleNames);
    }

    public String createUser(CreateUserRequest request, String tenantReference) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put(TENANT_ID_ATTRIBUTE, List.of(tenantReference));
        if (StringUtils.hasText(request.merchantId())) {
            attributes.put(MERCHANT_ID_ATTRIBUTE, List.of(request.merchantId().strip()));
        }

        CreateUserPayload payload = new CreateUserPayload(
                request.username(),
                request.email(),
                true,
                Map.copyOf(attributes));

        ResponseEntity<Void> response = execute(
                token -> restClient.post()
                        .uri(adminUri("/users"))
                        .headers(headers -> headers.setBearerAuth(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity(),
                FailureMode.DUPLICATE_ON_CONFLICT);

        return createdUserId(response.getHeaders().getLocation());
    }

    public void setTemporaryPassword(String id, String password) {
        PasswordCredentialPayload payload = new PasswordCredentialPayload(password);
        execute(
                token -> {
                    restClient.put()
                            .uri(adminUri("/users/{id}/reset-password"), id)
                            .headers(headers -> headers.setBearerAuth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                FailureMode.USER_NOT_FOUND);
    }

    public void updateUser(String id, ManagedUser current, UpdateUserRequest request) {
        KeycloakUserRepresentation latest = execute(
                token -> restClient.get()
                        .uri(adminUri("/users/{id}"), id)
                        .headers(headers -> headers.setBearerAuth(token))
                        .retrieve()
                        .body(KeycloakUserRepresentation.class),
                FailureMode.USER_NOT_FOUND);

        Map<String, List<String>> attributes = mutableAttributes(latest.attributes());
        preserveKnownAttribute(attributes, TENANT_ID_ATTRIBUTE, current.tenantId());
        preserveKnownAttribute(attributes, MERCHANT_ID_ATTRIBUTE, current.merchantId());
        if (request.attributes() != null) {
            request.attributes().forEach((name, values) -> attributes.put(name, List.copyOf(values)));
        }

        UpdateUserPayload payload = new UpdateUserPayload(
                current.username(),
                request.email() == null ? current.email() : request.email(),
                request.enabled() == null ? current.enabled() : request.enabled(),
                Map.copyOf(attributes));

        execute(
                token -> {
                    restClient.put()
                            .uri(adminUri("/users/{id}"), id)
                            .headers(headers -> headers.setBearerAuth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                FailureMode.DUPLICATE_OR_NOT_FOUND);
    }

    public void assignRealmComposites(String id, Collection<String> roleNames) {
        updateRealmComposites(id, roleNames, false);
    }

    public void removeRealmComposites(String id, Collection<String> roleNames) {
        updateRealmComposites(id, roleNames, true);
    }

    private void updateRealmComposites(String id, Collection<String> roleNames, boolean remove) {
        List<RoleRepresentation> roles = resolveRealmComposites(roleNames);
        if (roles.isEmpty()) {
            return;
        }

        execute(
                token -> {
                    RestClient.RequestBodySpec request = restClient
                            .method(remove ? HttpMethod.DELETE : HttpMethod.POST)
                            .uri(adminUri("/users/{id}/role-mappings/realm"), id);
                    request.headers(headers -> headers.setBearerAuth(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(roles)
                            .retrieve()
                            .toBodilessEntity();
                    return null;
                },
                FailureMode.USER_NOT_FOUND);
    }

    private List<RoleRepresentation> resolveRealmComposites(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }

        List<RoleRepresentation> roles = new ArrayList<>(roleNames.size());
        for (String roleName : roleNames) {
            if (!CompositeRole.isAssignable(roleName)) {
                throw new InvalidRoleException();
            }
            RoleRepresentation role = execute(
                    token -> restClient.get()
                            .uri(adminUri("/roles/{roleName}"), roleName)
                            .headers(headers -> headers.setBearerAuth(token))
                            .retrieve()
                            .body(RoleRepresentation.class),
                    FailureMode.INVALID_ROLE_NOT_FOUND);
            roles.add(role);
        }
        return List.copyOf(roles);
    }

    private ManagedUser toManagedUser(KeycloakUserRepresentation user) {
        return new ManagedUser(
                user.id(),
                user.username(),
                user.email(),
                Boolean.TRUE.equals(user.enabled()),
                firstAttribute(user.attributes(), TENANT_ID_ATTRIBUTE),
                firstAttribute(user.attributes(), MERCHANT_ID_ATTRIBUTE),
                getRealmCompositeRoleNames(user.id()));
    }

    private <T> T execute(AdminCall<T> call, FailureMode failureMode) {
        String token = tokenProvider.getAdminToken();
        try {
            return call.execute(token);
        } catch (HttpClientErrorException.Unauthorized exception) {
            String refreshedToken = tokenProvider.refreshAfterUnauthorized(token);
            try {
                return call.execute(refreshedToken);
            } catch (RestClientResponseException retryException) {
                return mapResponseFailure(retryException, failureMode);
            } catch (RestClientException retryException) {
                throw new KeycloakAdminUnavailableException();
            }
        } catch (RestClientResponseException exception) {
            return mapResponseFailure(exception, failureMode);
        } catch (RestClientException exception) {
            throw new KeycloakAdminUnavailableException();
        }
    }

    private <T> T mapResponseFailure(RestClientResponseException exception, FailureMode failureMode) {
        if (exception.getStatusCode().value() == 404) {
            return switch (failureMode) {
                case OPTIONAL_NOT_FOUND -> null;
                case INVALID_ROLE_NOT_FOUND -> throw new InvalidRoleException();
                case USER_NOT_FOUND, DUPLICATE_OR_NOT_FOUND -> throw new UserNotFoundException();
                case ADMIN_UNAVAILABLE, DUPLICATE_ON_CONFLICT ->
                        throw new KeycloakAdminUnavailableException();
            };
        }
        if (exception.getStatusCode().value() == 409
                && (failureMode == FailureMode.DUPLICATE_ON_CONFLICT
                || failureMode == FailureMode.DUPLICATE_OR_NOT_FOUND)) {
            throw new DuplicateUserException();
        }
        throw new KeycloakAdminUnavailableException();
    }

    private String adminUri(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + "/admin/realms/" + properties.getRealm() + path;
    }

    private static String createdUserId(URI location) {
        if (location == null || !StringUtils.hasText(location.getPath())) {
            throw new KeycloakAdminUnavailableException();
        }
        String path = location.getPath();
        int separator = path.lastIndexOf('/');
        if (separator < 0 || separator == path.length() - 1) {
            throw new KeycloakAdminUnavailableException();
        }
        return path.substring(separator + 1);
    }

    private static Optional<String> optionalText(String value) {
        return StringUtils.hasText(value) ? Optional.of(value.strip()) : Optional.empty();
    }

    private static Map<String, List<String>> mutableAttributes(Map<String, List<String>> source) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        if (source != null) {
            source.forEach((name, values) -> copy.put(
                    name,
                    values == null ? List.of() : List.copyOf(values)));
        }
        return copy;
    }

    private static void preserveKnownAttribute(
            Map<String, List<String>> attributes,
            String name,
            String value) {
        if (!attributes.containsKey(name) && StringUtils.hasText(value)) {
            attributes.put(name, List.of(value));
        }
    }

    private static String firstAttribute(Map<String, List<String>> attributes, String name) {
        if (attributes == null) {
            return null;
        }
        List<String> values = attributes.get(name);
        return values == null || values.isEmpty() ? null : values.getFirst();
    }

    @FunctionalInterface
    private interface AdminCall<T> {
        T execute(String token);
    }

    private enum FailureMode {
        OPTIONAL_NOT_FOUND,
        USER_NOT_FOUND,
        INVALID_ROLE_NOT_FOUND,
        ADMIN_UNAVAILABLE,
        DUPLICATE_ON_CONFLICT,
        DUPLICATE_OR_NOT_FOUND
    }

    private record KeycloakUserRepresentation(
            String id,
            String username,
            String email,
            Boolean enabled,
            Map<String, List<String>> attributes) {
    }

    private record RoleRepresentation(
            String id,
            String name,
            Boolean composite,
            Boolean clientRole,
            String containerId) {
    }

    private record CreateUserPayload(
            String username,
            String email,
            boolean enabled,
            Map<String, List<String>> attributes) {
    }

    private record UpdateUserPayload(
            String username,
            String email,
            boolean enabled,
            Map<String, List<String>> attributes) {
    }

    private static final class PasswordCredentialPayload {

        private final String type = "password";
        private final String value;
        private final boolean temporary = true;

        private PasswordCredentialPayload(String value) {
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }

        public boolean isTemporary() {
            return temporary;
        }
    }
}

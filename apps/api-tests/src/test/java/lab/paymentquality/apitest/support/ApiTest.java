package lab.paymentquality.apitest.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for live black-box API specs.
 *
 * <p>Composes {@link ApiStackExtension} (starts/manages the Testcontainers stack)
 * and tags the test class as {@code live} (for filtering in CI).
 *
 * <p>Only applicable to {@code *Spec.java} classes executed by {@code maven-failsafe-plugin}
 * via {@code mvn verify}. Never place this on {@code *Test.java} unit wiring tests —
 * they must remain offline and dependency-free.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ApiStackExtension.class)
@Tag("live")
public @interface ApiTest {
}

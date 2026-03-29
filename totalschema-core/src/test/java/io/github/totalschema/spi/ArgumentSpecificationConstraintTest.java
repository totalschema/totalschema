package io.github.totalschema.spi;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;
import static org.testng.Assert.*;

import io.github.totalschema.spi.factory.ArgumentSpecification;
import io.github.totalschema.spi.factory.Constraint;
import org.testng.annotations.Test;

/** Tests for ArgumentSpecification constraint validation functionality. */
public class ArgumentSpecificationConstraintTest {

    @Test
    public void testStringLengthConstraint() {
        ArgumentSpecification<String> spec = string("host", 1, 255);

        // Valid length
        spec.validateValue("localhost");

        // Test minimum length
        spec.validateValue("a");

        // Test maximum length
        String maxString = "a".repeat(255);
        spec.validateValue(maxString);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStringTooShort() {
        ArgumentSpecification<String> spec = string("host", 5, 255);
        spec.validateValue("abc"); // Only 3 characters, should fail
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testStringTooLong() {
        ArgumentSpecification<String> spec = string("host", 1, 10);
        spec.validateValue("this is way too long"); // More than 10 characters
    }

    @Test
    public void testIntegerRangeConstraint() {
        ArgumentSpecification<Integer> spec = integer("port", 1, 65535);

        // Valid ports
        spec.validateValue(1);
        spec.validateValue(8080);
        spec.validateValue(65535);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIntegerTooSmall() {
        ArgumentSpecification<Integer> spec = integer("port", 1, 65535);
        spec.validateValue(0); // Below minimum
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testIntegerTooLarge() {
        ArgumentSpecification<Integer> spec = integer("port", 1, 65535);
        spec.validateValue(99999); // Above maximum
    }

    @Test
    public void testPatternConstraint() {
        ArgumentSpecification<String> spec =
                string("protocol").withConstraint(pattern("^(http|https|ftp)$"));

        // Valid protocols
        spec.validateValue("http");
        spec.validateValue("https");
        spec.validateValue("ftp");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testPatternConstraintFails() {
        ArgumentSpecification<String> spec =
                string("protocol").withConstraint(pattern("^(http|https)$"));
        spec.validateValue("ftp"); // Not in allowed list
    }

    @Test
    public void testCustomConstraint() {
        ArgumentSpecification<Integer> spec =
                new ArgumentSpecification<>(Integer.class, "retryCount")
                        .withConstraint(range(0, 10))
                        .withConstraint(custom(n -> n % 2 == 0, "must be even number"));

        // Valid values
        spec.validateValue(0);
        spec.validateValue(2);
        spec.validateValue(10);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testCustomConstraintFails() {
        ArgumentSpecification<Integer> spec =
                new ArgumentSpecification<>(Integer.class, "retryCount")
                        .withConstraint(custom(n -> n % 2 == 0, "must be even number"));

        spec.validateValue(3); // Odd number should fail
    }

    @Test
    public void testMultipleConstraints() {
        ArgumentSpecification<String> spec =
                string("email")
                        .withConstraint(minLength(5))
                        .withConstraint(maxLength(320))
                        .withConstraint(pattern("^[^@]+@[^@]+$"));

        // Valid email
        spec.validateValue("user@example.com");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMultipleConstraintsFailsOnFirst() {
        ArgumentSpecification<String> spec =
                string("email")
                        .withConstraint(minLength(5))
                        .withConstraint(maxLength(320))
                        .withConstraint(pattern("^[^@]+@[^@]+$"));

        spec.validateValue("a@b"); // Too short
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMultipleConstraintsFailsOnLast() {
        ArgumentSpecification<String> spec =
                string("email")
                        .withConstraint(minLength(5))
                        .withConstraint(maxLength(320))
                        .withConstraint(pattern("^[^@]+@[^@]+$"));

        spec.validateValue("notanemail"); // Doesn't match pattern
    }

    @Test
    public void testNoConstraints() {
        ArgumentSpecification<String> spec = string("name");

        // Should accept any string
        spec.validateValue("a");
        spec.validateValue("");
        spec.validateValue("very long string with no constraints applied");
    }

    @Test
    public void testMinLengthConstraint() {
        ArgumentSpecification<String> spec = string("username").withConstraint(minLength(3));

        spec.validateValue("abc");
        spec.validateValue("abcd");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be at least 3 characters.*")
    public void testMinLengthConstraintWithMessage() {
        ArgumentSpecification<String> spec = string("username").withConstraint(minLength(3));
        spec.validateValue("ab");
    }

    @Test
    public void testMaxLengthConstraint() {
        ArgumentSpecification<String> spec = string("code").withConstraint(maxLength(10));

        spec.validateValue("abc");
        spec.validateValue("1234567890");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be at most 10 characters.*")
    public void testMaxLengthConstraintWithMessage() {
        ArgumentSpecification<String> spec = string("code").withConstraint(maxLength(10));
        spec.validateValue("12345678901");
    }

    @Test
    public void testRangeConstraint() {
        Constraint<Integer> constraint = range(10, 20);

        assertTrue(constraint.test(10));
        assertTrue(constraint.test(15));
        assertTrue(constraint.test(20));
        assertFalse(constraint.test(9));
        assertFalse(constraint.test(21));
    }

    @Test
    public void testRangeConstraintErrorMessage() {
        Constraint<Integer> constraint = range(10, 20);
        String message = constraint.getErrorMessage(25);

        assertTrue(message.contains("10"));
        assertTrue(message.contains("20"));
        assertTrue(message.contains("25"));
    }

    @Test
    public void testCustomConstraintWithLambda() {
        Constraint<String> notEmpty = custom(s -> !s.isEmpty(), "cannot be empty");

        assertTrue(notEmpty.test("value"));
        assertFalse(notEmpty.test(""));
        assertEquals(notEmpty.getErrorMessage(""), "cannot be empty");
    }

    @Test
    public void testNotBlankConstraintValid() {
        Constraint<String> constraint = notBlank();

        assertTrue(constraint.test("value"));
        assertTrue(constraint.test("a"));
        assertTrue(constraint.test("  text  "));
    }

    @Test
    public void testNotBlankConstraintEmpty() {
        Constraint<String> constraint = notBlank();
        assertFalse(constraint.test(""));
        assertTrue(constraint.getErrorMessage("").contains("cannot be empty"));
    }

    @Test
    public void testNotBlankConstraintWhitespace() {
        Constraint<String> constraint = notBlank();
        assertFalse(constraint.test("   "));
        assertFalse(constraint.test("\t"));
        assertFalse(constraint.test("\n"));
        assertTrue(constraint.getErrorMessage("   ").contains("whitespace"));
    }

    @Test
    public void testNotBlankConstraintWithSpec() {
        ArgumentSpecification<String> spec = string("name").withConstraint(notBlank());

        // Valid values
        spec.validateValue("value");
        spec.validateValue("  trimmed  ");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*cannot be empty.*")
    public void testNotBlankConstraintWithSpecFailsEmpty() {
        ArgumentSpecification<String> spec = string("name").withConstraint(notBlank());
        spec.validateValue("");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*whitespace.*")
    public void testNotBlankConstraintWithSpecFailsWhitespace() {
        ArgumentSpecification<String> spec = string("name").withConstraint(notBlank());
        spec.validateValue("   ");
    }
}

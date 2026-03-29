package io.github.totalschema.spi;

import static io.github.totalschema.spi.factory.ArgumentSpecification.*;
import static org.testng.Assert.*;

import io.github.totalschema.spi.factory.ArgumentHandler;
import io.github.totalschema.spi.factory.ArgumentSpecification;
import java.util.List;
import org.testng.annotations.Test;

/** Tests for ArgumentHandler standalone usage. */
public class ArgumentHandlerTest {

    @Test
    public void testBasicUsage() {
        ArgumentSpecification<String> nameSpec = string("name").withConstraint(notBlank());
        ArgumentSpecification<Integer> ageSpec = new ArgumentSpecification<>(Integer.class, "age");

        ArgumentHandler handler = new ArgumentHandler(nameSpec, ageSpec);

        List<Object> args = List.of("John", 25);

        // Validate structure
        handler.validateStructure(args, "TestFactory");

        // Retrieve arguments
        String name = handler.getArgument(nameSpec, args);
        Integer age = handler.getArgument(ageSpec, args);

        assertEquals(name, "John");
        assertEquals(age, Integer.valueOf(25));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidationFailsOnBlank() {
        ArgumentSpecification<String> nameSpec = string("name").withConstraint(notBlank());

        ArgumentHandler handler = new ArgumentHandler(nameSpec);

        List<Object> args = List.of("  "); // Blank string
        handler.validateStructure(args, "TestFactory");
        handler.getArgument(nameSpec, args); // Should fail constraint validation
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testSpecNotInList() {
        ArgumentSpecification<String> declaredSpec = string("declared");
        ArgumentSpecification<String> undeclaredSpec = string("undeclared");

        ArgumentHandler handler = new ArgumentHandler(declaredSpec);

        List<Object> args = List.of("value");
        handler.getArgument(undeclaredSpec, args); // Should fail - spec not in list
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWrongArgumentCount() {
        ArgumentSpecification<String> nameSpec = string("name");
        ArgumentSpecification<Integer> ageSpec = new ArgumentSpecification<>(Integer.class, "age");

        ArgumentHandler handler = new ArgumentHandler(nameSpec, ageSpec);

        List<Object> args = List.of("John"); // Missing age argument
        handler.validateStructure(args, "TestFactory"); // Should fail
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testWrongType() {
        ArgumentSpecification<Integer> ageSpec = new ArgumentSpecification<>(Integer.class, "age");

        ArgumentHandler handler = new ArgumentHandler(ageSpec);

        List<Object> args = List.of("not an integer");
        handler.validateStructure(args, "TestFactory"); // Should fail type check
    }

    @Test
    public void testConstraintValidation() {
        ArgumentSpecification<String> hostSpec = string("host", 1, 255);
        ArgumentSpecification<Integer> portSpec = integer("port", 1, 65535);

        ArgumentHandler handler = new ArgumentHandler(hostSpec, portSpec);

        List<Object> args = List.of("localhost", 8080);
        handler.validateStructure(args, "TestFactory");

        String host = handler.getArgument(hostSpec, args);
        Integer port = handler.getArgument(portSpec, args);

        assertEquals(host, "localhost");
        assertEquals(port, Integer.valueOf(8080));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testConstraintViolation() {
        ArgumentSpecification<Integer> portSpec = integer("port", 1, 65535);

        ArgumentHandler handler = new ArgumentHandler(portSpec);

        List<Object> args = List.of(99999); // Out of range
        handler.validateStructure(args, "TestFactory"); // Passes structural validation
        handler.getArgument(portSpec, args); // Should fail constraint validation
    }

    @Test
    public void testGetSpecifications() {
        ArgumentSpecification<String> nameSpec = string("name");
        ArgumentSpecification<Integer> ageSpec = new ArgumentSpecification<>(Integer.class, "age");

        ArgumentHandler handler = new ArgumentHandler(nameSpec, ageSpec);

        assertEquals(handler.getSpecifications().size(), 2);
        assertEquals(handler.getSpecifications().get(0), nameSpec);
        assertEquals(handler.getSpecifications().get(1), ageSpec);
    }
}

package io.github.totalschema.spi;

import static io.github.totalschema.spi.ArgumentSpecification.*;
import static org.testng.Assert.*;

import io.github.totalschema.engine.api.Context;
import java.util.List;
import org.testng.annotations.Test;

/** Integration tests demonstrating ComponentFactory.getArgument() with constraint validation. */
public class ComponentFactoryArgumentConstraintIntegrationTest {

    /** Mock component for testing. */
    static class Server {
        final String host;
        final int port;
        final String protocol;

        Server(String host, int port, String protocol) {
            this.host = host;
            this.port = port;
            this.protocol = protocol;
        }
    }

    /** Example factory with constrained arguments. */
    static class ServerFactory extends ComponentFactory<Server> {
        private static final ArgumentSpecification<String> HOST_ARG = string("host", 1, 255);
        private static final ArgumentSpecification<Integer> PORT_ARG = integer("port", 1, 65535);
        private static final ArgumentSpecification<String> PROTOCOL_ARG =
                string("protocol").withConstraint(pattern("^(http|https|ftp)$"));

        @Override
        public boolean isLazy() {
            return false;
        }

        @Override
        public Class<Server> getComponentType() {
            return Server.class;
        }

        @Override
        public String getQualifier() {
            return null;
        }

        @Override
        public List<Class<?>> getRequiredContextTypes() {
            return List.of();
        }

        @Override
        public List<ArgumentSpecification<?>> getArgumentSpecifications() {
            return List.of(HOST_ARG, PORT_ARG, PROTOCOL_ARG);
        }

        @Override
        public Server newComponent(Context context, Object... arguments) {
            // Validate structure once
            validateArguments(arguments);

            // Retrieve and validate constraints
            String host = getArgument(HOST_ARG, arguments, 0);
            Integer port = getArgument(PORT_ARG, arguments, 1);
            String protocol = getArgument(PROTOCOL_ARG, arguments, 2);

            return new Server(host, port, protocol);
        }
    }

    @Test
    public void testValidArgumentsWithConstraints() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null; // Not needed for this test

        Server server = factory.newComponent(mockContext, "localhost", 8080, "http");

        assertEquals(server.host, "localhost");
        assertEquals(server.port, 8080);
        assertEquals(server.protocol, "http");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be between 1 and 65535.*")
    public void testPortOutOfRange() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null;

        // Port 99999 is out of valid range
        factory.newComponent(mockContext, "localhost", 99999, "http");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must match pattern.*")
    public void testInvalidProtocol() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null;

        // "ssh" is not in allowed protocols
        factory.newComponent(mockContext, "localhost", 8080, "ssh");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must be at most 255 characters.*")
    public void testHostTooLong() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null;

        String veryLongHost = "a".repeat(256);
        factory.newComponent(mockContext, veryLongHost, 8080, "http");
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*expects exactly 3 argument.*")
    public void testWrongArgumentCount() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null;

        // Only 2 arguments, should fail structural validation
        factory.newComponent(mockContext, "localhost", 8080);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*incorrect type.*")
    public void testWrongArgumentType() {
        ServerFactory factory = new ServerFactory();
        Context mockContext = null;

        // Port should be Integer, not String
        factory.newComponent(mockContext, "localhost", "8080", "http");
    }

    /** Factory demonstrating custom constraint. */
    static class EmailServiceFactory extends ComponentFactory<Object> {
        private static final ArgumentSpecification<String> EMAIL_ARG =
                string("email")
                        .withConstraint(pattern("^[^@]+@[^@]+$"))
                        .withConstraint(maxLength(320));

        private static final ArgumentSpecification<Integer> RETRY_ARG =
                integer("retryCount", 0, 10)
                        .withConstraint(custom(n -> n % 2 == 0, "retry count must be even"));

        @Override
        public boolean isLazy() {
            return false;
        }

        @Override
        public Class<Object> getComponentType() {
            return Object.class;
        }

        @Override
        public String getQualifier() {
            return "email";
        }

        @Override
        public List<Class<?>> getRequiredContextTypes() {
            return List.of();
        }

        @Override
        public List<ArgumentSpecification<?>> getArgumentSpecifications() {
            return List.of(EMAIL_ARG, RETRY_ARG);
        }

        @Override
        public Object newComponent(Context context, Object... arguments) {
            validateArguments(arguments);

            String email = getArgument(EMAIL_ARG, arguments, 0);
            Integer retryCount = getArgument(RETRY_ARG, arguments, 1);

            return new Object() {
                @Override
                public String toString() {
                    return "EmailService[email=" + email + ", retries=" + retryCount + "]";
                }
            };
        }
    }

    @Test
    public void testCustomConstraintValid() {
        EmailServiceFactory factory = new EmailServiceFactory();
        Context mockContext = null;

        Object service = factory.newComponent(mockContext, "user@example.com", 4);
        assertNotNull(service);
        assertTrue(service.toString().contains("user@example.com"));
        assertTrue(service.toString().contains("4"));
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*retry count must be even.*")
    public void testCustomConstraintFails() {
        EmailServiceFactory factory = new EmailServiceFactory();
        Context mockContext = null;

        // Retry count is odd, should fail
        factory.newComponent(mockContext, "user@example.com", 3);
    }

    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*must match pattern.*")
    public void testEmailPatternFails() {
        EmailServiceFactory factory = new EmailServiceFactory();
        Context mockContext = null;

        // No @ symbol
        factory.newComponent(mockContext, "notanemail", 2);
    }

    /** Tests that getArgument validates type even if structural validation passed. */
    @Test(
            expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = ".*incorrect type.*")
    public void testGetArgumentTypeCheck() {
        ComponentFactory<Object> factory =
                new ComponentFactory<Object>() {
                    private final ArgumentSpecification<String> NAME_ARG = string("name");

                    @Override
                    public boolean isLazy() {
                        return false;
                    }

                    @Override
                    public Class<Object> getComponentType() {
                        return Object.class;
                    }

                    @Override
                    public String getQualifier() {
                        return null;
                    }

                    @Override
                    public List<Class<?>> getRequiredContextTypes() {
                        return List.of();
                    }

                    @Override
                    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
                        return List.of(new ArgumentSpecification<>(Object.class, "name"));
                    }

                    @Override
                    public Object newComponent(Context context, Object... arguments) {
                        // Skip validateArguments to test getArgument type checking
                        // This simulates a mismatch between spec and actual argument type
                        String name = getArgument(NAME_ARG, arguments, 0);
                        return name;
                    }
                };

        // Pass an Integer where String is expected in getArgument
        factory.newComponent(null, 123);
    }

    @Test
    public void testNoConstraintsStillWorks() {
        ComponentFactory<String> factory =
                new ComponentFactory<String>() {
                    private final ArgumentSpecification<String> NAME_ARG = string("name");

                    @Override
                    public boolean isLazy() {
                        return false;
                    }

                    @Override
                    public Class<String> getComponentType() {
                        return String.class;
                    }

                    @Override
                    public String getQualifier() {
                        return null;
                    }

                    @Override
                    public List<Class<?>> getRequiredContextTypes() {
                        return List.of();
                    }

                    @Override
                    public List<ArgumentSpecification<?>> getArgumentSpecifications() {
                        return List.of(NAME_ARG);
                    }

                    @Override
                    public String newComponent(Context context, Object... arguments) {
                        validateArguments(arguments);
                        return getArgument(NAME_ARG, arguments, 0);
                    }
                };

        String result = factory.newComponent(null, "test");
        assertEquals(result, "test");
    }
}

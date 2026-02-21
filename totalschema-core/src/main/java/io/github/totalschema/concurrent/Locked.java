package io.github.totalschema.concurrent;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A thread-safe wrapper that ensures an object can only be accessed while holding a lock.
 *
 * <p>This class provides a safe way to perform operations on an object with automatic lock
 * management. The lock is acquired before executing the callback and released afterwards,
 * preventing concurrent access issues.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Locked<List<String>> lockedList = Locked.of(new ArrayList<>(), 5, TimeUnit.SECONDS);
 *
 * // Add an item with automatic locking
 * lockedList.withTryLock(list -> list.add("item"));
 *
 * // Get the size with automatic locking
 * int size = lockedList.withTryLock(list -> list.size());
 * }</pre>
 *
 * @param <C> the type of the object being protected by the lock
 */
public class Locked<C> {

    private final C object;
    private final LockTemplate lockTemplate;

    /**
     * Creates a new Locked instance with the specified object and lock timeout.
     *
     * @param <C> the type of the object to protect
     * @param object the object to protect with a lock (must not be null)
     * @param timeout the maximum time to wait for the lock
     * @param timeUnit the time unit of the timeout argument
     * @return a new Locked instance wrapping the object
     */
    public static <C> Locked<C> of(C object, long timeout, TimeUnit timeUnit) {
        return new Locked<>(object, timeout, timeUnit);
    }

    /**
     * Constructs a Locked instance with a new ReentrantLock and the specified timeout.
     *
     * @param object the object to protect with a lock (must not be null)
     * @param timeout the maximum time to wait for the lock
     * @param timeUnit the time unit of the timeout argument
     */
    public Locked(C object, long timeout, TimeUnit timeUnit) {
        this(object, new LockTemplate(timeout, timeUnit, new ReentrantLock()));
    }

    /**
     * Constructs a Locked instance with a custom LockTemplate.
     *
     * @param object the object to protect with a lock (must not be null)
     * @param lockTemplate the lock template to use for lock management (must not be null)
     * @throws NullPointerException if object or lockTemplate is null
     */
    public Locked(C object, LockTemplate lockTemplate) {
        Objects.requireNonNull(object, "Argument object cannot be null");
        Objects.requireNonNull(lockTemplate, "Argument lockTemplate cannot be null");

        this.object = object;
        this.lockTemplate = lockTemplate;
    }

    /**
     * Executes a function on the protected object while holding the lock.
     *
     * <p>The lock is acquired before the callback is executed and released afterwards. If the lock
     * cannot be acquired within the configured timeout, an exception is thrown.
     *
     * <p><strong>Important:</strong> The callback must not return the locked object itself, as this
     * would allow access to the object without the lock being held.
     *
     * @param <R> the return type of the callback function
     * @param callback the function to execute with the locked object
     * @return the result of the callback function
     * @throws RuntimeException if the callback returns the locked object itself, if the lock cannot
     *     be acquired, or if an unexpected error occurs
     */
    public <R> R withTryLock(Function<C, R> callback) {
        try {
            R returnValue = lockTemplate.withTryLock(() -> callback.apply(object));

            if (returnValue == object) {
                // catches the common mistake of returning the locked object itself,
                // which can lead to confusion and potential issues with lock management
                throw new RuntimeException(
                        "Lock callback returned the locked object itself, which is not allowed");
            }
            return returnValue;

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception during locked execution", e);
        }
    }

    /**
     * Executes a consumer operation on the protected object while holding the lock.
     *
     * <p>The lock is acquired before the callback is executed and released afterwards. If the lock
     * cannot be acquired within the configured timeout, an exception is thrown.
     *
     * <p>This method is useful for operations that modify the object but do not need to return a
     * value.
     *
     * @param callback the consumer operation to execute with the locked object
     * @throws RuntimeException if the lock cannot be acquired or if an unexpected error occurs
     */
    public void withTryLock(Consumer<C> callback) {
        try {
            lockTemplate.withTryLock(() -> callback.accept(object));

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Unexpected exception during locked execution", e);
        }
    }
}

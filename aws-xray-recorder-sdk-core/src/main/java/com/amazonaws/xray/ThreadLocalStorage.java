package com.amazonaws.xray;

import java.security.SecureRandom;

import com.amazonaws.xray.entities.Entity;


public class ThreadLocalStorage {

    static class LocalEntity extends ThreadLocal<Entity> {
        @Override
        protected Entity initialValue() {
            return null;
        }
    }

    static class LocalSecureRandom extends ThreadLocal<SecureRandom> {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    }

    private static final LocalEntity CURRENT_ENTITY = new LocalEntity();
    private static final LocalSecureRandom CURRENT_RANDOM = new LocalSecureRandom();

    public static Entity get() {
        return CURRENT_ENTITY.get();
    }

    public static boolean any() {
        return CURRENT_ENTITY.get() != null;
    }

    public static void set(Entity entity) {
        CURRENT_ENTITY.set(entity);
    }

    /**
     * Clears the current stored entity.
     *
     */
    public static void clear() {
        CURRENT_ENTITY.remove();
    }

    public static SecureRandom getRandom() {
        return CURRENT_RANDOM.get();
    }
}

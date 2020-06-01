/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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

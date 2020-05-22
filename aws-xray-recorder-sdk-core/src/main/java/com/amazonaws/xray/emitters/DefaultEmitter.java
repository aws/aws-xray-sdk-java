package com.amazonaws.xray.emitters;

import java.net.SocketException;

/**
 * @deprecated Use {@link Emitter#create()}.
 */
@Deprecated
public class DefaultEmitter extends UDPEmitter {
    public DefaultEmitter() throws SocketException {
        super();
    }
}

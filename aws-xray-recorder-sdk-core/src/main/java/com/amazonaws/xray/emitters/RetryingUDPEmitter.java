package com.amazonaws.xray.emitters;

import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.strategy.StreamingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.SocketException;
import java.util.function.Function;

public class RetryingUDPEmitter extends UDPEmitter {
    private static final Log logger = LogFactory.getLog(MethodHandles.lookup().lookupClass());
    private StreamingStrategy streamingStrategy;

    /**
     * @see UDPEmitter#UDPEmitter()
     */
    public RetryingUDPEmitter() throws SocketException {
    }

    @Override
    public void init(StreamingStrategy streamingStrategy) {
        super.init(streamingStrategy);
        this.streamingStrategy = streamingStrategy;
    }

    @Override
    protected <T extends Entity> boolean sendData(T entity, Function<T, String> serializer) {
        try {
            return sendPacket(entity, serializer);
        } catch (IOException e) {
            logger.info("send of entity " + entity.getId() + " failed, retrying because of:" + e.getMessage());
            streamingStrategy.streamSome(entity, this);
            return super.sendData(entity, serializer);
        }
    }
}

package org.glassfish.jersey.server;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.CountDownLatch;
import javax.ws.rs.container.ConnectionCallback;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScope.Instance;
import org.glassfish.jersey.server.internal.process.AsyncContext;

/**
 *
 */
public class LatchChunkedOutput<T> extends ChunkedOutput<T> {

    public final CountDownLatch latch = new CountDownLatch(1);

    public LatchChunkedOutput(Type chunkType) {
        super(chunkType);
    }

    @Override
    void setContext(RequestScope requestScope,
        Instance requestScopeInstance,
        ContainerRequest requestContext,
        ContainerResponse responseContext,
        ConnectionCallback connectionCallbackRunner,
        Value<AsyncContext> asyncContext) throws IOException {
        super.setContext(requestScope, requestScopeInstance, requestContext, responseContext, connectionCallbackRunner, asyncContext);
        latch.countDown();
    }
}

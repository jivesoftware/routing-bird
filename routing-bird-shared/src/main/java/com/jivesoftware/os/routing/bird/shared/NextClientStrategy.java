package com.jivesoftware.os.routing.bird.shared;

import com.jivesoftware.os.routing.bird.shared.ReturnFirstNonFailure.Favored;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public interface NextClientStrategy {

    <C, R> R call(String family,
        ClientCall<C, R, HttpClientException> httpCall,
        ConnectionDescriptor[] connectionDescriptors,
        long connectionDescriptorsVersion,
        C[] clients,
        ClientHealth[] clientHealths,
        int deadAfterNErrors,
        long checkDeadEveryNMillis,
        AtomicInteger[] clientsErrors,
        AtomicLong[] clientsDeathTimestamp,
        Favored favored
    ) throws HttpClientException;

}

package com.jivesoftware.os.routing.bird.shared;

/**
 * Created by jonathan.colt on 3/9/17.
 */
public interface ClientHealth {

    void attempt(String family);

    void success(String family, long latencyMillis);

    void markedDead();

    void connectivityError(String family);

    void fatalError(String family, Exception x);

    void stillDead();

    void interrupted(String family, Exception e);
}

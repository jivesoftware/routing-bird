package com.jivesoftware.os.routing.bird.shared;

import java.util.List;

/**
 *
 * @author jonathan.colt
 */
public class InstanceThrown {

    public String key;
    public String message;
    public List<InstanceTrace> stackTrace;
    public long thrown;
    public long timestamp;
    public List<InstanceThrown> causes;

    public InstanceThrown() {
    }

    public InstanceThrown(String key, String message, List<InstanceTrace> stackTrace, long thrown, long timestamp, List<InstanceThrown> causes) {
        this.key = key;
        this.message = message;
        this.stackTrace = stackTrace;
        this.thrown = thrown;
        this.timestamp = timestamp;
        this.causes = causes;
    }

    public static class InstanceTrace {

        public String declaringClass;
        public String methodName;
        public String fileName;
        public int lineNumber;

        public InstanceTrace() {
        }

        public InstanceTrace(String declaringClass, String methodName, String fileName, int lineNumber) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.fileName = fileName;
            this.lineNumber = lineNumber;
        }

    }

}

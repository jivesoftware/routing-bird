package com.jivesoftware.os.routing.bird.server.oauth;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorState;
import org.glassfish.hk2.api.Unqualified;
import org.glassfish.jersey.oauth1.signature.HmaSha1Method;
import org.glassfish.jersey.oauth1.signature.OAuth1SignatureMethod;
import org.glassfish.jersey.oauth1.signature.PlaintextMethod;

/**
 *
 * @author jonathan.colt
 */
public class OAuthServiceLocatorShim implements ServiceLocator {

    @Override
    public <T> T getService(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T getService(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T getService(Class<T> contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T getService(Type contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> List<T> getAllServices(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
        if (contractOrImpl.equals(OAuth1SignatureMethod.class)) {
            return (List<T>) Arrays.asList(
                new OAuth1SignatureMethod[]{
                    new HmaSha1Method(),
                    new SimpleRsaSha1Method(),
                    new PlaintextMethod()
            });
        }
        return null;
    }

    @Override
    public <T> List<T> getAllServices(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> List<T> getAllServices(Annotation qualifier, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public List<?> getAllServices(Filter searchCriteria) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Class<T> contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(Type contractOrImpl, String name, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> List<ServiceHandle<T>> getAllServiceHandles(Class<T> contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Type contractOrImpl, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Annotation qualifier, Annotation... qualifiers) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public List<ServiceHandle<?>> getAllServiceHandles(Filter searchCriteria) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public List<ActiveDescriptor<?>> getDescriptors(Filter filter) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ActiveDescriptor<?> getBestDescriptor(Filter filter) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor, Injectee injectee) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ActiveDescriptor<?> reifyDescriptor(Descriptor descriptor) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ActiveDescriptor<?> getInjecteeDescriptor(Injectee injectee) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor, Injectee injectee) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> ServiceHandle<T> getServiceHandle(ActiveDescriptor<T> activeDescriptor) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T getService(ActiveDescriptor<T> activeDescriptor,
        ServiceHandle<?> root) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T getService(ActiveDescriptor<T> activeDescriptor,
        ServiceHandle<?> root, Injectee injectee) throws MultiException {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public String getDefaultClassAnalyzerName() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void setDefaultClassAnalyzerName(String defaultClassAnalyzer) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public Unqualified getDefaultUnqualified() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void setDefaultUnqualified(Unqualified unqualified) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public long getLocatorId() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ServiceLocator getParent() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void shutdown() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public ServiceLocatorState getState() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public boolean getNeutralContextClassLoader() {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void setNeutralContextClassLoader(boolean neutralContextClassLoader) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T create(Class<T> createMe) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <T> T create(Class<T> createMe, String strategy) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void inject(Object injectMe) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void inject(Object injectMe, String strategy) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void postConstruct(Object postConstructMe) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void postConstruct(Object postConstructMe, String strategy) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void preDestroy(Object preDestroyMe) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public void preDestroy(Object preDestroyMe, String strategy) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <U> U createAndInitialize(Class<U> createMe) {
        throw new UnsupportedOperationException("Will not be supported!");
    }

    @Override
    public <U> U createAndInitialize(Class<U> createMe, String strategy) {
        throw new UnsupportedOperationException("Will not be supported!");
    }
}

package org.merlin.config;

/**
 *
 * @author jonathan.colt
 */
public interface ConfigProvider {

    <T extends Config> T config(Class<T> clazz);

}

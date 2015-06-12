/*
 * Copyright 2013 Jive Software, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.jivesoftware.os.routing.bird.deployable.config.extractor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.merlin.config.BindInterfaceToConfiguration;
import org.merlin.config.Config;
import org.merlin.config.MapBackConfiguration;

public class ConfigBinder {

    private final Properties properties;
    private final PropertyPrefix propertyPrefix;

    public ConfigBinder(String[] args) throws IOException {
        this.propertyPrefix = new PropertyPrefix();
        this.properties = new Properties();
        for (String arg : args) {
            File f = new File(arg);
            if (f.exists()) {
                this.properties.load(new FileInputStream(f));
            }
        }
    }

    public ConfigBinder(Properties properties) {
        this.propertyPrefix = new PropertyPrefix();
        this.properties = properties;
    }

    public String get(String propertyKey) {
        return properties.getProperty(propertyKey);
    }

    public <T extends Config> T bind(Class<T> configInterface) {
        return bind(configInterface, new HashMap<String, String>());
    }

    public <T extends Config> T bind(Class<T> configInterface, Map<String, String> backingStorage) {

        Map<String, String> required = new HashMap<>();
        Config config = new BindInterfaceToConfiguration<>(new MapBackConfiguration(required), configInterface)
                .bind();
        config.applyDefaults();

        final String prefix = propertyPrefix.propertyPrefix(configInterface);
        Map<String, String> available = new HashMap<>();

        for (Entry<Object, Object> e : properties.entrySet()) {
            if (e.getKey().toString().startsWith(prefix)) {
                available.put(e.getKey().toString().substring(prefix.length()), e.getValue().toString());
            }
        }
        Set<String> requiredKeys = required.keySet();
        requiredKeys.removeAll(available.keySet());
        if (!requiredKeys.isEmpty()) {
            System.err.println("WARNING: The provided properties lacks the following properties: " + requiredKeys);
            System.err.println("WARNING: Populated config for " + configInterface + " with defaults.");
            T t = BindInterfaceToConfiguration.bindDefault(configInterface);
            return t;
        } else {

            T t = new BindInterfaceToConfiguration<>(new MapBackConfiguration(available), configInterface)
                    .bind();
            return t;
        }
    }
}

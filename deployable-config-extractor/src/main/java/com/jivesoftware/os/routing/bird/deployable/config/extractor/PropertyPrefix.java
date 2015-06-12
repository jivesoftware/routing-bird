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

import org.merlin.config.Config;

public class PropertyPrefix {

    public String propertyPrefix(Class c) {
        String name = c.getSimpleName();
        String instance = "default";
        Class[] implemented = c.getInterfaces();
        if (implemented != null && implemented.length == 1 && implemented[0] != Config.class) {
            name = implemented[0].getSimpleName();
            instance = c.getSimpleName();
        }

        return name + "_" + instance + "_";
    }
}

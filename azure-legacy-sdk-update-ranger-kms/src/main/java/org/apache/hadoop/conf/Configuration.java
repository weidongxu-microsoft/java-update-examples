/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.conf;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple stub for Hadoop Configuration to make the code compile
 */
public class Configuration {
    private Map<String, String> properties = new HashMap<>();

    public String get(String key) {
        return properties.get(key);
    }

    public void set(String key, String value) {
        properties.put(key, value);
    }

    public String get(String key, String defaultValue) {
        String value = properties.get(key);
        return value != null ? value : defaultValue;
    }
}

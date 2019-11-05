/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package io.helidon.examples.openapisnakeyaml;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class TypeDescriptionWithEnums extends TypeDescription {

    private final Map<String, Function<String, Enum<?>> > enumEvaluators = new HashMap<>();

    static TypeDescriptionWithEnums newInstance(Class<? extends Object> clazz, Class<?> impl) {
        TypeDescriptionWithEnums result = new TypeDescriptionWithEnums(clazz, impl);
        return result;
    }

    TypeDescriptionWithEnums(Class<? extends Object> clazz, Class<?> impl) {
        super(clazz, null, impl);
    }

    <E extends Enum<E>> TypeDescriptionWithEnums addEnum(String propertyName, Function<String,
            Enum<?>> fn) {
        enumEvaluators.put(propertyName, fn);
        return this;
    }

    @Override
    public Object newInstance(String propertyName, Node node) {
        if (enumEvaluators.containsKey(propertyName)) {
            return enumEvaluators.get(propertyName).apply(((ScalarNode) node).getValue().toUpperCase());
        }
        return super.newInstance(propertyName, node);
    }
}

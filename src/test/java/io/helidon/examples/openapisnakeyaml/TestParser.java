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

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TestParser {

    @Test
    public void testParser() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/openapi-greeting.yml")) {
            OpenAPI openAPI = Parser.parse(is);
            // Following fails: class java.util.LinkedHashMap cannot be cast to class org.eclipse.microprofile.openapi.models.PathItem
            System.err.println("getPathItems should be returns " + openAPI.getPaths().getPathItems().getClass().getName());
            System.err.flush();
            openAPI.getPaths().getPathItems().forEach((operationName, pathItem) -> {
                Map<PathItem.HttpMethod, Operation> operations = pathItem.getOperations();
            });
        }
    }
}

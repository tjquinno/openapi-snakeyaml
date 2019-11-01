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

import java.io.InputStream;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.common.CollectionsHelper;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.ExternalDocumentationImpl;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.callbacks.CallbackImpl;
import io.smallrye.openapi.api.models.examples.ExampleImpl;
import io.smallrye.openapi.api.models.headers.HeaderImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.api.models.links.LinkImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.parameters.RequestBodyImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.security.SecurityRequirementImpl;
import io.smallrye.openapi.api.models.security.SecuritySchemeImpl;
import io.smallrye.openapi.api.models.servers.ServerImpl;
import io.smallrye.openapi.api.models.servers.ServerVariableImpl;
import io.smallrye.openapi.api.models.tags.TagImpl;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.Paths;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Construct;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;


class Parser {

    static OpenAPI parse(InputStream is) {
        return parseYAML(is);
    }

    private static OpenAPI parseYAML(InputStream inputStream) {

        TypeDescription openAPITD = new TypeDescription(OpenAPI.class, OpenAPIImpl.class);
        openAPITD.putListPropertyType("security", SecurityRequirement.class);
        openAPITD.putListPropertyType("servers", Server.class);
        openAPITD.putListPropertyType("tags", Tag.class);


        // This use of a custom constructor is part of a separate experiment.
//        Constructor topConstructor = new CustomConstructor(openAPITD);
        Constructor topConstructor = new Constructor(openAPITD);

        topConstructor.addTypeDescription(new TypeDescription(Info.class, InfoImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(ExternalDocumentation.class, ExternalDocumentationImpl.class));

        TypeDescription serverTD = new TypeDescription(Server.class, ServerImpl.class);
        serverTD.putMapPropertyType("variables", String.class, ServerVariable.class);
        topConstructor.addTypeDescription(serverTD);

        TypeDescription serverVariableTD = new TypeDescription(ServerVariable.class, ServerVariableImpl.class);
        serverVariableTD.putListPropertyType("enumeration", String.class);
        topConstructor.addTypeDescription(serverVariableTD);

        TypeDescription securityRequirementTD = new TypeDescription(SecurityRequirement.class, SecurityRequirementImpl.class);
        securityRequirementTD.putListPropertyType("scheme", String.class);
        securityRequirementTD.putMapPropertyType("schemes", String.class, String.class);
        topConstructor.addTypeDescription(securityRequirementTD);

        topConstructor.addTypeDescription(new TypeDescription(Tag.class, TagImpl.class));

        TypeDescription pathsTD = new TypeDescription(Paths.class, PathsImpl.class);
        pathsTD.putMapPropertyType("pathItems", String.class, CustomPathItem.class);
        topConstructor.addTypeDescription(pathsTD);

        /*
         * We cannot use the following commented code because PathItem declares only getOperations(),
         * not setOperations(Map). So instead, let's try using a custom interface and implementation
         * that do support the setOperations(Map) method.
         */
//        TypeDescription pathItemTD = new TypeDescription(PathItem.class, PathItemImpl.class);
//        pathItemTD.putMapPropertyType("operations", PathItem.HttpMethod.class, Operation.class);
//        for (PathItem.HttpMethod m : PathItem.HttpMethod.values()) {
//            pathItemTD.substituteProperty(m.name().toLowerCase(), Operation.class, getter(m), setter(m));
//        }
        TypeDescription pathItemTD = new TypeDescription(CustomPathItem.class, CustomPathItemImpl.class);
        pathItemTD.putMapPropertyType("operations", PathItem.HttpMethod.class, Operation.class);
        pathItemTD.putListPropertyType("servers", Server.class);
        pathItemTD.putListPropertyType("parameters", Parameter.class);
        topConstructor.addTypeDescription(pathItemTD);

        TypeDescription operationTD = new TypeDescription(Operation.class, OperationImpl.class);
        operationTD.putMapPropertyType("callbacks", String.class, Callback.class);
        operationTD.putListPropertyType("security", SecurityRequirement.class);
        operationTD.putListPropertyType("servers", Server.class);
        operationTD.putListPropertyType("tags", String.class);
        topConstructor.addTypeDescription(operationTD);

        TypeDescription componentsTD = new TypeDescription(Components.class, ComponentsImpl.class);
        componentsTD.putMapPropertyType("schemas", String.class, Schema.class);
        componentsTD.putMapPropertyType("responses", String.class, APIResponse.class);
        componentsTD.putMapPropertyType("parameters", String.class, Parameter.class);
        componentsTD.putMapPropertyType("examples", String.class, Example.class);
        componentsTD.putMapPropertyType("requestBodies", String.class, RequestBody.class);
        componentsTD.putMapPropertyType("headers", String.class, Header.class);
        componentsTD.putMapPropertyType("securitySchemes", String.class, SecurityScheme.class);
        componentsTD.putMapPropertyType("links", String.class, Link.class);
        componentsTD.putMapPropertyType("callbacks", String.class, Callback.class);
        topConstructor.addTypeDescription(componentsTD);

        TypeDescription schemaTD = new TypeDescription(Schema.class, SchemaImpl.class);
        schemaTD.putMapPropertyType("properties", String.class, Schema.class);
        schemaTD.putListPropertyType("required", String.class);
        topConstructor.addTypeDescription(schemaTD);

        TypeDescription apiResponseTD = new TypeDescription(APIResponse.class, APIResponseImpl.class);
        apiResponseTD.putMapPropertyType("headers", String.class, Header.class);
        apiResponseTD.putMapPropertyType("links", String.class, Link.class);
        topConstructor.addTypeDescription(apiResponseTD);

        TypeDescription parameterTD = new TypeDescription(Parameter.class, ParameterImpl.class);
        parameterTD.putMapPropertyType("examples", String.class, Example.class);
        topConstructor.addTypeDescription(parameterTD);

        topConstructor.addTypeDescription(new TypeDescription(Example.class, ExampleImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(RequestBody.class, RequestBodyImpl.class));

        TypeDescription headerTD = new TypeDescription(Header.class, HeaderImpl.class);
        headerTD.putMapPropertyType("examples", String.class, Example.class);
        topConstructor.addTypeDescription(headerTD);

        topConstructor.addTypeDescription(new TypeDescription(SecurityScheme.class, SecuritySchemeImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(Link.class, LinkImpl.class));

        TypeDescription callbackTD = new TypeDescription(Callback.class, CallbackImpl.class);
        callbackTD.putMapPropertyType("pathItems", String.class, PathItem.class);
        topConstructor.addTypeDescription(callbackTD);

        Yaml yaml = new Yaml(topConstructor);
        OpenAPI result = OpenAPI.class.cast(yaml.loadAs(inputStream, OpenAPI.class));
        return result;
    }

    private static String getter(PathItem.HttpMethod method) {
        return methodName("get", method);
    }

    private static String setter(PathItem.HttpMethod method) {
        return methodName("set", method);
    }

    private static String methodName(String operation, PathItem.HttpMethod method) {
        return operation + method.name();
    }



    static interface CustomPathItem extends PathItem {
        void setOperations(Map<PathItem.HttpMethod, Operation> operations);
    }

    static class CustomPathItemImpl extends PathItemImpl implements CustomPathItem {

        private final EnumMap<HttpMethod, Consumer<Operation>> methodToSets =
                new EnumMap<HttpMethod, Consumer<Operation>>(HttpMethod.class);

        {
            methodToSets.put(HttpMethod.DELETE, this::setDELETE);
            methodToSets.put(HttpMethod.GET, this::setGET);
            methodToSets.put(HttpMethod.HEAD, this::setHEAD);
            methodToSets.put(HttpMethod.OPTIONS, this::setOPTIONS);
            methodToSets.put(HttpMethod.PATCH, this::setPATCH);
            methodToSets.put(HttpMethod.POST, this::setPOST);
            methodToSets.put(HttpMethod.PUT, this::setPUT);
            methodToSets.put(HttpMethod.TRACE, this::setTRACE);
        }

        public void setOperations(Map<PathItem.HttpMethod, Operation> operations) {
            operations.forEach((method, op) -> methodToSets.get(method).accept(op));
        }
    }
}

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
import java.util.HashMap;
import java.util.Map;

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
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.EncodingImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.parameters.ParameterImpl;
import io.smallrye.openapi.api.models.parameters.RequestBodyImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;
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
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.Scopes;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.servers.ServerVariables;
import org.eclipse.microprofile.openapi.models.tags.Tag;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

class Parser {

    static OpenAPI parse(InputStream is) {
        return parseYAML(is);
    }

    private static OpenAPI parseYAML(InputStream inputStream) {

        TypeDescription openAPITD = new TypeDescription(OpenAPI.class, OpenAPIImpl.class);
        openAPITD.addPropertyParameters("security", SecurityRequirement.class);
        openAPITD.addPropertyParameters("servers", Server.class);
        openAPITD.addPropertyParameters("tags", Tag.class);

        Constructor topConstructor = new CustomConstructor(openAPITD);

        topConstructor.addTypeDescription(new TypeDescription(Info.class, InfoImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(ExternalDocumentation.class, ExternalDocumentationImpl.class));

        TypeDescription serverTD = new TypeDescription(Server.class, ServerImpl.class);
        serverTD.addPropertyParameters("variables", String.class, ServerVariable.class);
        topConstructor.addTypeDescription(serverTD);

        TypeDescription serverVariableTD = new TypeDescription(ServerVariable.class, ServerVariableImpl.class);
        serverVariableTD.addPropertyParameters("enumeration", String.class);
        topConstructor.addTypeDescription(serverVariableTD);

        TypeDescription securityRequirementTD = new TypeDescription(SecurityRequirement.class, SecurityRequirementImpl.class);
        securityRequirementTD.addPropertyParameters("scheme", String.class);
        securityRequirementTD.addPropertyParameters("schemes", String.class, String.class);
        topConstructor.addTypeDescription(securityRequirementTD);

        topConstructor.addTypeDescription(new TypeDescription(Tag.class, TagImpl.class));

        TypeDescription pathsTD = new TypeDescription(Paths.class, PathsImpl.class);
        pathsTD.addPropertyParameters("pathItems", String.class, PathItem.class);
        topConstructor.addTypeDescription(pathsTD);

        TypeDescription pathItemTD = new TypeDescription(PathItem.class, PathItemImpl.class);
        // The Operation method names have upper-case HTTP method names (e.g., getPUT) but the
        // yaml property names are lower-case (e.g., 'put').
        for (PathItem.HttpMethod m : PathItem.HttpMethod.values()) {
            pathItemTD.substituteProperty(m.name().toLowerCase(), Operation.class, getter(m), setter(m));
        }
        pathItemTD.addPropertyParameters("servers", Server.class);
        pathItemTD.addPropertyParameters("parameters", Parameter.class);
        topConstructor.addTypeDescription(pathItemTD);

        TypeDescription operationTD = new TypeDescription(Operation.class, OperationImpl.class);
        operationTD.addPropertyParameters("callbacks", String.class, Callback.class);
        operationTD.addPropertyParameters("security", SecurityRequirement.class);
        operationTD.addPropertyParameters("servers", Server.class);
        operationTD.addPropertyParameters("tags", String.class);
        topConstructor.addTypeDescription(operationTD);

        topConstructor.addTypeDescription(new TypeDescription(APIResponses.class, APIResponsesImpl.class));

        TypeDescription componentsTD = new TypeDescription(Components.class, ComponentsImpl.class);
        componentsTD.addPropertyParameters("schemas", String.class, Schema.class);
        componentsTD.addPropertyParameters("responses", String.class, APIResponse.class);
        componentsTD.addPropertyParameters("parameters", String.class, Parameter.class);
        componentsTD.addPropertyParameters("examples", String.class, Example.class);
        componentsTD.addPropertyParameters("requestBodies", String.class, RequestBody.class);
        componentsTD.addPropertyParameters("headers", String.class, Header.class);
        componentsTD.addPropertyParameters("securitySchemes", String.class, SecurityScheme.class);
        componentsTD.addPropertyParameters("links", String.class, Link.class);
        componentsTD.addPropertyParameters("callbacks", String.class, Callback.class);
        topConstructor.addTypeDescription(componentsTD);

        TypeDescription schemaTD = new TypeDescription(Schema.class, SchemaImpl.class) {
            @Override
            public Object newInstance(String propertyName, Node node) {
                if (propertyName.equals("type")) {
                    return Schema.SchemaType.valueOf(((ScalarNode) node).getValue().toUpperCase());
                }
                return super.newInstance(propertyName, node);
            }
        };
        schemaTD.addPropertyParameters("properties", String.class, Schema.class);
        schemaTD.addPropertyParameters("required", String.class);
        schemaTD.addPropertyParameters("allOf", Schema.class);
        schemaTD.addPropertyParameters("anyOf", Schema.class);
        schemaTD.addPropertyParameters("oneOf", Schema.class);
        topConstructor.addTypeDescription(schemaTD);

        TypeDescription apiResponseTD = new TypeDescription(APIResponse.class, APIResponseImpl.class);
        apiResponseTD.addPropertyParameters("headers", String.class, Header.class);
        apiResponseTD.addPropertyParameters("links", String.class, Link.class);
        topConstructor.addTypeDescription(apiResponseTD);

        TypeDescription parameterTD = new TypeDescription(Parameter.class, ParameterImpl.class) {
            @Override
            public Object newInstance(String propertyName, Node node) {
                if (propertyName.equals("in")) {
                    return Parameter.In.valueOf(((ScalarNode) node).getValue().toUpperCase());
                }
                return super.newInstance(propertyName, node);
            }
        };
        parameterTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(parameterTD);

        topConstructor.addTypeDescription(new TypeDescription(Example.class, ExampleImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(RequestBody.class, RequestBodyImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(Content.class, ContentImpl.class));

        TypeDescription mediaTypeTD = new TypeDescription(MediaType.class, MediaTypeImpl.class);
        mediaTypeTD.addPropertyParameters("encoding", String.class, Encoding.class);
        mediaTypeTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(mediaTypeTD);

        TypeDescription encodingTD = new TypeDescription(Encoding.class, EncodingImpl.class);
        encodingTD.addPropertyParameters("headers", String.class, Header.class);
        topConstructor.addTypeDescription(encodingTD);

        TypeDescription headerTD = new TypeDescription(Header.class, HeaderImpl.class);
        headerTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(headerTD);

        topConstructor.addTypeDescription(new TypeDescription(SecurityScheme.class,
                SecuritySchemeImpl.class) {
            @Override
            public Object newInstance(String propertyName, Node node) {
                if (propertyName.equals("in")) {
                    return SecurityScheme.In.valueOf(((ScalarNode) node).getValue().toUpperCase());
                }
                return super.newInstance(propertyName, node);
            }
        });
        topConstructor.addTypeDescription(new TypeDescription(Link.class, LinkImpl.class));

        TypeDescription callbackTD = new TypeDescription(Callback.class, CallbackImpl.class);
        callbackTD.addPropertyParameters("pathItems", String.class, PathItem.class);
        topConstructor.addTypeDescription(callbackTD);

        Yaml yaml = new Yaml(topConstructor);
        OpenAPI result = yaml.loadAs(inputStream, OpenAPI.class);
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

    private static class CustomConstructor extends Constructor {

        private static final Map<Class<?>, Class<?>> childTypes = new HashMap<>();

        static {
            childTypes.put(Paths.class, PathItem.class);
            childTypes.put(Callback.class, PathItem.class);
            childTypes.put(Content.class, MediaType.class);
            childTypes.put(APIResponses.class, APIResponse.class);
            childTypes.put(ServerVariables.class, ServerVariable.class);
            childTypes.put(Scopes.class, String.class);
            childTypes.put(SecurityRequirement.class, String.class);
        }

        CustomConstructor(TypeDescription td) {
            super(td);
        }

        @Override
        protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
            Class<?> parentType = node.getType();
            if (childTypes.containsKey(parentType)) {
                Class<?> childType = childTypes.get(parentType);
                node.getValue().forEach(tuple -> {
                    Node valueNode = tuple.getValueNode();
                    if (valueNode.getType() == Object.class) {
                        valueNode.setType(childType);
                    }
                });
            }
            super.constructMapping2ndStep(node, mapping);
        }
    }
}

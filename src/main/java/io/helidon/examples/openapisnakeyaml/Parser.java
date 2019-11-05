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
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import io.smallrye.openapi.api.models.info.LicenseImpl;
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
import io.smallrye.openapi.api.models.servers.ServerVariablesImpl;
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
import org.eclipse.microprofile.openapi.models.info.License;
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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;
import org.yaml.snakeyaml.representer.Representer;

class Parser {

    static OpenAPI parse(InputStream is) {
        return parseYAML(is);
    }

    static void toYAML(OpenAPI openAPI, Writer writer) {
        DumperOptions opts = new DumperOptions();
        opts.setIndent(2);
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(new CustomRepresenter(), opts);
        yaml.dump(openAPI, new TagSuppressingWriter(writer));
    }

    private static OpenAPI parseYAML(InputStream inputStream) {

        TypeDescription openAPITD = new TypeDescription(OpenAPI.class, OpenAPIImpl.class);
        openAPITD.addPropertyParameters("security", SecurityRequirement.class);
        openAPITD.addPropertyParameters("servers", Server.class);
        openAPITD.addPropertyParameters("tags", Tag.class);

        Constructor topConstructor = new CustomConstructor(openAPITD);

        topConstructor.addTypeDescription(new TypeDescription(Info.class, InfoImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(License.class, LicenseImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(Content.class, ContentImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(ExternalDocumentation.class, ExternalDocumentationImpl.class));

        TypeDescription serverTD = new TypeDescription(Server.class, ServerImpl.class);
        topConstructor.addTypeDescription(serverTD);

        topConstructor.addTypeDescription(new TypeDescription(ServerVariables.class, ServerVariablesImpl.class));

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

        TypeDescription schemaTD = TypeDescriptionWithEnums.newInstance(Schema.class, SchemaImpl.class)
                .addEnum("type", Schema.SchemaType::valueOf);
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

        TypeDescription parameterTD = TypeDescriptionWithEnums.newInstance(Parameter.class, ParameterImpl.class)
            .addEnum("in", Parameter.In::valueOf);
        parameterTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(parameterTD);

        topConstructor.addTypeDescription(new TypeDescription(Example.class, ExampleImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(RequestBody.class, RequestBodyImpl.class));
        topConstructor.addTypeDescription(new TypeDescription(Content.class, ContentImpl.class));

        TypeDescription mediaTypeTD = new TypeDescription(MediaType.class, MediaTypeImpl.class);
        mediaTypeTD.addPropertyParameters("encoding", String.class, Encoding.class);
        mediaTypeTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(mediaTypeTD);

        TypeDescription encodingTD = TypeDescriptionWithEnums.newInstance(Encoding.class, EncodingImpl.class)
                .addEnum("style", Encoding.Style::valueOf);
        encodingTD.addPropertyParameters("headers", String.class, Header.class);
        topConstructor.addTypeDescription(encodingTD);

        TypeDescription headerTD = TypeDescriptionWithEnums.newInstance(Header.class,
                HeaderImpl.class)
                    .addEnum("in", Parameter.In::valueOf)
                    .addEnum("style", Parameter.Style::valueOf);
        headerTD.addPropertyParameters("examples", String.class, Example.class);
        topConstructor.addTypeDescription(headerTD);

        topConstructor.addTypeDescription(TypeDescriptionWithEnums.newInstance(SecurityScheme.class, SecuritySchemeImpl.class)
                .addEnum("in", SecurityScheme.In::valueOf)
                .addEnum("type", SecurityScheme.Type::valueOf));
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

        private static final Map<Class<?>, Class<?>> childMapTypes = new HashMap<>();
        private static final Map<Class<?>, Class<?>> childMapOfListTypes = new HashMap<>();

        static {
            childMapTypes.put(Paths.class, PathItem.class);
            childMapTypes.put(Callback.class, PathItem.class);
            childMapTypes.put(Content.class, MediaType.class);
            childMapTypes.put(APIResponses.class, APIResponse.class);
            childMapTypes.put(ServerVariables.class, ServerVariable.class);
            childMapTypes.put(Scopes.class, String.class);
            childMapOfListTypes.put(SecurityRequirement.class, String.class);
        }

        CustomConstructor(TypeDescription td) {
            super(td);
        }

        @Override
        protected void constructMapping2ndStep(MappingNode node, Map<Object, Object> mapping) {
            Class<?> parentType = node.getType();
            if (childMapTypes.containsKey(parentType)) {
                Class<?> childType = childMapTypes.get(parentType);
                node.getValue().forEach(tuple -> {
                    Node valueNode = tuple.getValueNode();
                    if (valueNode.getType() == Object.class) {
                        valueNode.setType(childType);
                    }
                });
            } else if (childMapOfListTypes.containsKey(parentType)) {
                Class<?> childType = childMapOfListTypes.get(parentType);
                node.getValue().forEach(tuple -> {
                    Node valueNode = tuple.getValueNode();
                    if (valueNode.getNodeId() == NodeId.sequence) {
                        SequenceNode seqNode = (SequenceNode) valueNode;
                        seqNode.setListType(childType);
                    }
                });
            }
            super.constructMapping2ndStep(node, mapping);
        }
    }

    private static class CustomRepresenter extends Representer {

        private static final Map<Class<?>, Set<String>> childEnumNames = new HashMap<>();
        private static final Map<Class<?>, Map<String, Set<String>>> childEnumValues =
                new HashMap<>();

        static {
            childEnumNames.put(PathItemImpl.class, toEnumNames(PathItem.HttpMethod.class));
            childEnumValues.put(SchemaImpl.class,
                    CollectionsHelper.mapOf("type", toEnumNames(Schema.SchemaType.class)));
            childEnumValues.put(ParameterImpl.class,
                    CollectionsHelper.mapOf("style", toEnumNames(Parameter.Style.class),
                            "in", toEnumNames(Parameter.In.class)));
        }

        private static <E extends Enum<E>> Set<String> toEnumNames(Class<E> enumType) {
            Set<String> result = new HashSet<>();
            for (Enum<E> e : enumType.getEnumConstants()) {
                result.add(e.name());
            }
            return result;
        }

        @Override
        protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue,
                org.yaml.snakeyaml.nodes.Tag customTag) {
            if (propertyValue == null) {
                return null;
            }
            NodeTuple result = super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);

            if (childEnumNames.getOrDefault(javaBean.getClass(), Collections.emptySet()).contains(property.getName())) {
                result = new NodeTuple(adjustNode(result.getKeyNode()), result.getValueNode());
            }
            if (propertyValue instanceof Enum && childEnumValues.getOrDefault(javaBean.getClass(),
                    Collections.emptyMap())
                    .getOrDefault(property.getName(), Collections.emptySet())
                    .contains(((Enum) propertyValue).name())) {
                result = new NodeTuple(result.getKeyNode(), adjustNode(result.getValueNode()));
            }
            return result;
        }

        private static Node adjustNode(Node n) {
            Node result = n;
            if (n instanceof ScalarNode) {
                ScalarNode orig = (ScalarNode) n;
                result = new ScalarNode(orig.getTag(), orig.getValue()
                        .toLowerCase(),
                        orig.getStartMark(), orig.getEndMark(), orig.getScalarStyle());
            }
            return result;
        }
    }

    /**
     * Suppress the tag output so the resulting document can be read into any MP OpenAPI
     * implementation, not just SmallRye's.
     */
    static class TagSuppressingWriter extends PrintWriter {

        private static final Pattern UNQUOTED_TRAILING_TAG_PATTERN = Pattern.compile("\\![^\"]+$");

        TagSuppressingWriter(Writer out) {
            super(out);
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            int effLen = detag(CharBuffer.wrap(cbuf), off, len);
            if (effLen > 0) {
                super.write(cbuf, off, effLen);
            }
        }

        @Override
        public void write(String s, int off, int len) {
            int effLen = detag(s, off, len);
            if (effLen > 0) {
                super.write(s, off, effLen);
            }
        }

        private int detag(CharSequence cs, int off, int len) {
            int result = len;
            Matcher m = UNQUOTED_TRAILING_TAG_PATTERN.matcher(cs.subSequence(off, off + len));
            if (m.matches()) {
                result = len - (m.end() - m.start());
            }

            return result;
        }
    }
}

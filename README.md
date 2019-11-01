# openapi-snakeyaml

Simple example loading a MicroProfile OpenAPI 1.1 yaml file using SnakeYAML 1.24.

The MP OpenAPI `Paths` and `APIResponses` interfaces extend `Map` which seem to cause SnakeYAML to incorrectly instantiate
the values in the maps.

Note that the `Parser` class declares a custom constructor but as written the code does not currently use it.

To run the test:

```mvn test```

Issues:

1. In MP OpenAPI 1.1, `OpenAPI.getPaths()` returns a single `Paths` instance which happens to implement `Map`. SnakeYAML
 seems to correctly instantiate `PathImpl` (see the `Parser` class in this repo). But SnakeYAML instantiates the `pathItems` property of `Paths` as a 
`LinkedHashMap<String, LinkedHashMap>` instead of the expected `LinkedHashMap<String, PathItem>`. 
<br><br>
SnakeYAML seems to have skipped converting 
the lower-level yaml into `PathItem` objects to populate the map, although the `yaml.loadAs` in `Parser` returns as if successful. This leads to 
the following error when trying to access the values in the map that was built for the `pathItems` property:<br><br>
```
java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class org.eclipse.microprofile.openapi.models.PathItem (java.util.LinkedHashMap is in module java.base of loader 'bootstrap'; org.eclipse.microprofile.openapi.models.PathItem is in unnamed module of loader 'app')
	at java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:684)
	at java.base/java.util.Collections$UnmodifiableMap.forEach(Collections.java:1503)
	at io.helidon.examples.openapisnakeyaml.TestParser.testParser(TestParser.java:37)
```

2. Adding<br><br>```openAPITD.putMapPropertyType("paths", String.class, PathItem.class);```
<br><br>to try to work around the previous issue results in an exception inside `yaml.loadAs`. 
(The initial reference to line 16, column 1 for `openapi` seems irrelevant): <br><br>
```
Cannot create property=paths for JavaBean=io.smallrye.openapi.api.models.OpenAPIImpl@5276e6b0
 in 'reader', line 16, column 1:
    openapi: 3.0.0
    ^
java.lang.InstantiationException
 in 'reader', line 28, column 3:
      /greet/greeting:
      ^
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.constructJavaBean2ndStep(Constructor.java:268)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.construct(Constructor.java:149)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructYamlObject.construct(Constructor.java:309)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObjectNoCheck(BaseConstructor.java:215)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObject(BaseConstructor.java:205)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructDocument(BaseConstructor.java:164)
	at org.yaml.snakeyaml.constructor.BaseConstructor.getSingleData(BaseConstructor.java:148)
	at org.yaml.snakeyaml.Yaml.loadFromReader(Yaml.java:525)
	at org.yaml.snakeyaml.Yaml.loadAs(Yaml.java:519)
	at io.helidon.examples.openapisnakeyaml.Parser.parseYAML(Parser.java:178)
	at io.helidon.examples.openapisnakeyaml.Parser.parse(Parser.java:78)
	at io.helidon.examples.openapisnakeyaml.TestParser.testParser(TestParser.java:33)
Caused by: org.yaml.snakeyaml.error.YAMLException: java.lang.InstantiationException
	at org.yaml.snakeyaml.constructor.BaseConstructor.newInstance(BaseConstructor.java:289)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.construct(Constructor.java:145)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObjectNoCheck(BaseConstructor.java:215)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObject(BaseConstructor.java:205)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructMapping2ndStep(BaseConstructor.java:465)
	at org.yaml.snakeyaml.constructor.SafeConstructor.constructMapping2ndStep(SafeConstructor.java:184)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructMapping(BaseConstructor.java:446)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.construct(Constructor.java:136)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObjectNoCheck(BaseConstructor.java:215)
	at org.yaml.snakeyaml.constructor.BaseConstructor.constructObject(BaseConstructor.java:205)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.newInstance(Constructor.java:283)
	at org.yaml.snakeyaml.constructor.Constructor$ConstructMapping.constructJavaBean2ndStep(Constructor.java:245)
	... 29 more
Caused by: java.lang.InstantiationException
	at org.yaml.snakeyaml.constructor.BaseConstructor.newInstance(BaseConstructor.java:325)
	at org.yaml.snakeyaml.constructor.BaseConstructor.newInstance(BaseConstructor.java:294)
	at org.yaml.snakeyaml.constructor.BaseConstructor.newInstance(BaseConstructor.java:287)
	... 40 more
```
  
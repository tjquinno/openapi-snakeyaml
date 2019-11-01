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
java.lang.ClassCastException: class java.util.LinkedHashMap cannot be cast to class org.eclipse.microprofile.openapi.models.PathItem
(java.util.LinkedHashMap is in module java.base of loader 'bootstrap'; org.eclipse.microprofile.openapi.models.PathItem is in unnamed module of loader 'app')
	at java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:684)
	at java.base/java.util.Collections$UnmodifiableMap.forEach(Collections.java:1503)
	at io.helidon.examples.openapisnakeyaml.TestParser.testParser(TestParser.java:37)
```
The `Parser` class includes
```
TypeDescription pathsTD = new TypeDescription(Paths.class, PathsImpl.class);
        pathsTD.putMapPropertyType("pathItems", String.class, CustomPathItem.class);
        topConstructor.addTypeDescription(pathsTD);
```
specifying `CustomPathItem` because the original `PathItem` and `PathItemImpl` do not have a `setOperations(Map)` method. 
  
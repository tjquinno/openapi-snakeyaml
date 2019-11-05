# openapi-snakeyaml

Simple example loading a MicroProfile OpenAPI 1.1 yaml file using SnakeYAML 1.24.

This example shows one way to handle the fact that some MP OpenAPI interfaces (`Paths` and `APIResponses` among others) 
extend `Map`. A bit of code in a custom `Constructor` does the trick. Thanks to Andrey and Alexander for their suggestions 
on this issue: https://bitbucket.org/asomov/snakeyaml/issues/463/pojo-implements-map-so-snakeyaml

To run the test:

```mvn test```

Issues:

1. The `TestParser.testParserUsingJSON` test currently fails because the `petstore.json` file uses `$ref` which SnakeYAML 
does not seem to be handling.
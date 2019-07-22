[![Build Status](https://travis-ci.org/franz-see/swagger-codegen-micronaut-generator.svg?branch=master)](https://travis-ci.org/franz-see/swagger-codegen-micronaut-generator) 
[ ![Download](https://api.bintray.com/packages/franz-see/maven-repo/swagger-codegen-micronaut-generator/images/download.svg?version=1.0.0) ](https://bintray.com/franz-see/maven-repo/swagger-codegen-micronaut-generator/0.0.1/link)

# swagger-codegen-micronaut-generator
Template to Generate Micronaut-based server side java application from a swagger/openapi specification

# How to Use

First, Download the jars

```
# download swagger cli
curl -O https://repo1.maven.org/maven2/io/swagger/codegen/v3/swagger-codegen-cli/3.0.10/swagger-codegen-cli-3.0.10.jar

# download micronaut generator
curl -O https://dl.bintray.com/franz-see/maven-repo/ph/net/see/swagger-codegen-micronaut-generator/1.0.0/swagger-codegen-micronaut-generator-1.0.0.jar
```

Next, use both jars to generate a micronaut project based on your swagger.yaml

```
java -cp swagger-codegen-micronaut-generator-1.0.0.jar:swagger-codegen-cli-3.0.10.jar \
  io.swagger.codegen.v3.cli.SwaggerCodegen \
  generate \
  -l ph.net.see.swaggercodegenmicronautgenerator.MicronautCodegen \
  -i /path/to/your/swagger.yaml \
  -o /where/you/want/the/project/to/be/generated
``` 

This will read your swagger/openapi configuration `/path/to/your/swagger.yaml` and generate the project directory in `/where/you/want/the/project/to/be/generated`. 


# Technical Note
This project has been heavily copied from SpringCodegen :) It will grow out of it soon but for now, it's very much a copy of it but changed the spring parts into micronaut :)
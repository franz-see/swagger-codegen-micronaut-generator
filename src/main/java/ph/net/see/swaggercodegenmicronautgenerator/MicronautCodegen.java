package ph.net.see.swaggercodegenmicronautgenerator;

import com.github.jknack.handlebars.Lambda;
import com.google.common.collect.ImmutableMap;
import io.swagger.codegen.v3.*;
import io.swagger.codegen.v3.generators.features.BeanValidationFeatures;
import io.swagger.codegen.v3.generators.features.OptionalFeatures;
import io.swagger.codegen.v3.generators.handlebars.lambda.*;
import io.swagger.codegen.v3.generators.java.AbstractJavaCodegen;
import io.swagger.codegen.v3.utils.URLPathUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ph.net.see.swaggercodegenmicronautgenerator.lambda.CapitaliseLambda;
import ph.net.see.swaggercodegenmicronautgenerator.lambda.EscapeDoubleQuotesLambda;
import ph.net.see.swaggercodegenmicronautgenerator.lambda.RemoveLineBreakLambda;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ENUMS_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class MicronautCodegen extends AbstractJavaCodegen implements BeanValidationFeatures, OptionalFeatures {
    static Logger LOGGER = LoggerFactory.getLogger(MicronautCodegen.class);
    public static final String TITLE = "title";
    public static final String CONFIG_PACKAGE = "configPackage";
    public static final String BASE_PACKAGE = "basePackage";
    public static final String INTERFACE_ONLY = "interfaceOnly";
    public static final String DELEGATE_PATTERN = "delegatePattern";
    public static final String SINGLE_CONTENT_TYPES = "singleContentTypes";
    public static final String ASYNC = "async";
    public static final String RESPONSE_WRAPPER = "responseWrapper";
    public static final String USE_TAGS = "useTags";
    public static final String IMPLICIT_HEADERS = "implicitHeaders";
    public static final String SWAGGER_DOCKET_CONFIG = "swaggerDocketConfig";

    protected String title = "swagger-petstore";
    protected String configPackage = "io.swagger.configuration";
    protected String basePackage = "io.swagger";
    protected boolean interfaceOnly = true;
    protected boolean delegatePattern = false;
    protected boolean delegateMethod = false;
    protected boolean singleContentTypes = false;
    protected boolean async = false;
    protected String responseWrapper = "";
    protected boolean useTags = false;
    protected boolean useBeanValidation = true;
    protected boolean implicitHeaders = false;
    protected boolean swaggerDocketConfig = false;
    protected boolean useOptional = false;

    public MicronautCodegen() {
        super();
        outputFolder = "generated-code/javaMicronaut";
        apiPackage = "io.swagger.api";
        modelPackage = "io.swagger.model";
        invokerPackage = "io.swagger.api";
        artifactId = "swagger-micronaut";

        additionalProperties.put(CONFIG_PACKAGE, configPackage);
        additionalProperties.put(BASE_PACKAGE, basePackage);

        // micronaut uses the jackson lib
        additionalProperties.put("jackson", "true");

        cliOptions.add(new CliOption(TITLE, "server title name or client service name"));
        cliOptions.add(new CliOption(CONFIG_PACKAGE, "configuration package for generated code"));
        cliOptions.add(new CliOption(BASE_PACKAGE, "base package (invokerPackage) for generated code"));
        cliOptions.add(CliOption.newBoolean(INTERFACE_ONLY, "Whether to generate only API interface stubs without the server files."));
        cliOptions.add(CliOption.newBoolean(DELEGATE_PATTERN, "Whether to generate the server files using the delegate pattern"));
        cliOptions.add(CliOption.newBoolean(SINGLE_CONTENT_TYPES, "Whether to select only one produces/consumes content-type by operation."));
        cliOptions.add(CliOption.newBoolean(ASYNC, "use async Callable controllers"));
        cliOptions.add(new CliOption(RESPONSE_WRAPPER, "wrap the responses in given type (Future,Callable,CompletableFuture,ListenableFuture,DeferredResult,HystrixCommand,RxObservable,RxSingle or fully qualified type)"));
        cliOptions.add(CliOption.newBoolean(USE_TAGS, "use tags for creating interface and controller classnames"));
        cliOptions.add(CliOption.newBoolean(USE_BEANVALIDATION, "Use BeanValidation API annotations"));
        cliOptions.add(CliOption.newBoolean(IMPLICIT_HEADERS, "Use of @ApiImplicitParams for headers."));
        cliOptions.add(CliOption.newBoolean(SWAGGER_DOCKET_CONFIG, "Generate Micronaut Swagger Docket configuration class."));
        cliOptions.add(CliOption.newBoolean(USE_OPTIONAL,
                "Use Optional container for optional parameters"));

        supportedLibraries.put(DEFAULT_LIBRARY, "Java Micronaut Server application.");
        setLibrary(DEFAULT_LIBRARY);

        CliOption library = new CliOption(CodegenConstants.LIBRARY, "library template (sub-template) to use");
        library.setDefault(DEFAULT_LIBRARY);
        library.setEnum(supportedLibraries);
        library.setDefault(DEFAULT_LIBRARY);
        cliOptions.add(library);
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    @Override
    public String getName() {
        return "micronaut";
    }

    @Override
    public String getHelp() {
        return "Generates a Java Micronaut Server application.";
    }

    @Override
    public void processOpts() {
        setUseOas2(false);
        additionalProperties.put(CodegenConstants.USE_OAS2, false);

        if (additionalProperties.containsKey("httpMethod")) {
            String httpMethod = (String) additionalProperties.get("httpMethod");
            String httpMethodNormalCase = Character.toUpperCase(httpMethod.charAt(0)) + httpMethod.substring(1);
            additionalProperties.put("httpMethodNormalCase", httpMethodNormalCase);
        }


        // set invokerPackage as basePackage
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setBasePackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
            additionalProperties.put(BASE_PACKAGE, basePackage);
            LOGGER.info("Set base package to invoker package (" + basePackage + ")");
        }

        super.processOpts();

        if (StringUtils.isBlank(templateDir)) {
            embeddedTemplateDir = templateDir = getTemplateDir();
        }

        // clear model and api doc template as this codegen
        // does not support auto-generated markdown doc at the moment
        //TODO: add doc templates
        modelDocTemplateFiles.remove("model_doc.mustache");
        apiDocTemplateFiles.remove("api_doc.mustache");

        if (additionalProperties.containsKey(TITLE)) {
            this.setTitle((String) additionalProperties.get(TITLE));
        }

        if (additionalProperties.containsKey(CONFIG_PACKAGE)) {
            this.setConfigPackage((String) additionalProperties.get(CONFIG_PACKAGE));
        }

        if (additionalProperties.containsKey(BASE_PACKAGE)) {
            this.setBasePackage((String) additionalProperties.get(BASE_PACKAGE));
        }

        if (additionalProperties.containsKey(INTERFACE_ONLY)) {
            this.setInterfaceOnly(Boolean.valueOf(additionalProperties.get(INTERFACE_ONLY).toString()));
        }

        if (additionalProperties.containsKey(DELEGATE_PATTERN)) {
            this.setDelegatePattern(Boolean.valueOf(additionalProperties.get(DELEGATE_PATTERN).toString()));
        }

        if (additionalProperties.containsKey(SINGLE_CONTENT_TYPES)) {
            this.setSingleContentTypes(Boolean.valueOf(additionalProperties.get(SINGLE_CONTENT_TYPES).toString()));
        }

        if (additionalProperties.containsKey(ASYNC)) {
            this.setAsync(Boolean.valueOf(additionalProperties.get(ASYNC).toString()));
        }

        if (additionalProperties.containsKey(RESPONSE_WRAPPER)) {
            this.setResponseWrapper((String) additionalProperties.get(RESPONSE_WRAPPER));
        }

        if (additionalProperties.containsKey(USE_TAGS)) {
            this.setUseTags(Boolean.valueOf(additionalProperties.get(USE_TAGS).toString()));
        }

        if (additionalProperties.containsKey(USE_BEANVALIDATION)) {
            this.setUseBeanValidation(convertPropertyToBoolean(USE_BEANVALIDATION));
        }

        if (additionalProperties.containsKey(USE_OPTIONAL)) {
            this.setUseOptional(convertPropertyToBoolean(USE_OPTIONAL));
        }

        if (useBeanValidation) {
            writePropertyBack(USE_BEANVALIDATION, useBeanValidation);
        }

        if (additionalProperties.containsKey(IMPLICIT_HEADERS)) {
            this.setImplicitHeaders(Boolean.valueOf(additionalProperties.get(IMPLICIT_HEADERS).toString()));
        }

        if (additionalProperties.containsKey(SWAGGER_DOCKET_CONFIG)) {
            this.setSwaggerDocketConfig(Boolean.valueOf(additionalProperties.get(SWAGGER_DOCKET_CONFIG).toString()));
        }

        if (useOptional) {
            writePropertyBack(USE_OPTIONAL, useOptional);
        }

        if (this.interfaceOnly && this.delegatePattern) {
            this.delegateMethod = true;
            additionalProperties.put("delegate-method", true);
        }

        supportingFiles.add(new SupportingFile("pom.mustache", "", "pom.xml"));
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));

        if (!this.interfaceOnly) {

            supportingFiles.add(new SupportingFile("homeController.mustache",
                    (sourceFolder + File.separator + configPackage).replace(".", File.separator), "HomeController.java"));
            supportingFiles.add(new SupportingFile("mainApplication.mustache",
                    (sourceFolder + File.separator + basePackage).replace(".", File.separator), "MainApplication.java"));
            supportingFiles.add(new SupportingFile("RFC3339DateFormat.mustache",
                    (sourceFolder + File.separator + basePackage).replace(".", File.separator), "RFC3339DateFormat.java"));
            supportingFiles.add(new SupportingFile("application.mustache",
                    ("src.main.resources").replace(".", File.separator), "application.yml"));
            apiTemplateFiles.put("apiController.mustache", "Controller.java");
            supportingFiles.add(new SupportingFile("apiException.mustache",
                    (sourceFolder + File.separator + apiPackage).replace(".", File.separator), "ApiException.java"));
            supportingFiles.add(new SupportingFile("apiResponseMessage.mustache",
                    (sourceFolder + File.separator + apiPackage).replace(".", File.separator), "ApiResponseMessage.java"));
            supportingFiles.add(new SupportingFile("notFoundException.mustache",
                    (sourceFolder + File.separator + apiPackage).replace(".", File.separator), "NotFoundException.java"));
        }

        if (this.delegatePattern && !this.delegateMethod) {
            additionalProperties.put("isDelegate", "true");
            apiTemplateFiles.put("apiDelegate.mustache", "Delegate.java");
        }

        if (this.async) {
            additionalProperties.put(RESPONSE_WRAPPER, "CompletableFuture");
        }

        switch (this.responseWrapper) {
            case "Future":
            case "Callable":
            case "CompletableFuture":
                additionalProperties.put(RESPONSE_WRAPPER, "java.util.concurrent" + this.responseWrapper);
                break;
            case "HystrixCommand":
                additionalProperties.put(RESPONSE_WRAPPER, "com.netflix.hystrix.HystrixCommand");
                break;
            case "RxObservable":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Observable");
                break;
            case "RxSingle":
                additionalProperties.put(RESPONSE_WRAPPER, "rx.Single");
                break;
            default:
                break;
        }

        addHandlebarsLambdas(additionalProperties);
    }

    private void addHandlebarsLambdas(Map<String, Object> objs) {
        Map<String, Lambda> lambdas = new ImmutableMap.Builder<String, Lambda>()
                .put("lowercase", new LowercaseLambda().generator(this))
                .put("uppercase", new UppercaseLambda())
                .put("titlecase", new TitlecaseLambda())
                .put("camelcase", new CamelCaseLambda().generator(this))
                .put("camelcase_param", new CamelCaseLambda().generator(this).escapeAsParamName(true))
                .put("indented", new IndentedLambda())
                .put("indented_8", new IndentedLambda(8, " "))
                .put("indented_12", new IndentedLambda(12, " "))
                .put("indented_16", new IndentedLambda(16, " "))
                .put("capitalise", new CapitaliseLambda())
                .put("escapeDoubleQuote", new EscapeDoubleQuotesLambda())
                .put("removeLineBreak", new RemoveLineBreakLambda())
                .build();

        if (objs.containsKey("lambda")) {
            LOGGER.warn("An property named 'lambda' already exists. Mustache lambdas renamed from 'lambda' to '_lambda'. " +
                    "You'll likely need to use a custom template, " +
                    "see https://github.com/swagger-api/swagger-codegen#modifying-the-client-library-format. ");
            objs.put("_lambda", lambdas);
        } else {
            objs.put("lambda", lambdas);
        }
    }

    @Override
    public void addOperationToGroup(String tag, String resourcePath, Operation operation, CodegenOperation co, Map<String, List<CodegenOperation>> operations) {
        if(!useTags) {
            String basePath = resourcePath;
            if (basePath.startsWith("/")) {
                basePath = basePath.substring(1);
            }
            int pos = basePath.indexOf("/");
            if (pos > 0) {
                basePath = basePath.substring(0, pos);
            }

            if (basePath.equals("")) {
                basePath = "default";
            } else {
                co.subresourceOperation = !co.path.isEmpty();
            }
            List<CodegenOperation> opList = operations.get(basePath);
            if (opList == null) {
                opList = new ArrayList<CodegenOperation>();
                operations.put(basePath, opList);
            }
            opList.add(co);
            co.baseName = basePath;
        } else {
            super.addOperationToGroup(tag, resourcePath, operation, co, operations);
        }
    }

    @Override
    public String getArgumentsLocation() {
        return null;
    }

    @Override
    public String getDefaultTemplateDir() {
        return "JavaMicronaut";
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        if(!additionalProperties.containsKey(TITLE)) {
            // From the title, compute a reasonable name for the package and the API
            String title = openAPI.getInfo().getTitle();

            // Drop any API suffix
            if (title != null) {
                title = title.trim().replace(" ", "-");
                if (title.toUpperCase().endsWith("API")) {
                    title = title.substring(0, title.length() - 3);
                }

                this.title = camelize(sanitizeName(title), true);
            }
            additionalProperties.put(TITLE, this.title);
        }

        final URL urlInfo = URLPathUtil.getServerURL(openAPI);
        String port = "8080"; // Default value for a JEE Server
        if ( urlInfo != null && urlInfo.getPort() != 0) {
            port = String.valueOf(urlInfo.getPort());
        }

        this.additionalProperties.put("serverPort", port);
        if (openAPI.getPaths() != null) {
            for (String pathname : openAPI.getPaths().keySet()) {
                PathItem pathItem = openAPI.getPaths().get(pathname);
                final List<Operation> operations = pathItem.readOperations();
                for (Operation operation : operations) {
                    if (operation.getTags() != null) {
                        List<Map<String, String>> tags = new ArrayList<>();
                        for (String tag : operation.getTags()) {
                            Map<String, String> value = new HashMap<>();
                            value.put("tag", tag);
                            value.put("hasMore", "true");
                            tags.add(value);
                        }
                        if (tags.size() > 0) {
                            tags.get(tags.size() - 1).remove("hasMore");
                        }
                        if (operation.getTags().size() > 0) {
                            String tag = operation.getTags().get(0);
                            operation.setTags(Arrays.asList(tag));
                        }
                        operation.addExtension("x-tags", tags);
                    }
                }
            }
        }
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (final CodegenOperation operation : ops) {
                List<CodegenResponse> responses = operation.responses;
                if (responses != null) {
                    for (final CodegenResponse resp : responses) {
                        if ("0".equals(resp.code)) {
                            resp.code = "200";
                        }
                        doDataTypeAssignment(resp.dataType, new DataTypeAssigner() {
                            @Override
                            public void setReturnType(final String returnType) {
                                resp.dataType = returnType;
                            }

                            @Override
                            public void setReturnContainer(final String returnContainer) {
                                resp.containerType = returnContainer;
                            }
                        });
                    }
                }

                doDataTypeAssignment(operation.returnType, new DataTypeAssigner() {

                    @Override
                    public void setReturnType(final String returnType) {
                        operation.returnType = returnType;
                    }

                    @Override
                    public void setReturnContainer(final String returnContainer) {
                        operation.returnContainer = returnContainer;
                    }
                });

                if(implicitHeaders){
                    removeHeadersFromAllParams(operation.allParams);
                    removeHeadersFromContents(operation.contents);
                }
            }
        }

        return objs;
    }

    private interface DataTypeAssigner {
        void setReturnType(String returnType);
        void setReturnContainer(String returnContainer);
    }

    /**
     *
     * @param returnType The return type that needs to be converted
     * @param dataTypeAssigner An object that will assign the data to the respective fields in the model.
     */
    private void doDataTypeAssignment(String returnType, MicronautCodegen.DataTypeAssigner dataTypeAssigner) {
        final String rt = returnType;
        if (rt == null) {
            dataTypeAssigner.setReturnType("Void");
        } else if (rt.startsWith("List")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("List<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("List");
            }
        } else if (rt.startsWith("Map")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Map<".length(), end).split(",")[1].trim());
                dataTypeAssigner.setReturnContainer("Map");
            }
        } else if (rt.startsWith("Set")) {
            int end = rt.lastIndexOf(">");
            if (end > 0) {
                dataTypeAssigner.setReturnType(rt.substring("Set<".length(), end).trim());
                dataTypeAssigner.setReturnContainer("Set");
            }
        }
    }

    /**
     * This method removes header parameters from the list of parameters and also
     * corrects last allParams hasMore state.
     * @param allParams list of all parameters
     */
    private void removeHeadersFromAllParams(List<CodegenParameter> allParams) {
        if(allParams.isEmpty()){
            return;
        }
        final ArrayList<CodegenParameter> copy = new ArrayList<>(allParams);
        allParams.clear();

        for(CodegenParameter p : copy){
            if(!getBooleanValue(p, CodegenConstants.IS_HEADER_PARAM_EXT_NAME)){
                allParams.add(p);
            }
        }
        allParams.get(allParams.size()-1).getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);
    }

    private void removeHeadersFromContents(List<CodegenContent> contents) {
        if(contents == null || contents.isEmpty()){
            return;
        }
        for(int index = 0; index < contents.size(); index++) {
            final CodegenContent codegenContent = contents.get(index);
            final List<CodegenParameter> parameters = codegenContent.getParameters();
            if (parameters == null || parameters.isEmpty()) {
                continue;
            }
            final List<CodegenParameter> filteredParameters = parameters.stream()
                    .filter(codegenParameter -> !getBooleanValue(codegenParameter, CodegenConstants.IS_HEADER_PARAM_EXT_NAME))
                    .collect(Collectors.toList());
            parameters.clear();
            parameters.addAll(filteredParameters);
            parameters.get(parameters.size()-1).getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);
        }
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        List<CodegenSecurity> authMethods = (List<CodegenSecurity>) objs.get("authMethods");
        if (authMethods != null) {
            for (CodegenSecurity authMethod : authMethods) {
                authMethod.name = camelize(sanitizeName(authMethod.name), true);
            }
        }
        return objs;
    }

    @Override
    public String toApiName(String name) {
        if (name.length() == 0) {
            return "DefaultApi";
        }
        name = sanitizeName(name);
        return camelize(name) + "Api";
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "ControllerTest";
    }

    public String toBooleanGetter(String name) {
        return getterAndSetterCapitalize(name);
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }

    public void setBasePackage(String configPackage) {
        this.basePackage = configPackage;
    }

    public void setInterfaceOnly(boolean interfaceOnly) { this.interfaceOnly = interfaceOnly; }

    public void setDelegatePattern(boolean delegatePattern) { this.delegatePattern = delegatePattern; }

    public void setSingleContentTypes(boolean singleContentTypes) {
        this.singleContentTypes = singleContentTypes;
    }

    public void setAsync(boolean async) { this.async = async; }

    public void setResponseWrapper(String responseWrapper) { this.responseWrapper = responseWrapper; }

    public void setUseTags(boolean useTags) {
        this.useTags = useTags;
    }

    public void setImplicitHeaders(boolean implicitHeaders) {
        this.implicitHeaders = implicitHeaders;
    }

    public void setSwaggerDocketConfig(boolean swaggerDocketConfig) {
        this.swaggerDocketConfig = swaggerDocketConfig;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        super.postProcessModelProperty(model, property);

        if ("null".equals(property.example)) {
            property.example = null;
        }

        //Add imports for Jackson
        boolean isEnum = getBooleanValue(model, IS_ENUM_EXT_NAME);
        if (!Boolean.TRUE.equals(isEnum)) {
            model.imports.add("JsonProperty");
            boolean hasEnums = getBooleanValue(model, HAS_ENUMS_EXT_NAME);
            if (Boolean.TRUE.equals(hasEnums)) {
                model.imports.add("JsonValue");
            }
        } else { // enum class
            //Needed imports for Jackson's JsonCreator
            if (additionalProperties.containsKey("jackson")) {
                model.imports.add("JsonCreator");
            }
        }
        if (model.discriminator != null && model.discriminator.getPropertyName().equals(property.baseName)) {
            property.vendorExtensions.put("x-is-discriminator-property", true);

            //model.imports.add("JsonTypeId");
        }
    }

    @Override
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);

        //Add imports for Jackson
        List<Map<String, String>> imports = (List<Map<String, String>>)objs.get("imports");
        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");
            // for enum model
            boolean isEnum = getBooleanValue(cm, IS_ENUM_EXT_NAME);
            if (Boolean.TRUE.equals(isEnum) && cm.allowableValues != null) {
                cm.imports.add(importMapping.get("JsonValue"));
                Map<String, String> item = new HashMap<String, String>();
                item.put("import", importMapping.get("JsonValue"));
                imports.add(item);
            }
        }

        return objs;
    }

    public void setUseBeanValidation(boolean useBeanValidation) {
        this.useBeanValidation = useBeanValidation;
    }

    @Override
    public void setUseOptional(boolean useOptional) {
        this.useOptional = useOptional;
    }
}

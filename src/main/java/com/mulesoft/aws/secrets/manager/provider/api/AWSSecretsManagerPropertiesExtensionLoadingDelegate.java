/*
 * (c) 2003-2018 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package com.mulesoft.aws.secrets.manager.provider.api;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionLoadingDelegate;

import static com.mulesoft.aws.secrets.manager.provider.api.AWSSecretsManagerConfigurationPropertiesConstants.*;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_11;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;

import static java.util.Arrays.asList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Declares extension for Secure Properties Configuration module
 *
 * @since 1.0
 */
public class AWSSecretsManagerPropertiesExtensionLoadingDelegate implements ExtensionLoadingDelegate {

  // We need to use reflection to avoid having to bump the minMuleVersion of this extension
  // If that is not an issue then this code can be simplified a lot by upgrading the modules-parent to at least 1.5.0
  private static final Method SUPPORTING_JAVA_VERSIONS_METHOD = getSupportingJavaVersionsMethod();

  private static Method getSupportingJavaVersionsMethod() {
    try {
      return ExtensionDeclarer.class.getMethod("supportingJavaVersions", Set.class);
    } catch (NoSuchMethodException e) {
      // The method doesn't exist in this version: the Runtime we are running with doesn't care about supported Java versions
      // declaration
      return null;
    }
  }

  @Override
  public void accept(ExtensionDeclarer extensionDeclarer, ExtensionLoadingContext context) {
    declareJavaVersionSupport(extensionDeclarer, new HashSet<>(asList(JAVA_8.version(), JAVA_11.version(), JAVA_17.version())));

    ConfigurationDeclarer configurationDeclarer = extensionDeclarer.named(EXTENSION_NAME)
      .describedAs(String.format("Crafted %s Extension", EXTENSION_NAME))
      .withCategory(SELECT)
      .onVersion("1.0.0")
      .fromVendor("AWS")
      .withConfig(CONFIG_ELEMENT);

    addSecretsManagerParameters(configurationDeclarer);
  }

  private void declareJavaVersionSupport(ExtensionDeclarer extensionDeclarer, Set<String> javaVersions) {
    if (SUPPORTING_JAVA_VERSIONS_METHOD == null) {
      // Nothing to do, the Runtime we are running with doesn't care about supported Java versions declaration
      return;
    }

    try {
      SUPPORTING_JAVA_VERSIONS_METHOD.invoke(extensionDeclarer, javaVersions);
    } catch (IllegalAccessException e) {
      throw new IllegalModelDefinitionException(e);
    } catch (InvocationTargetException e) {
      throw new IllegalModelDefinitionException(e.getTargetException());
    }
  }


  /**
   * Add the Basic Connection parameters to the parameter list
   *
   * @param configurationDeclarer Extension {@link ConfigurationDeclarer}
   */
  private void addSecretsManagerParameters(ConfigurationDeclarer configurationDeclarer) {

    ParameterGroupDeclarer addSecretsManagerParametersGroup = configurationDeclarer
            .onParameterGroup(SECRETS_MANAGER_PARAMETER_GROUP)
            .withDslInlineRepresentation(true);

    ParameterGroupDeclarer addBasicConnectionParametersGroup = configurationDeclarer
            .onParameterGroup(AWS_BASIC_CONNECTION_PARAMETER_GROUP)
            .withDslInlineRepresentation(true);

    ParameterGroupDeclarer addRoleConnectionParametersGroup = configurationDeclarer
            .onParameterGroup(AWS_ROLE_CONNECTION_PARAMETER_GROUP)
            .withDslInlineRepresentation(true);

    ParameterGroupDeclarer addAdvancedConnectionParametersGroup = configurationDeclarer
            .onParameterGroup(AWS_ADVANCED_CONNECTION_PARAMETER_GROUP)
            .withDslInlineRepresentation(true);

//    ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    addBasicConnectionParametersGroup
            .withRequiredParameter (AWS_REGION)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("AWS Secrets Manager Region").build())
            .describedAs("AWS Secrets Manager region as us-east-2");

    addBasicConnectionParametersGroup
            .withOptionalParameter(AWS_ACCESS_KEY)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("AWS Access Key").build())
            .describedAs("AWS Access Key");

    addBasicConnectionParametersGroup
            .withOptionalParameter (AWS_SECRET_KEY)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("AWS Secret Key").build())
            .describedAs("AWS Secret Key");

    addBasicConnectionParametersGroup
            .withOptionalParameter (AWS_SESSION_TOKEN)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("Session Token").build())
            .describedAs("The session token provided by Amazon STS.");

    addRoleConnectionParametersGroup
            .withOptionalParameter (AWS_ROLE_ARN)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("Role ARN").build())
            .describedAs("The Role ARN unique identifies role to assume in order to gain cross account access.");

    addSecretsManagerParametersGroup
            .withRequiredParameter(SECRET_NAME)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("Secret Name").build())
            .describedAs("Name of the AWS Secret. Prepend Environment if required.");

    addAdvancedConnectionParametersGroup
            .withOptionalParameter(AWS_CUSTOM_SERVICE_ENDPOINT)
            .ofType(BaseTypeBuilder.create(JAVA).stringType().build())
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("Custom Service Endpoint").build())
            .describedAs("Sets a custom service endpoint. Useful when a non-standard service endpoint is required, such as a VPC endpoint.");

    addAdvancedConnectionParametersGroup
            .withOptionalParameter(AWS_USE_DEFAULT_PROVIDER_CHAIN)
            .ofType(BaseTypeBuilder.create(JAVA).booleanType().build())
            .defaultingTo(Boolean.FALSE)
            .withExpressionSupport(ExpressionSupport.SUPPORTED)
            .withDisplayModel(DisplayModel.builder().displayName("Use Default AWSCredentials Provider Chain").build())
            .describedAs("Set this field to true to obtain credentials from the AWS environment, See: https://docs.aws.amazon.com/sdk-for-java/v2/developer-guide/credentials.html\"");
  }
}

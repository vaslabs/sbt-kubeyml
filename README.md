
# sbt-kubeyml 
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/1a2b9682f101488cb8bf5589e5bd7310)](https://www.codacy.com/manual/vaslabs/sbt-kubeyml?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vaslabs/sbt-kubeyml&amp;utm_campaign=Badge_Grade) [![CircleCI](https://circleci.com/gh/vaslabs/sbt-kubeyml/tree/master.svg?style=svg)](https://circleci.com/gh/vaslabs/sbt-kubeyml/tree/master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.vaslabs.kube/sbt-kubeyml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.vaslabs.kube/sbt-kubeyml) [![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)

![Sbt kubeyml logo](https://docs.google.com/drawings/d/e/2PACX-1vQDUAeGuIehxLwls0MN67VzK3h9wbVMFFJvu_Tcd12qDtS_c6-G-3KPsbBSgoqTt5BXez50LSWLvhTa/pub?w=500&h=551) 

An sbt plugin to generate typesafe kubernetes deployment plans for scala projects

## Deployment plugin

### Add the plugin to your plugins.sbt
```
addSbtPlugin("org.vaslabs.kube" % "sbt-kubeyml" % "0.3.9")
```

Add the plugin in your project and enable it
```
enablePlugins(KubeDeploymentPlugin)
```
The plugin depends on DockerPlugin from [sbt-native-packager](https://github.com/sbt/sbt-native-packager)

```
enablePlugins(DockerPlugin)
```

Try to run

```
kubeyml:gen
```


### Properties

| **sbt key**  | **description**  | **default**  | 
|---|---|---|
| namespace  | The kubernetes namespace of the deployment   |  Default value is project name | 
|  application | The name of the deployment  |  Default value is project name  |
|  dockerImage | The docker image to deploy in a single container |  Default is the picked from sbt-native-packager |
| ports | List of container ports optionally tagged with name | dockerExposedPorts from docker plugin|
| livenessProbe  | Healthcheck probe  | `HttpProbe(HttpGet("/health", 8080, List.empty), 0 seconds, 1 second, 10 seconds, 3, 1)` |
| readinessProbe  |  Probe to check when deployment is ready to receive traffic  | livenessProbe  |
| annotations  | `Map[String, String]` for spec template annotations (e.g. aws roles)  | empty  |
| replicas | the number of replicas to be deployed| 2 |
| imagePullPolicy | Image pull policy for kubernetes, set to IfNotPresent or Always | IfNotPresent |
| command | Command for the container | empty |
| args | arguments for the command | empty Seq |
| envs | Map of environment variables, raw, field path or secret are supported| empty |
| resourceRequests | Resource requests (cpu in the form of m, memory in the form of MiB |  `Resource(Cpu(100), Memory(256))` |
| resourceLimits | Resource limits (cpu in the form of m, memory in the form of MiB |  `Resource(Cpu(1000), Memory(512))` |
| target | The directory to output the deployment.yml | <target of this project> / kubeyml |
| deployment | The key to access the whole Deployment definition, exposed for further customisation | Instance with above defaults |

### Recipes

#### Single namespace, two types of deployments with secret and dependency

```scala
import kubeyml.deployment._
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._

lazy val deploymentName = sys.env.getOrElse("DEPLOYMENT_NAME", "myservice-test")
lazy val secretsName = sys.env.getOrElse("SECRETS_NAME", "myservice-test-secrets")
lazy val serviceDependencyConnection = sys.env.getOrElse("MY_DEPENDENCY", "https://localhost:8080")

lazy val deploymentSettings = Seq(
  namespace in kube := "my-namespace", //default is name in thisProject
  application in kube := deploymentName, //default is name in thisProject
  command in kube := Some("webserver"),
  args in kube := Seq("-c","/path/to/config"),
  envs in kube := Map(
    EnvName("JAVA_OPTS") -> EnvRawValue("-Xms256M -Xmx2048M"),
    EnvName("MY_DEPENDENCY_SERVICE") -> EnvRawValue(serviceDependencyConnection),
    EnvName("MY_SECRET_TOKEN") -> EnvSecretValue(name = secretsName, key = "my-token")
  ),
  resourceLimits in kube := Resource(Cpu.fromCores(2), Memory(2048+512)),
  resourceRequests in kube := Resource(Cpu(500), Memory(512)),
  //if you want you can use something like the below to modify any part of the deployment by hand
  deployment in kube := (deployment in kube).value.pullDockerImage(IfNotPresent)
)
```

#### Gitlab CI/CD usage (followup from previous)

```yaml
stages:
  - publish-image
  - deploy

.publish-template:
  stage: publish-image
  script:
      - sbt docker:publish
      - sbt kubeyml:gen
  artifacts:
      untracked: false
      paths:
        - target/kubeyml/deployment.yml

.deploy-template:
  stage: deploy
  image: docker-image-that-has-your-kubectl-config
  script:
     - kubectl apply -f target/kubeyml/deployment.yml

publish-test:
  before_script:
      export MY_DEPENDENCY=${MY_TEST_DEPENDENCY}
  extends: .publish-template

deploy-test:
  extends: .deploy-template
  dependencies:
     - publish-test

publish-prod:
  before_script:
    - export MY_DEPENDENCY=${MY_PROD_DEPENDENCY}
    - export SECRETS_NAME=${MY_PROD_SECRET_NAME}
    - export DEPLOYMENT_NAME=my-service-prod
  extends: .publish-template

deploy-prod:
  extends: .deploy-template
  dependencies:
   - publish-prod
```

## Service plugin

This plugin depends on the deployment plugin and every property is derived from that.

There's some room for customisation.

```
enablePlugins(KubeServicePlugin)
```

Then your gitlab publish template will look like (example extended from above)

```yaml
.publish-template:
  stage: publish-image
  script:
      - sbt docker:publish
      - sbt kubeyml:gen
  artifacts:
      untracked: false
      paths:
        - target/kubeyml/deployment.yml
        - target/kubeyml/service.yml
```
And deploy with
```yaml
.deploy-template:
  stage: deploy
  image: docker-image-that-has-your-kubectl-config
  script:
     - kubectl apply -f target/kubeyml/deployment.yml     
     - kubectl apply -f target/kubeyml/service.yml
```

### Properties

| **sbt key**  | **description**  | **default**  |
|---|---|---|
| portMappings  | Port mappings against the deployment (service to pod)   |  Derived from deployment |
| service  | Key configuration for modifying the service properties   |  Derived from deployment |


## Ingress Plugin

This plugin depends on the service plugin. It provides some safety nets to make sure the service name and service ports
are mapped properly to the ingress configuration.

To extend on the above, you can configure ingress generation with the following steps.

1. Enable the plugin
```sbt
enablePlugins(KubeIngressPlugin)
```

2. Set an ingress name and a hostname

```scala
lazy val ingressEnvName = sys.env.getOrElse("HELLO_INGRESS_NAME", "helloworld-ingress-test")

lazy val hostName = sys.env.getOrElse("YOUR_HOST_NAME", "your-hostname.yourdomain.smth")
```

3. Configure the plugin

```scala
  import kubeyml.protocol.NonEmptyString
  import kubeyml.deployment.plugin.Keys._
  import kubeyml.ingress.api._

  import kubeyml.ingress.plugin.Keys._
  import kubeyml.service.plugin.Keys._
  import kubeyml.ingress.{Host, HttpRule, ServiceMapping, Path => IngressPath}
  
  val ingressSettings = Seq(
      (ingressName in kube) := ingressEnvName,
      (ingressRules in kube) := List(
        HttpRule(Host(hostName), List(
          IngressPath(ServiceMapping((service in kube).value.name, 8085), "/hello-world")
        ))
      ),
      (ingressAnnotations in kube) := Map(
        Annotate.nginxIngress(), // this adds kubernetes.io/ingress.class: nginx
        Annotate.nginxRewriteTarget("/hello-world"), //this adds nginx.ingress.kubernetes.io/rewrite-target: /hello-world
        NonEmptyString("your-own-annotation-key") -> "value"
      )
    )
``` 

The command to generate the ingress is the same `kubeyml:gen` which will generate 3 yml files.

#### Gitlab CI extension 

Potentially the CI configuration evolves to

```yaml
.publish-template:
  stage: publish-image
  script:
      - sbt docker:publish
      - sbt kubeyml:gen
  artifacts:
      untracked: false
      paths:
        - target/kubeyml/deployment.yml
        - target/kubeyml/service.yml
        - target/kubeyml/ingress.yml
.deploy-template:
  stage: deploy
  image: docker-image-that-has-your-kubectl-config
  script:
     - kubectl apply -f target/kubeyml/deployment.yml
     - kubectl apply -f target/kubeyml/service.yml
     - kubectl apply -f target/kubeyml/ingress.yml
```

### Properties

| **sbt key**  | **description**  | **default**  |
|---|---|---|
| ingressRules  | A list of Rules (currently only supports HttpRule  |  N/A |
| ingressName | The name of the ingress | The application name from deployment |
| ingress  | Key configuration for modifying the ingress properties   |  Some values are derived from service |


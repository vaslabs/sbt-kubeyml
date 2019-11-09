# sbt-kubeyml [![Codacy Badge](https://api.codacy.com/project/badge/Grade/1a2b9682f101488cb8bf5589e5bd7310)](https://www.codacy.com/manual/vaslabs/sbt-kubeyml?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vaslabs/sbt-kubeyml&amp;utm_campaign=Badge_Grade) [![CircleCI](https://circleci.com/gh/vaslabs/sbt-kubeyml/tree/master.svg?style=svg)](https://circleci.com/gh/vaslabs/sbt-kubeyml/tree/master) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.vaslabs.kube/sbt-kubeyml/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.vaslabs.kube/sbt-kubeyml)
An sbt plugin to generate typesafe kubernetes deployment plans for scala projects

## Usage

### Add the plugin to your plugins.sbt
```
addSbtPlugin("org.vaslabs.kube" % "sbt-kubeyml" % "0.2.2")
```

Add the plugin in your project and enable it
```
enablePlugins(KubeDeploymentPlugin)
```
The plugin depends on a bunch of other plugins ([sbt-native-packager](https://github.com/sbt/sbt-native-packager))

```
enablePlugins(DockerPlugin)
```

Try to run

```
kubeyml:gen
```

## Properties

| sbt key  | description  | default  | 
|---|---|---|
| namespace  | The kubernetes namespace of the deployment   |  Default value is project name | 
|  application | The name of the deployment  |  Default value is project name  |
|  dockerImage | The docker image to deploy in a single container |  Default is the picked from sbt-native-packager |
| ports | List of container ports optionally tagged with name | dockerExposedPorts from docker plugin|
| livenessProbe  | Healtcheck probe  | `HttpProbe(HttpGet("/health", 8080, List.empty), 0 seconds, 1 second, 10 seconds, 3, 1)` |
| readinessProbe  |  Probe to check when deployment is ready to receive traffic  | livenessProbe  |
| annotations  | `Map[String, String]` for spec template annotations (e.g. aws roles)  | empty  |
| replicas | the number of replicas to be deployed| 2 |
| imagePullPolicy | Image pull policy for kubernetes, set to IfNotPresent or Always | Always |
| envs | Map of environment variables, raw, field path or secret are supported| empty |
| resourceRequests | Resource requests (cpu in the form of m, memory in the form of MiB |  `Resource(Cpu(500), Memory(256))` |
| resourceLimits | Resource limits (cpu in the form of m, memory in the form of MiB |  `Resource(Cpu(1000), Memory(512))` |
| target | The directory to output the deployment.yml | target of this project |
| deployment | The key to access the whole Deployment definition, exposed for further customisation | Instance with above defaults |

## Recipes

### Single namespace, two types of deployments with secret and dependency

```scala
import kubeyml.deployment.{Cpu, EnvName, EnvRawValue, EnvSecretValue, Memory, Resource}
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._

lazy val deploymentName = sys.env.getOrElse("DEPLOYMENT_NAME", "myservice-test")
lazy val secretsName = sys.env.getOrElse("SECRETS_NAME", "myservice-test-secrets")
lazy val serviceDependencyConnection = sys.env.getOrElse("MY_DEPENDENCY", "https://localhost:8080")

lazy val deploymentSettings = Seq(
  namespace in kube := "my-namespace", //default is name in thisProject
  application in kube := deploymentName, //default is name in thisProject
  envs in kube := Map(
    EnvName("JAVA_OPTS") -> EnvRawValue("-Xms256 -Xmx2048M"),
    EnvName("MY_DEPENDENCY_SERVICE") -> EnvRawValue(serviceDependencyConnection),
    EnvName("MY_SECRET_TOKEN") -> EnvSecretValue(name = secretsName, key = "my-token")
  ),
  resourceLimits in kube := Resource(Cpu.fromCores(2), Memory(2048+512)),
  resourceRequests in kube := Resource(Cpu(500), Memory(512))
)
```

### Gitlab CI/CD usage (followup from previous)

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
      untracked: true
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



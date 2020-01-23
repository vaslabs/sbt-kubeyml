---
layout: docs
title:  "Recipes"
---
## Gitlab CI recipe

This recipe assumes that you have a namespace per application. In each namespace you can deploy a
service on either test or production mode (or you can make up additional modes).
It also takes into account that you may have another http service dependency.

Your build.sbt can have the settings below:

```scala
import kubeyml.deployment._
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._

lazy val deploymentName = sys.env.getOrElse("DEPLOYMENT_NAME", "myservice-test")
lazy val secretsName = sys.env.getOrElse("SECRETS_NAME", "myservice-test-secrets")
lazy val serviceDependencyConnection = sys.env.getOrElse("MY_DEPENDENCY", "http://another-service.another-namespace:8080")

lazy val deploymentSettings = Seq(
  namespace in kube := "my-namespace", //default is name in thisProject
  application in kube := deploymentName, //default is name in thisProject
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


And your gitlab ci plan can look like
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
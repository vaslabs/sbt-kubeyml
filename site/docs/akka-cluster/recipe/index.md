---
layout: docs
title:  "Recipes"
---

# Akka cluster configuration with Akka management

You may have the configuration below in you application.conf

```
akka {
  discovery.kubernetes-api {
      # Namespace discovery path
      #
      # If this path doesn't exist, the namespace will default to "default".

      # Namespace to query for pods.
      #
      # Set this value to a specific string to override discovering the namespace using pod-namespace-path.
      pod-namespace = flerken
      pod-namespace = ${?MY_NAMESPACE}

      # Selector value to query pod API with.
      # `%s` will be replaced with the configured effective name, which defaults to the actor system name
      pod-label-selector = "app=%s"
  }
}

akka.management {
  cluster.bootstrap {
    contact-point-discovery {
      discovery-method = kubernetes-api
    }
  }
}

akka.management.http.port = 8558
akka.management.http.hostname = ${?MY_HOSTNAME}
```

For the discovery to work, the hostname needs to be the podIp. By enabling this plugin you
don't need to have this configuration in the deployment.

## Example
```sbt
import kubeyml.deployment._
import kubeyml.deployment.api._
import kubeyml.deployment.plugin.Keys._

import kubeyml.roles.akka.cluster.plugin.Keys._

lazy val akkaClusterKubernetesSettings = Seq(
  hostnameEnv in kube := Some(EnvName("MY_HOSTNAME")),
  namespaceEnv in kube := Some(EnvName("MY_NAMESPACE"))
)

lazy val deploymentSettings = Seq(
  namespace in kube := "my-namespace",
  application in kube := "my-application",
  envs in kube ++= Map(
    EnvName("ANOTHER_ENV") -> EnvRawValue("something")
  ),
  resourceRequests in kube := Resource(Cpu.fromCores(1), Memory(512)),
  resourceLimits in kube := Resource(Cpu.fromCores(2), Memory(2048 + 256))
)
```

Notice that you don't need to specify the liveness probe and the rediness probe. The `AkkaClusterPlugin` 
configures the following probes:
```
livenessProbe in kube := HttpProbe(HttpGet("/alive", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds),
readinessProbe in kube := HttpProbe(HttpGet("/ready", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds),
```

The plugin only depends on the deployment plugin. If you need this exposed via an ingress you need to configure that yourself.

## Gitlab integration

Following the example in [deployment](/deployment/recipe)  you can modify the templates as below.

```yaml
.publish-template:
  stage: publish-image
  script:
      - sbt kubeyml:gen
  artifacts:
      untracked: false
      paths:
        - target/kubeyml/

.deploy-template:
  stage: deploy
  image: docker-image-that-has-your-kubectl-config
  script:
     - kubectl apply -f target/kubeyml/deployment.yml
     - kubectl apply -f target/kubeyml/roles.yml
 ```
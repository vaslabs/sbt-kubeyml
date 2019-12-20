---
layout: docs
title:  "Service manifest"
position: 2
---
# Service

This generates a service manifest as described here https://kubernetes.io/docs/concepts/services-networking/service/#defining-a-service

Generating a service is very simple and normally you just need to enable the service plugin. 
 The service plugin depends on the deployment plugin and thus, every property of
the service is automatically derived from the deployment definition.

Consider the nginx deployment from previous, we can fully derive the service:
```scala
import kubeyml.deployment._
import kubeyml.service._
import kubeyml.deployment.api._
import scala.concurrent.duration._
val deployment = deploy.namespace("yournamespace")
    .service("nginx-deployment")
    .withImage("nginx:1.7.9")
    .withProbes(
      livenessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), period = 10 seconds),
      readinessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), failureThreshold = 10)
    ).replicas(3)
    .pullDockerImage(IfNotPresent)
    .addPorts(List(Port(None, 80))
  )

  val service = Service.fromDeployment(deployment)
 
```

Which would generate the following service.yml file

```yaml
kind: Service
apiVersion: v1
metadata:
    name: nginx-deployment
    namespace: yournamespace
spec:
    type: NodePort
    selector:
        app: nginx-deployment
    ports:
    -   name: pn80
        protocol: TCP
        port: 80
        targetPort: 80
```

At the moment there's little room for customisation. If you have a use-case you can open an issue or even better a
pull request.

## Sbt Properties

These are the available properties that you can customise from the sbt layer. 

| **sbt key**  | **description**  | **default**  |
|---|---|---|
| portMappings  | Port mappings against the deployment (service to pod)   |  Derived from deployment |
| service  | Key configuration for modifying the service properties   |  Derived from deployment |

You can also create your own instance
of the service case class and see how far customisation can get you. 

There's no DSL for this yet.

Go to the [service recipe](recipe/) for a full deployment example.





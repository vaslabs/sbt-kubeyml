---
layout: docs
title:  "Deployment manifest"
position: 1
---
# Deployment

Deployment generates a deployment.yml file as specified in 

https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#creating-a-deployment

The API is a bit opinionated from experiences in production and it forces you to set a liveness probe and a readiness probe.
It also sets defaults on resources but you can customise your own.


Because deployment manifests can be quite big there is a DSL so you can create your own. The below

```scala
import kubeyml.deployment._
import kubeyml.deployment.api._
import scala.concurrent.duration._

deploy.namespace("yournamespace")
    .service("nginx-deployment")
     .withImage("nginx:1.7.9")
    .withProbes(
      livenessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), period = 10 seconds), 
      readinessProbe = HttpProbe(HttpGet("/", port = 80, httpHeaders = List.empty), failureThreshold = 10)
    ).replicas(3)
    .pullDockerImage(IfNotPresent)
    .addPorts(List(Port(None, 80)))
    .deploymentStrategy(RollingUpdate(0, 1)/*Or Recreate*/)
```

Would generate this yaml file

```yaml
kind: Deployment
apiVersion: apps/v1
metadata:
    name: nginx-deployment
    namespace: yournamespace
spec:
    replicas: 3
    selector:
        matchLabels:
            app: nginx-deployment
    template:
        metadata:
            labels:
                app: nginx-deployment
            annotations: {}
        spec:
            containers:
            -   imagePullPolicy: IfNotPresent
                ports:
                -   containerPort: 80
                image: nginx:1.7.9
                readinessProbe:
                    httpGet:
                        path: /
                        port: 80
                    timeoutSeconds: 1
                    periodSeconds: 10
                    initialDelaySeconds: 0
                    successThreshold: 1
                    failureThreshold: 10
                name: nginx-deployment
                livenessProbe:
                    timeoutSeconds: 1
                    failureThreshold: 3
                    httpGet:
                        path: /
                        port: 80
                    initialDelaySeconds: 0
                    successThreshold: 1
                    periodSeconds: 10
                resources:
                    requests:
                        cpu: 100m
                        memory: 256Mi
                    limits:
                        cpu: 1000m
                        memory: 512Mi
    strategy:
        type: RollingUpdate
        rollingUpdate:
            maxSurge: 0
            maxUnavailable: 1
```

## Sbt Properties

On top of all this, there is an sbt layer which allows you to interact exclusively with sbt keys.

| **sbt key**       | **description**                                                                      | **default**                                                                              | 
|-------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| namespace         | The kubernetes namespace of the deployment                                           | Default value is project name                                                            | 
| application       | The name of the deployment                                                           | Default value is project name                                                            |
| dockerImage       | The docker image to deploy in a single container                                     | Default is the picked from sbt-native-packager                                           |
| ports             | List of container ports optionally tagged with name                                  | dockerExposedPorts from docker plugin                                                    |
| livenessProbe     | Healthcheck probe                                                                    | `HttpProbe(HttpGet("/health", 8080, List.empty), 0 seconds, 1 second, 10 seconds, 3, 1)` |
| readinessProbe    | Probe to check when deployment is ready to receive traffic                           | livenessProbe                                                                            |
| annotations       | `Map[String, String]` for spec template annotations (e.g. aws roles)                 | empty                                                                                    |
| replicas          | the number of replicas to be deployed                                                | 2                                                                                        |
| imagePullPolicy   | Image pull policy for kubernetes, set to IfNotPresent or Always                      | IfNotPresent                                                                             |
| command           | Command for the container                                                            | empty                                                                                    |
| args              | arguments for the command                                                            | empty Seq                                                                                |
| envs              | Map of environment variables, raw, field path or secret are supported                | empty                                                                                    |
| resourceRequests  | Resource requests (cpu in the form of m, memory in the form of MiB                   | `Resource(Cpu(100), Memory(256))`                                                        |
| resourceLimits    | Resource limits (cpu in the form of m, memory in the form of MiB                     | `Resource(Cpu(1000), Memory(512))`                                                       |
| target            | The directory to output the deployment.yml                                           | target of this project                                                                   |
| deployment        | The key to access the whole Deployment definition, exposed for further customisation | Instance with above defaults                                                             |
| persistentVolumes | Persistent volumes that should be used by deployment pods                            | empty Seq                                                                                |


Go to the [deployment recipe](recipe/) for a full deployment example.

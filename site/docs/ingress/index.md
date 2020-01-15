---
layout: docs
title:  "Ingress manifest"
position: 3
---
# Ingress

This generates an ingress as defined here https://kubernetes.io/docs/concepts/services-networking/ingress/#the-ingress-resource
minus the apiVersion which still uses the extension/v1beta1 (see [here](https://github.com/vaslabs/sbt-kubeyml/issues/37) for progress)


The ingress derives some properties from the service but it requires you to set the hostname.

If we continue with our nginx example from the [service](service) section, the ingress looks like this

```scala mdoc:silent

import kubeyml.ingress.{CustomIngress, Host, HttpRule, Ingress, ServiceMapping, Path => IngressPath, Spec => IngressSpec}
import kubeyml.ingress.api.Annotate

import kubeyml.deployment.{HttpProbe, IfNotPresent, HttpGet, Port => DeployPort}
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
    .addPorts(List(DeployPort(None, 80))
  )

  val service = Service.fromDeployment(deployment)
 
val ingress: Ingress = CustomIngress(
    "nginx-ingress",
    "yournamespace",
    Map(Annotate.nginxRewriteTarget("/"), Annotate.nginxIngress()),
    IngressSpec(
      List(HttpRule(Host("your-host.domain.smth"),
      List(IngressPath(ServiceMapping("nginx-deployment", 80), "/testpath"))))
    )
)
```

Since version 0.2.9, ingress uses the new API version following this migration [guide](https://kubernetes.io/blog/2019/07/18/api-deprecations-in-1-16/) 

This would generate the following yaml file
```yaml
apiVersion: networking.k8s.io/v1beta1
kind: Ingress
metadata:
    annotations:
        nginx.ingress.kubernetes.io/rewrite-target: /
        kubernetes.io/ingress.class: nginx
    name: nginx-ingress
    namespace: yournamespace
spec:
    rules:
    -   host: your-host.domain.smth
        http:
            paths:
            -   backend:
                    serviceName: nginx-deployment
                    servicePort: 80
                path: /testpath
```

To switch to the legacy apiVersion simply do:
```scala mdoc:silent
def toExtensions_v1beta1(customIngress: CustomIngress) =
    customIngress.legacy
```


Of course in most cases you won't have to write any of this as the sbt properties that sit on top of the API are
derived from the service plugin which this depends upon. See the [recipe](recipe/) for an example.

## Sbt properties

| **sbt key**  | **description**  | **default**  |
|---|---|---|
| ingressRules  | A list of Rules (currently only supports HttpRule  |  N/A |
| ingressName | The name of the ingress | The application name from deployment |
| ingress  | Key configuration for modifying the ingress properties   |  Some values are derived from service |


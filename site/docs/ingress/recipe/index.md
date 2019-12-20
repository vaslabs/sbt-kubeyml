---
layout: docs
title:  "Recipes"
---
## Gitlab CI recipe

It is recommended that you set a custom ingress name despite the fact that it is derived from the service name.
You also need to set a host and the rules against the service ports (there is no dsl here to help with programmatic mapping)

But if you make a mistake and you use a service name or a port that doesn't match, you'll get an error with details
on what's going wrong.


```
enablePlugins(KubeIngressPlugin)
```


```scala
lazy val ingressEnvName = sys.env.getOrElse("HELLO_INGRESS_NAME", "helloworld-ingress-test")

lazy val hostName = sys.env.getOrElse("YOUR_HOST_NAME", "your-hostname.yourdomain.smth")

  import kubeyml.protocol.NonEmptyString
  import kubeyml.deployment.plugin.Keys._
  import kubeyml.ingress.api._
  import kubeyml.ingress.plugin.Keys._
  import kubeyml.service.plugin.Keys._

  import kubeyml.ingress.plugin.Keys._
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

The command is the same
```sbtshell
kubeyml:gen
```

And will generate an ingress.yml file


Now you can extend your gitlab ci to look like the below.
```yaml
.publish-template:
  stage: publish-image
  script:
      - sbt docker:publish
      - sbt kubeyml:gen
  artifacts:
      untracked: true
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


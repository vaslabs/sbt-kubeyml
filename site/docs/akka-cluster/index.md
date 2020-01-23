---
layout: docs
title:  "Akka Cluster"
position: 4
---

# Akka cluster support

Akka management supports cluster bootstrapping via the [Kubernetes API](https://doc.akka.io/docs/akka-management/current/bootstrap/kubernetes-api.html)

This may require several configurations in application.conf and Kubernetes manifests to get it working. The 
plugin provides the ability to auto-generate the roles yaml file and also inject the deployment with environment 
variables of the user's choice.

See the [recipes](recipe/) section for examples and the [settings](settings/) for explanation on the available options.

Alternatively you can get started by adding enabling the plugin and let the API lead you.

```
enablePlugins(KubeDeploymentPlugin, AkkaClusterPlugin)
```

```sbtshell
kubeyml:gen
```
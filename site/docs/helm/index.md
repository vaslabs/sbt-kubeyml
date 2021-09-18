---
layout: docs
title:  "Helm Support"
position: 4
---
# Helm support

**This is an experimental feature**

It is possible since 0.3.9 to generate a helm compatible structure.

You have to simply enable HelmPlugin

```scala  
  enablePlugins(KubeDeploymentPlugin, KubeServicePlugin, KubeIngressPlugin, HelmPlugin)
```

This will generate under target/kubeyml a Chart.yml and a templates/ directory
with all the kubernetes manifests already rendered.

The aim for this is that you can have the whole templating done with Scala
in your sbt build configuration.

## Mixing the file

If you want to have a hybrid solution you can mix helm templates and 
this plugin. 

In order to make the plugin store the files somewhere other than target/kubeyml
you can override the setting.


```scala
(kube / target) := ".deployment/templates"
```

Remember that if you override the target you need to point to a templates directory


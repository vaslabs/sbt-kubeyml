---
layout: docs
title:  "Settings"
---

# Plugin configuration

Depending on the configuration you use you may need to set the names for the following environment variables.

| **sbt key**  | **description**  | **default**  |
|---|---|---|
| discoveryMethodEnv  | The environment variable name that controls the discovery method. The value will be set to kubernetes-api  |  None |
| hostnameEnv | The environment variable name that controls the hostname. This will be set into a kubernetes field value (status.podIP) | None |
| namespaceEnv  | The environment variable name that controls the namespace. This will be set to the namespace defined in the deployment   |  None |


If you don't set this values, the plugin assume that you have set them manually in the deployment settings and it
won't attempt to define them. Remember that these are for the environment variable **names** not the values. The
values are set automatically as specified in the Akka documentation for kubernetes api discovery [here](https://doc.akka.io/docs/akka-management/current/bootstrap/kubernetes-api.html)


The generated yaml file can contain the following environment variables
```yaml
env:
                -   name: MY_DISCOVERY_METHOD
                    value: kubernetes-api
                -   name: MY_HOSTNAME
                    valueFrom:
                        fieldRef:
                            fieldPath: status.podIP
                -   name: AKKA_CLUSTER_BOOTSTRAP_SERVICE_NAME
                    valueFrom:
                        fieldRef:
                            fieldPath: metadata.labels['app']
```

The AKKA_CLUSTER_BOOTSTRAP_SERVICE_NAME environment variable is added by default. You can only control
the 3 environment variables mentioned in the table above.
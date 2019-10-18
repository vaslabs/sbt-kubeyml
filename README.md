# sbt-kubeyml
WIP: An sbt plugin to generate typesafe kubernetes deployment plans for scala projects

## Usage
Note: Not published yet. 

Publish the plugin
```
sbt publishLocal
```

Add the plugin in your project and enable it
```
enablePlugins(KubeDeploymentPlugin)
```
The plugin depends on a bunch of other plugins (sbt-native-packager)

```
enablePlugins(DockerPlugin)
```

Try to run

```
kubeyml:gen
```

Watch this page, more coming soon

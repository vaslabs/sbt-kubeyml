---
layout: docs
title:  "Recipes"
---
Enable the plugin in your module that you want to deploy

```
enablePlugins(KubeServicePlugin)
```
The command is the same
```sbtshell
kubeyml:gen
```

Then your gitlab publish template will look like (example extended from [deployment recipe](/deployment/recipe))

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
```
And deploy with
```yaml
.deploy-template:
  stage: deploy
  image: docker-image-that-has-your-kubectl-config
  script:
     - kubectl apply -f target/kubeyml/deployment.yml     
     - kubectl apply -f target/kubeyml/service.yml
```
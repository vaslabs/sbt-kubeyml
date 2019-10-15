package kubeyml.deployment.api

sealed trait EmptyDeployment
object EmptyDeployment extends EmptyDeployment
case class NamespaceDeployment(namespace: String)
case class AppDeployment(namespace: String, service: String)
case class DockerisedAppDeployment(namespace: String, service: String, image: String)
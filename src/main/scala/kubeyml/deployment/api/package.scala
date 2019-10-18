package kubeyml.deployment

package object api {

  def deploy: EmptyDeployment =
    EmptyDeployment


  implicit final class KubernetesDeploymentOps(val kubernetesDeployment: EmptyDeployment) extends AnyVal {
    def namespace(name: String): NamespaceDeployment =  NamespaceDeployment(name)
  }
  implicit final class NamespaceDeploymentOps(val namespaceDeployment: NamespaceDeployment) extends AnyVal {
    def service(name: String): AppDeployment = AppDeployment(namespaceDeployment.namespace, name)
  }

  implicit final class AppDeploymentOps(val appDeployment: AppDeployment) extends AnyVal {
    def withImage(dockerImage: String): DockerisedAppDeployment =
      DockerisedAppDeployment(appDeployment.namespace, appDeployment.service, dockerImage)
  }

  implicit final class DockerisedAppDeploymentOps(val dockerisedAppDeployment: DockerisedAppDeployment) extends AnyVal {
    import dockerisedAppDeployment._
    def withProbes(livenessProbe: Probe, readinessProbe: Probe): Deployment = Deployment(
        metadata = DeploymentMetadata(name = dockerisedAppDeployment.service, namespace = namespace),
        spec = Spec(
          replicas = 2,
          selector = Selector(MatchLabels(Some(service))),
          template = Template(
            TemplateMetadata(
              Labels(service),
              Map.empty
            ),
            TemplateSpec(
              List(
                Container(
                  service,
                  image,
                  List.empty,
                  Always,
                  livenessProbe,
                  readinessProbe,
                  Map.empty
                )
              )
            )
          ),
          strategy = RollingUpdate()
        )
      )
  }

  implicit final class DeploymentOps(val deployment: Deployment) extends AnyVal {
    def addPorts(ports: List[Port]): Deployment = deployment.addContainerPorts(ports)
    def replicas(number: Int): Deployment =
      deployment.copy(spec = deployment.spec.copy(replicas = number))
    def annotateTemplate(annotations: Map[String, String]) = deployment.annotateSpecTemplate(annotations)
  }
}

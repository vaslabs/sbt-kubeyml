package kubeyml.deployment

import scala.concurrent.duration.FiniteDuration

sealed trait KubernetesState

case class Deployment(
  apiVersion: String = "apps/v1",
  metadata: DeploymentMetadata,
  spec: Spec
) extends KubernetesState {
  private[kubeyml] def addContainerPorts(ports: List[Port]): Deployment =
    this.copy(spec = spec.addContainerPorts(ports))
  private[kubeyml] def annotateSpecTemplate(annotations: Map[String, String]): Deployment =
    this.copy(spec = spec.annotate(annotations))
}

case class DeploymentMetadata(
  name: String,
  namespace: String
)


case class Labels(app: String)

case class Spec(
  replicas: Int,
  selector: Selector,
  template: Template,
  strategy: DeploymentStrategy
) {
  private[deployment] def addContainerPorts(ports: List[Port]): Spec =
    this.copy(template = template.addContainerPorts(ports))
  private[deployment] def annotate(annotations: Map[String, String]): Spec =
    this.copy(template = template.annotate(annotations))
}

case class Selector(
  matchLabels: MatchLabels
)

case class MatchLabels(app: Option[String])

case class Template(metadata: TemplateMetadata, spec: TemplateSpec) {
  private[deployment] def addContainerPorts(ports: List[Port]): Template =
    this.copy(spec = spec.addContainerPorts(ports))

  private[deployment] def annotate(annotations: Map[String, String]): Template =
    this.copy(metadata = metadata.copy(annotations = annotations))
}

case class TemplateMetadata(labels: Labels, annotations: Map[String, String])

case class TemplateSpec(containers: List[Container]) {
  private[deployment] def addContainerPorts(ports: List[Port]): TemplateSpec =
    this.copy(containers = containers.map {
      container => container.copy(ports = container.ports ++ ports)
    }
  )
}

case class Container(
    name: String,
    image: String,
    ports: List[Port],
    imagePullPolicy: ImagePullPolicy,
    livenessProbe: Probe,
    readinessProbe: Probe
)

// For more probes go https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/
sealed trait Probe
case class HttpProbe(httpGet: HttpGet, initialDelay: FiniteDuration, period: FiniteDuration, failureThreshold: Option[Short]) extends Probe
case object NoProbe extends Probe

case class HttpGet(path: String, port: Int, httpHeaders: List[Header])

case class Header(name: String, value: String)

case class Port(name: Option[String], containerPort: Int)

sealed trait DeploymentStrategy

case class RollingUpdate(maxSurge: Int = 0, maxUnavailable: Int = 1) extends DeploymentStrategy


sealed trait ImagePullPolicy

case object Always extends ImagePullPolicy
case object IfNotPresent extends ImagePullPolicy

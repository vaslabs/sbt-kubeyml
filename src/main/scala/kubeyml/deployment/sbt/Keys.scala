package kubeyml.deployment.sbt
import kubeyml.deployment.{Port, Probe}
import sbt._

trait Keys {
  val dockerImage = settingKey[String]("The docker image to deploy")
  val namespace = settingKey[String]("The namespace that the application will be deployed to")
  val application = settingKey[String]("The application name")

  val libenessProbe = settingKey[Probe]("The liveness probe for healthchecks")
  val readinessProbe = settingKey[Probe]("The readiness probe to get the application to receive traffic")

  val ports = settingKey[List[Port]]("A set of port numbers and names to be exposed to a kube service")
  val replicas = settingKey[Int]("The number of replicas of the application")
  val annotations = settingKey[Map[String, String]]("Wildcard for setting annotations on the deployment spec")

  val gen = taskKey[Unit]("Generates a kubernetes yml file for deployment")

  val kube = Configuration.of("KubeDeployment", "kubeyml")
}

object Keys extends Keys {
  import kubeyml.deployment.api._
  lazy val kubeymlSettings: Seq[Def.Setting[_]] = Seq(
    gen in kube := {
      Plugin.generate(
        deploy.namespace(
          (namespace in kube).value
        ).service(
          (application in kube).value
        ).withImage(
          (dockerImage in kube).value
        ).withProbes(
          (libenessProbe in kube).value,
          (readinessProbe in kube).value
        ).addContainerPorts((ports in kube).value)
          .annotateSpecTemplate((annotations in kube).value)
          .replicas((replicas in kube).value)
      )
    }
  )
}
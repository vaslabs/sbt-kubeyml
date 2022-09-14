/*
 * Copyright (c) 2019 Vasilis Nicolaou
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package kubeyml.deployment.plugin

import kubeyml.deployment._
import kubeyml.deployment.api._
import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._
import kubeyml.protocol.NonEmptyString

trait Keys {
  val dockerImage = settingKey[String]("The docker image to deploy")
  val namespace = settingKey[String]("The namespace that the application will be deployed to")
  val application = settingKey[String]("The application name")

  val livenessProbe = settingKey[Probe]("The liveness probe for healthchecks")
  val readinessProbe = settingKey[Probe]("The readiness probe to get the application to receive traffic")

  val ports = settingKey[List[Port]]("A set of port numbers and names to be exposed to a kube service")
  val replicas = settingKey[Int]("The number of replicas of the application")
  val annotations = settingKey[Map[String, String]]("Wildcard for setting annotations on the deployment spec")

  val resourceLimits = settingKey[Resource]("Cpu and memory limit for the container")
  val resourceRequests = settingKey[Resource]("Cpu and memory request, must not exceed limits")

  val envs = settingKey[Map[EnvName, EnvValue]]("Environment variables for the container")
  val command = settingKey[Option[NonEmptyString]]("Command for the container")
  val args = settingKey[Seq[String]]("arguments for the container")

  val persistentVolumes = settingKey[Seq[VolumeWithClaim]]("Persistent volume for the container")

  val imagePullPolicy = settingKey[ImagePullPolicy]("Pull policy of docker image")
  val deployment = settingKey[Deployment]("The kubernetes deployment description")

  val gen = taskKey[Unit]("Generates a kubernetes yml file for deployment")

  val kube = Configuration.of("KubeDeployment", "kubeyml")

}

object Keys extends Keys {

  lazy val deploymentSettings: Seq[Def.Setting[_]] = Seq(
    (kube / target) := (ThisProject / target).value / "kubeyml",
    kube / gen :=
      Plugin.generate(
        (kube / deployment).value,
        (kube / target).value
      ),
    namespace := (ThisProject / name).value,
    application := (ThisProject / name).value,
    dockerImage := (dockerAlias).value.toString(),
    ports := dockerExposedPorts.value.toList.map(Port(None, _)),
    livenessProbe := HttpProbe(
      HttpGet("/health", 8080, List.empty)
    ),
    readinessProbe := (kube / livenessProbe).value,
    annotations := Map.empty,
    replicas := 2,
    envs := Map.empty,
    persistentVolumes := Seq.empty,
    resourceRequests := Resources().requests,
    resourceLimits := Resources().limits,
    command := None,
    args := Seq.empty,
    imagePullPolicy := IfNotPresent,
    deployment :=
      deploy
        .namespace(kubeSetting(namespace).value)
        .service(kubeSetting(application).value)
        .withImage(kubeSetting(dockerImage).value)
        .withProbes(
          kubeSetting(livenessProbe).value,
          kubeSetting(readinessProbe).value
        )
        .addContainerPorts(kubeSetting(ports).value)
        .annotateSpecTemplate(kubeSetting(annotations).value)
        .addCommand(kubeSetting(command).value, kubeSetting(args).value)
        .replicas(kubeSetting(replicas).value)
        .addEnv(kubeSetting(envs).value)
        .addPersistentVolumes(kubeSetting(persistentVolumes).value)
        .resources(kubeSetting(resourceLimits).value, kubeSetting(resourceRequests).value)
        .pullPolicy(kubeSetting(imagePullPolicy).value)
  )

  private def kubeSetting[A](setting: SettingKey[A]): SettingKey[A] = (kube / setting)
}

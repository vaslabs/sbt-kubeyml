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

package kubeyml.deployment.sbt
import kubeyml.deployment.{EnvName, EnvValue, HttpGet, HttpProbe, Port, Probe, Resource, Resources}
import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._

import scala.concurrent.duration._

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
          (livenessProbe in kube).value,
          (readinessProbe in kube).value
        ).addContainerPorts((ports in kube).value)
          .annotateSpecTemplate((annotations in kube).value)
          .replicas((replicas in kube).value)
          .addEnv((envs in kube).value)
          .requestResource((resourceRequests in kube).value)
          .limit((resourceLimits in kube).value),
        (target in ThisProject).value
      )
    },
    namespace := (name in ThisProject).value,
    application := (name in ThisProject).value,
    dockerImage := (dockerAlias).value.toString(),
    ports := dockerExposedPorts.value.toList.map(Port(None, _)),
    livenessProbe := HttpProbe(
      HttpGet("/health", 8080, List.empty), 5 seconds, 5 seconds, None
    ),
    readinessProbe := (livenessProbe in kube).value,
    annotations := Map.empty,
    replicas := 2,
    envs := Map.empty,
    resourceRequests := Resources().requests,
    resourceLimits := Resources().limits
  )
}
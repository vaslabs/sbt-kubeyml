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

package kubeyml.roles.akka.cluster.plugin

import kubeyml.deployment.{EnvFieldValue, EnvName, EnvRawValue, HttpGet, HttpProbe}
import kubeyml.roles.{Role, RoleBinding, RoleBindingMetadata, RoleMetadata, RoleRef}
import sbt._
import kubeyml.deployment.plugin.Keys.{envs, gen, kube, livenessProbe, readinessProbe}
import kubeyml.deployment.plugin.{Keys => DeploymentKeys}
import kubeyml.deployment.api._
import sbt.Keys.target

import scala.concurrent.duration._

trait Keys {
  val discoveryMethodEnv =
    settingKey[Option[EnvName]]("The name of the discovery method environment variable")
  val hostnameEnv =
    settingKey[Option[EnvName]]("The name of the hostname environment variable")
  val namespaceEnv = settingKey[Option[EnvName]]("The name of the environment variable for the namespace")
}

object Keys extends Keys {
  lazy val akkaClusterSettings: Seq[Def.Setting[_]] = Seq(
    discoveryMethodEnv in kube := None,
    hostnameEnv in kube := None,
    namespaceEnv in kube := None,
    livenessProbe in kube := HttpProbe(HttpGet("/alive", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds),
    readinessProbe in kube := HttpProbe(HttpGet("/ready", 8558, List.empty), 10 seconds, 3 seconds, 5 seconds),
    (envs in kube) ++=
      Map(List(
          (discoveryMethodEnv in kube).value.map(_ -> EnvRawValue("kubernetes-api")),
          (hostnameEnv in kube).value.map(_ -> EnvFieldValue("status.podIP")),
          (namespaceEnv in kube).value.map(_ -> EnvFieldValue("metadata.namespace"))
        ).flatten: _*) ++ Map(EnvName("AKKA_CLUSTER_BOOTSTRAP_SERVICE_NAME") -> EnvFieldValue("metadata.labels['app']")),
    gen in kube := {
      (gen in kube).value
      val namespace = (DeploymentKeys.namespace in kube).value
      val role = Role(
        RoleMetadata(
          "pod-reader",
          namespace
        ),
        Essential.rules
      )
      Plugin.generate(
        role,
        RoleBinding(
          RoleBindingMetadata("read-pods", namespace),
          Essential.subjects(namespace),
          RoleRef(role)
        ),
        (target in kube).value
      )
    }
  )
}
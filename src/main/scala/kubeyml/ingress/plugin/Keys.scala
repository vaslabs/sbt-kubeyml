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

package kubeyml.ingress.plugin

import kubeyml.ingress._
import kubeyml.deployment.api._
import kubeyml.service.plugin.{Keys => ServiceKeys}
import kubeyml.deployment.plugin.{Keys => DeploymentKeys}
import kubeyml.protocol.NonEmptyString
import sbt._
import sbt.Keys._

trait Keys {

  val ingress = settingKey[Ingress]("Kubernetes ingress definition")

  val ingressRules = settingKey[List[Rule]]("Mapping rules from an ingress to a service")

  val name = settingKey[String]("The name of the ingress")

  val annotations = settingKey[Map[NonEmptyString, String]]("Set annotations for the ingress metadata")

  val kube = Configuration.of("KubeDeployment", "kubeyml")

  val gen = taskKey[Unit]("Generates a kubernetes yml file for ingress")

}

object Keys extends Keys {
  lazy val kubeymlSettings: Seq[Def.Setting[_]] = Seq(
    gen in kube := Plugin.generate(
      (DeploymentKeys.deployment in kube).value,
      (ServiceKeys.service in kube).value,
      (Keys.ingress in kube).value,
      (target in ThisProject).value,
      (streams.value.log)
    ),

    ingress in kube := CustomIngress(
      (DeploymentKeys.application in kube).value,
      (DeploymentKeys.namespace in kube).value,
      (annotations in kube).value,
      Spec((ingressRules in kube).value)
    )
  )
}


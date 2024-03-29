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
import kubeyml.roles.json_support._

import kubeyml.deployment.plugin.Keys.kube
import kubeyml.deployment.plugin.KubeDeploymentPlugin
import kubeyml.roles.{Role, RoleBinding}

import sbt.AutoPlugin

import java.io.File

object AkkaClusterPlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires = KubeDeploymentPlugin
  override val projectSettings = sbt.inConfig(kube)(Keys.akkaClusterSettings)
}

object Plugin {
  def generate(role: Role, roleBinding: RoleBinding, target: File): Unit =
    kubeyml.plugin.writePlansInSingle(role, roleBinding, target, "roles")
}

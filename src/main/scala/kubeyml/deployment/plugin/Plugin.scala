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

import java.io.{File, PrintWriter}

import kubeyml.deployment.Deployment
import kubeyml.deployment.json_support._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.syntax._
import sbt.AutoPlugin

object Plugin {
  def generate(deployment: Deployment, buildTarget: File): Unit = {
    val genTarget = new File(buildTarget, "kubeyml")
    genTarget.mkdirs()
    val file = new File(genTarget, "deployment.yml")
    val printWriter = new PrintWriter(file)
    try {
      printWriter.println(deployment.asJson.asYaml.spaces4)
    } finally {
      printWriter.close()
    }
  }
}

object KubeDeploymentPlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires = sbt.plugins.JvmPlugin

  override val projectSettings = sbt.inConfig(Keys.kube)(Keys.kubeymlSettings)

}
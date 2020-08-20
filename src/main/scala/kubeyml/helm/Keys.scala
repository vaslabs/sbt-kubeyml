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

package kubeyml.helm

import kubeyml.deployment.plugin.Keys.application
import kubeyml.deployment.plugin.Keys.{gen, kube}
import sbt.Def
import sbt._
import sbt.Keys._
import kubeyml.protocol.NonEmptyString

object Keys {

  lazy val helmSettings: Seq[Def.Setting[_]] = Seq(
    (target in kube) := (target in kube).value / "templates",
      gen in kube := {
      (gen in kube).value
      val chartTarget = (target in kube).value
      val chart = Chart(
        NonEmptyString((version in ThisBuild).value),
        NonEmptyString((application in kube).value)
      )
      Plugin.generate(chart, chartTarget.getParentFile)
    }
  )
}

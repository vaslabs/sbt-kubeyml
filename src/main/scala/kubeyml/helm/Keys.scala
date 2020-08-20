package kubeyml.helm

import kubeyml.deployment.plugin.Keys.application
import kubeyml.deployment.plugin.Keys.{gen, kube}
import sbt.Def
import sbt._
import sbt.Keys._
import kubeyml.protocol.NonEmptyString

object Keys {

  lazy val helmSettings: Seq[Def.Setting[_]] = Seq(
    gen in kube := {
      val chartTarget = (target in kube).value
      val chart = Chart(
        NonEmptyString((version in ThisBuild).value),
        NonEmptyString((application in kube).value)
      )
      (target in kube) := (target in kube).value / "templates"
      (gen in kube).value
      Plugin.generate(chart, chartTarget)
    }
  )
}

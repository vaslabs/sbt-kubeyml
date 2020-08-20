package kubeyml.helm

import java.io.File

import sbt.AutoPlugin
import kubeyml.deployment.plugin.Keys.kube

import json_support._

object HelmPlugin extends AutoPlugin {
  override def trigger = noTrigger

  override val projectSettings = sbt.inConfig(kube)(Keys.helmSettings)
}

object Plugin {

  def generate(chart: Chart, file: File) =
    kubeyml.plugin.writePlan(chart, file, "Chart")
}
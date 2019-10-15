package kubeyml.deployment.sbt

import java.io.{File, PrintWriter}

import kubeyml.deployment.Deployment
import kubeyml.deployment.json_support._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.yaml.syntax._

object Plugin {
  def generate(deployment: Deployment, buildTarget: File): Unit = {
    val genTarget = new File(buildTarget, "kubeyml")
    genTarget.mkdir()
    val file = new File(genTarget, "deployment.yml")
    val printWriter = new PrintWriter(file)
    try {
      printWriter.println(deployment.asJson.asYaml.spaces4)
    } finally {
      printWriter.close()
    }
  }
}

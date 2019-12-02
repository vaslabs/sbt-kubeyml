package kubeyml.service

import kubeyml.protocol.NonEmptyString
import org.scalatest.{FlatSpec, Matchers}

class JsonSpec extends FlatSpec with Matchers{



}


object JsonSpec {

  case class PortVariable(
      name: NonEmptyString,
      protocol: NetworkProtocol,
      port: Int,
      targetPort: Int
  )

  case class ServiceVariables(
    name: String,
    namespace: String,
    `type`: ServiceType,
    appSelector: String,
    ports: List[PortVariable]
  )

  def generateYaml()


}
package kubeyml.service

import kubeyml.protocol.{NonEmptyString, PortNumber}


case class Service(
    name: NonEmptyString,
    namespace: NonEmptyString
)

case class Spec(
  `type`: ServiceType,
  selector: Selector,

)


sealed trait ServiceType

case object NodePort extends ServiceType

sealed trait Selector
case class AppSelector(appName: NonEmptyString) extends Selector


case class Port(
     name: NonEmptyString,
     protocol: NetworkProtocol,
     port: PortNumber,
     targetPort: TargetPort
)

sealed trait NetworkProtocol
case object TCP extends NetworkProtocol


sealed trait TargetPort
case class NamedTargetPort(name: NonEmptyString)
case class NumberedTargetPort(value: PortNumber)
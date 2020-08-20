package kubeyml.helm

import kubeyml.protocol.NonEmptyString

case class Chart(
  version: NonEmptyString,
  name: NonEmptyString
)
package kubeyml.protocol

case class NonEmptyString(value: String) {
  require(value.nonEmpty)
}
case class PortNumber(value: Int) {
  require(value >= 0 && value <= 65535)
}
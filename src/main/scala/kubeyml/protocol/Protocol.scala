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

package kubeyml.protocol

import java.net.InetAddress
import java.nio.ByteBuffer

case class NonEmptyString(value: String) {
  require(value.nonEmpty, "Empty strings are not allowed")
}

case class PortNumber(value: Int) {
  require(value >= 0 && value <= 65535, "Out of port range [0,65535]")
}

case class IPv4 private (value: Int) {
  val show: String =
    InetAddress.getByAddress(
      ByteBuffer.allocate(4)
        .putInt(value).array()
    ).getHostAddress
}

object IPv4 {
  def apply(str: String): IPv4 =
    apply(
      ByteBuffer.wrap(InetAddress.getByName(str).getAddress).getInt
    )

  private def apply(value: Int): IPv4 = new IPv4(value)
}


case class Host(value: String) {
  private val validationRegex = "[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*"
  private def errorMessage = s""""
    Invalid value: ${value}:
    a DNS-1123 subdomain must consist of lower case alphanumeric characters, '-' or '.',
    and must start and end with an alphanumeric character
    (e.g. 'example.com', regex used for validation is
    '${validationRegex}')
  """
  require(value.nonEmpty, "Hostname cannot be empty")
  require(value.matches(s"${validationRegex}"), errorMessage)
}
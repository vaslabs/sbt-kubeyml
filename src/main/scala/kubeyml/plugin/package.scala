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

package kubeyml

import java.io.{File, PrintWriter}

import io.circe.Encoder
import io.circe.syntax._
import io.circe.yaml.syntax._

package object plugin {

  private[kubeyml] def writePlan[A](a: A, buildTarget: File, kind: String)(implicit encoder: Encoder[A]) = {
    val genTarget = new File(buildTarget, "kubeyml")
    genTarget.mkdirs()
    val file = new File(genTarget, s"${kind}.yml")
    val printWriter = new PrintWriter(file)
    try {
      printWriter.println(a.asJson.asYaml.spaces4)
    } finally {
      printWriter.close()
    }
  }

  private[kubeyml] def writePlansInSingle[A, B](a: A, b: B, buildTarget: File, kind: String)(implicit
                                              encoderA: Encoder[A], encoder: Encoder[B]
  ) = {
    val genTarget = new File(buildTarget, "kubeyml")
    genTarget.mkdirs()
    val file = new File(genTarget, s"${kind}.yml")
    val printWriter = new PrintWriter(file)
    try {
      printWriter.println(a.asJson.asYaml.spaces4)
      printWriter.println("---")
      printWriter.println(b.asJson.asYaml.spaces4)
    } finally {
      printWriter.close()
    }
  }
}

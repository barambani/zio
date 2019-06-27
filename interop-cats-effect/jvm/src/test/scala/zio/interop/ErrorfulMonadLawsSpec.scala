/*
 * Copyright 2017-2019 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio
package interop

import cats.instances.int._
import cats.instances.tuple._
import cats.laws.discipline.MonadTests
import cats.{ Eq, Invariant, Monad }
import org.scalacheck.Arbitrary.arbInt
import org.scalacheck.Cogen.cogenInt
import org.scalacheck.{ Arbitrary, Cogen }
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline
import zio.interop.bio.Errorful2
import zio.interop.runtime.TestRuntime

final class ErrorfulMonadLawsSpec extends FunSuite with TestRuntime with Discipline with GenIO {

  {
    import default.testZioInstances._

    val ev: Monad[IO[String, ?]] = Errorful2[IO].monad[String]

    checkAll(
      "Errorful2's Monad[IO[String, ?]]",
      MonadTests[IO[String, ?]](ev).monad[Int, Int, Int]
    )

    implicit def zioInvariant[R, E]: Invariant[ZIO[R, E, ?]] =
      new Invariant[ZIO[R, E, ?]] {
        def imap[A, B](fa: ZIO[R, E, A])(f: A => B)(g: B => A): ZIO[R, E, B] =
          fa map f
      }

    implicit def ioArbitrary[E, A: Arbitrary: Cogen]: Arbitrary[IO[E, A]] =
      Arbitrary(genSuccess[E, A])

    implicit def catsEQ[E, A: Eq]: Eq[IO[E, A]] =
      new Eq[IO[E, A]] {
        import zio.duration._

        def eqv(io1: IO[E, A], io2: IO[E, A]): Boolean = {
          val v1  = testRuntime.unsafeRunSync(io1.timeout(20.seconds)) map (_.get)
          val v2  = testRuntime.unsafeRunSync(io2.timeout(20.seconds)) map (_.get)
          val res = v1 === v2
          if (!res) {
            println(s"Mismatch: $v1 != $v2")
          }
          res
        }
      }
  }
}
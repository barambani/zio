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

package scalaz.zio
package interop
package bio

import cats.Monad

abstract class Errorful2[F[+ _, + _]] extends Guaranteed2[F] {

  def monad[E]: Monad[F[E, ?]]

  /**
   * Returns an effect `F` that will fail with an error of type `E`.
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  def raiseError[E](e: E): F[E, Nothing]

  /**
   * Allows to recover from the error, accepting effects that handle both
   * the failure and the success case. The handling effects can still fail
   * themselves with an error of type `E2`.
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   * @see [[redeemPure]] to recover from the error in a non failing way.
   *
   */
  @inline def redeem[E1, E2, A, B](fa: F[E1, A])(failure: E1 => F[E2, B], success: A => F[E2, B]): F[E2, B]

  /**
   * Allows to recover from the error in a non failing way.
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def redeemPure[E1, A, B](fa: F[E1, A])(failure: E1 => B, success: A => B): F[Nothing, B] =
    redeem(fa)(
      failure andThen monad.pure,
      success andThen monad.pure
    )

  /**
   * Allows to specify an alternative in case the original effect fail.
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def orElse[E1, E2, A, AA >: A](fa: F[E1, A])(that: => F[E2, AA]): F[E2, AA] =
    redeem(fa)(_ => that, monad.pure)

  /**
   * Allows to surface a possible failure of the effect and make it explicit
   * in the result type as an `Either`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   * * @see [[absolve]] to submerge the error in `F`
   *
   */
  @inline def either[E, A](fa: F[E, A]): F[Nothing, Either[E, A]] =
    redeemPure(fa)(Left(_), Right(_))

  /**
   * Inverse of [[either]] submerges the `Either`'s error in `F`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def absolve[E, A](fa: F[Nothing, Either[E, A]]): F[E, A] =
    monad.flatMap(fa)(
      _.fold(raiseError, monad.pure)
    )

  /**
   * Recovers from the error if the effect fails, and forwards the result
   * in case of success
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def catchAll[E1, E2, A, AA >: A](fa: F[E1, A])(f: E1 => F[E2, AA]): F[E2, AA] =
    redeem(fa)(f, monad.pure)

  /**
   * Recovers from some or all the errors depending on `pf`'s domain
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def catchSome[E, EE >: E, A, AA >: A](fa: F[E, A])(pf: PartialFunction[E, F[EE, AA]]): F[EE, AA] =
    redeem(fa)(
      pf.applyOrElse(_, raiseError[EE]),
      monad.pure
    )

  /**
   * Verifies a predicate and returns a failing effect if false
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def ensure[E, A](fa: F[E, A])(predicate: A => Boolean)(ifFalse: A => E): F[E, A] =
    monad.flatMap(fa) { a =>
      if (predicate(a)) monad.pure(a) else raiseError(ifFalse(a))
    }

  /**
   * Flips the types of `F`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def flip[E, A](fa: F[E, A]): F[A, E] =
    redeem(fa)(monad.pure, raiseError)

  /**
   * Lifts an `Option` into `F`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def fromOption[A](oa: Option[A]): F[Unit, A] =
    fromOption(())(oa)

  /**
   * Lifts an `Option` into `F` provided the error if `None`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def fromOption[E, A](ifNone: => E)(oa: Option[A]): F[E, A] =
    oa match {
      case Some(a) => monad.pure(a)
      case None    => raiseError(ifNone)
    }

  /**
   * Lifts an `Either` into `F`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def fromEither[E, A](ea: Either[E, A]): F[E, A] =
    ea match {
      case Left(e)  => raiseError(e)
      case Right(a) => monad.pure(a)
    }

  /**
   * Lifts a `Try` into `F`
   *
   * TODO: Example:
   * {{{
   *
   * }}}
   *
   */
  @inline def fromTry[A](ta: scala.util.Try[A]): F[Throwable, A] =
    ta match {
      case scala.util.Failure(th) => raiseError(th)
      case scala.util.Success(a)  => monad.pure(a)
    }

  override def bimap[A, B, C, D](fab: F[A, B])(f: A => C, g: B => D): F[C, D] =
    redeem(fab)(
      f andThen raiseError,
      g andThen monad.pure
    )
}

object Errorful2 {

  @inline def apply[F[+ _, + _]: Errorful2]: Errorful2[F] = implicitly
}

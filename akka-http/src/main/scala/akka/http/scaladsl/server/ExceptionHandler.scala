/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package akka.http.scaladsl.server

import scala.util.control.NonFatal
import akka.http.scaladsl.settings.RoutingSettings
import akka.http.scaladsl.model._
import StatusCodes._

trait ExceptionHandler extends ExceptionHandler.PF {

  /**
   * Creates a new [[ExceptionHandler]] which uses the given one as fallback for this one.
   */
  def withFallback(that: ExceptionHandler): ExceptionHandler

  /**
   * "Seals" this handler by attaching a default handler as fallback if necessary.
   */
  def seal(settings: RoutingSettings): ExceptionHandler
}

object ExceptionHandler {
  type PF = PartialFunction[Throwable, Route]
  private[http] val ErrorMessageTemplate: String = {
    "Error during processing of request: '{}'. Completing with {} response. " +
      "To change default exception handling behavior, provide a custom ExceptionHandler."
  }

  implicit def apply(pf: PF): ExceptionHandler = apply(knownToBeSealed = false)(pf)

  private def apply(knownToBeSealed: Boolean)(pf: PF): ExceptionHandler =
    new ExceptionHandler {
      def isDefinedAt(error: Throwable) = pf.isDefinedAt(error)
      def apply(error: Throwable) = pf(error)
      def withFallback(that: ExceptionHandler): ExceptionHandler =
        if (!knownToBeSealed) ExceptionHandler(knownToBeSealed = false)(this orElse that) else this
      def seal(settings: RoutingSettings): ExceptionHandler =
        if (!knownToBeSealed) ExceptionHandler(knownToBeSealed = true)(this orElse default(settings)) else this
    }

  /**
   * Default [[ExceptionHandler]] that discards the request's entity by default.
   */
  def default(settings: RoutingSettings): ExceptionHandler =
    apply(knownToBeSealed = true) {
      case IllegalRequestException(info, status) => ctx => {
        ctx.log.warning("Illegal request: '{}'. Completing with {} response.", info.summary, status)
        ctx.request.discardEntityBytes(ctx.materializer)
        ctx.complete((status, info.format(settings.verboseErrorMessages)))
      }
      case e: EntityStreamSizeException => ctx => {
        ctx.log.error(e, ErrorMessageTemplate, e, ContentTooLarge)
        ctx.request.discardEntityBytes(ctx.materializer)
        ctx.complete((ContentTooLarge, e.getMessage))
      }
      case e: ExceptionWithErrorInfo => ctx => {
        ctx.log.error(e, ErrorMessageTemplate, e.info.formatPretty, InternalServerError)
        ctx.request.discardEntityBytes(ctx.materializer)
        ctx.complete((InternalServerError, e.info.format(settings.verboseErrorMessages)))
      }
      case NonFatal(e) => ctx => {
        val message = Option(e.getMessage).getOrElse(s"${e.getClass.getName} (No error message supplied)")
        ctx.log.error(e, ErrorMessageTemplate, message, InternalServerError)
        ctx.request.discardEntityBytes(ctx.materializer)
        ctx.complete(InternalServerError)
      }
    }

  /**
   * Creates a sealed ExceptionHandler from the given one. Returns the default handler if the given one
   * is `null`.
   */
  def seal(handler: ExceptionHandler)(implicit settings: RoutingSettings): ExceptionHandler =
    if (handler ne null) handler.seal(settings) else ExceptionHandler.default(settings)
}

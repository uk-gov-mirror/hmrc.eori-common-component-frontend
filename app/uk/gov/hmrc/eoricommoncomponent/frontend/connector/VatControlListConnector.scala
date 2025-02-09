/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.eoricommoncomponent.frontend.connector

import javax.inject.{Inject, Singleton}
import play.api.Logger
import uk.gov.hmrc.eoricommoncomponent.frontend.config.AppConfig
import uk.gov.hmrc.eoricommoncomponent.frontend.domain._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatControlListConnector @Inject() (http: HttpClient, appConfig: AppConfig)(implicit ec: ExecutionContext) {

  private val logger = Logger(this.getClass)
  private val url    = appConfig.getServiceUrl("vat-known-facts-control-list")

  def vatControlList(
    request: VatControlListRequest
  )(implicit hc: HeaderCarrier): Future[Either[EoriHttpResponse, VatControlListResponse]] =
    http.GET[VatControlListResponse](url, request.queryParams) map { resp =>
      // $COVERAGE-OFF$Loggers
      logger.debug(s"vat-known-facts-control-list successful. url: $url")
      // $COVERAGE-ON
      Right(resp)
    } recover {
      case _: NotFoundException =>
        logFailed _
        Left(NotFoundResponse)
      case _: BadRequestException =>
        logFailed _
        Left(InvalidResponse)
      case _: ServiceUnavailableException =>
        logFailed _
        Left(ServiceUnavailableResponse)
      case e: Throwable =>
        logFailed(e)
        throw e
    }

  def logFailed(e: Throwable) = logger.warn(s"VatControlList failed. url: $url, error: $e", e)

}

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

package uk.gov.hmrc.eoricommoncomponent.frontend.controllers.email

import javax.inject.{Inject, Singleton}
import play.api.mvc._
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.auth.AuthAction
import uk.gov.hmrc.eoricommoncomponent.frontend.controllers.CdsController
import uk.gov.hmrc.eoricommoncomponent.frontend.domain.{InternalId, LoggedInUserWithEnrolments}
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.EmailForm.emailForm
import uk.gov.hmrc.eoricommoncomponent.frontend.forms.models.email.{EmailStatus, EmailViewModel}
import uk.gov.hmrc.eoricommoncomponent.frontend.models.{Journey, Service}
import uk.gov.hmrc.eoricommoncomponent.frontend.services.Save4LaterService
import uk.gov.hmrc.eoricommoncomponent.frontend.views.html.email._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatIsYourEmailController @Inject() (
  authAction: AuthAction,
  mcc: MessagesControllerComponents,
  whatIsYourEmailView: what_is_your_email,
  save4LaterService: Save4LaterService
)(implicit ec: ExecutionContext)
    extends CdsController(mcc) {

  private def populateView(email: Option[String], service: Service, journey: Journey.Value)(implicit
    request: Request[AnyContent]
  ): Future[Result] = {
    lazy val form = email.map(EmailViewModel).fold(emailForm) {
      emailForm.fill
    }
    Future.successful(Ok(whatIsYourEmailView(emailForm = form, service, journey)))
  }

  def createForm(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction { implicit request => _: LoggedInUserWithEnrolments =>
      populateView(None, service, journey)
    }

  def submit(service: Service, journey: Journey.Value): Action[AnyContent] =
    authAction.ggAuthorisedUserWithEnrolmentsAction {
      implicit request => userWithEnrolments: LoggedInUserWithEnrolments =>
        emailForm.bindFromRequest.fold(
          formWithErrors =>
            Future.successful(BadRequest(whatIsYourEmailView(emailForm = formWithErrors, service, journey))),
          formData => submitNewDetails(InternalId(userWithEnrolments.internalId), formData, service, journey)
        )
    }

  private def submitNewDetails(
    internalId: InternalId,
    formData: EmailViewModel,
    service: Service,
    journey: Journey.Value
  )(implicit hc: HeaderCarrier): Future[Result] =
    save4LaterService
      .saveEmail(internalId, EmailStatus(Some(formData.email)))
      .flatMap(_ => Future.successful(Redirect(routes.CheckYourEmailController.createForm(service, journey))))

}

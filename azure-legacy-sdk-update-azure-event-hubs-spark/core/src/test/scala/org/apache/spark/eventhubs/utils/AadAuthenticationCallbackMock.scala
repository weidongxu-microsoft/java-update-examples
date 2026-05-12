package org.apache.spark.eventhubs.utils

import java.time.OffsetDateTime

import com.azure.core.credential.{ AccessToken, TokenRequestContext }
import reactor.core.publisher.Mono

class AadAuthenticationCallbackMock extends AadAuthenticationCallback {
  override def getToken(request: TokenRequestContext): Mono[AccessToken] =
    Mono.just(new AccessToken("fake-token", OffsetDateTime.now().plusHours(1)))

  override def authority: String = "Fake-tenant-id"
}

class AadAuthenticationCallbackMockWithParams(params: Map[String, Object])
    extends AadAuthenticationCallback {
  override def getToken(request: TokenRequestContext): Mono[AccessToken] =
    Mono.just(new AccessToken("fake-token", OffsetDateTime.now().plusHours(1)))

  override def authority: String = params("authority").asInstanceOf[String]
}

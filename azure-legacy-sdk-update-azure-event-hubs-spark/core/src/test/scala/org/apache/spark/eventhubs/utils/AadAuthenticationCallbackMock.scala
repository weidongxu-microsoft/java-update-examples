package org.apache.spark.eventhubs.utils

class AadAuthenticationCallbackMock extends AadAuthenticationCallback {
  override def authority: String = "Fake-tenant-id"
}

class AadAuthenticationCallbackMockWithParams(params: Map[String, Object])
    extends AadAuthenticationCallback {
  override def authority: String = params("authority").asInstanceOf[String]
}

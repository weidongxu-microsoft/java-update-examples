package org.apache.spark.eventhubs.utils

import com.azure.core.credential.TokenCredential

// NOTE: Modern Azure SDK uses TokenCredential instead of AuthenticationCallback.
// Implementations should extend TokenCredential or use DefaultAzureCredential/ClientSecretCredential
trait AadAuthenticationCallback extends TokenCredential with Serializable {
  def authority: String
}

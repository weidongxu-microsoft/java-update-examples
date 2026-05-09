package org.apache.spark.eventhubs.utils

// NOTE: Track 2 uses TokenCredential from azure-identity instead of AzureActiveDirectoryTokenProvider.AuthenticationCallback
// This trait needs to be refactored during Phase 2.2 to support modern authentication patterns
// For now, keeping the interface for compatibility but removing the Track 1 dependency
trait AadAuthenticationCallback extends Serializable {
  def authority: String
}

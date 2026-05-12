package com.azure.messaging.eventhubs

import java.time.{ Duration, Instant }
import java.util.concurrent.{ CompletableFuture, ScheduledExecutorService }

import com.azure.messaging.eventhubs.models.EventPosition
import org.apache.spark.eventhubs.utils.AadAuthenticationCallback

class ReceiverOptions {
  def setPrefetchCount(value: Int): ReceiverOptions = this
  def setIdentifier(value: String): ReceiverOptions = this
  def setReceiverRuntimeMetricEnabled(value: Boolean): ReceiverOptions = this
}

class RetryPolicy

object RetryPolicy {
  private val defaultPolicy = new RetryPolicy
  def getDefault: RetryPolicy = defaultPolicy
}

class EventHubClientOptions {
  def setMaximumSilentTime(value: Duration): EventHubClientOptions = this
  def setOperationTimeout(value: Duration): EventHubClientOptions = this
  def setRetryPolicy(value: RetryPolicy): EventHubClientOptions = this
}

class LegacyEventPosition(private val sequenceNumber: java.lang.Long = null) {
  def getSequenceNumber: java.lang.Long = sequenceNumber
}

class PartitionRuntimeInformation(
    private val partitionId: String,
    private val eventHubPath: String,
    private val beginSequenceNumber: Long,
    private val lastEnqueuedSequenceNumber: Long,
    private val lastEnqueuedTimeUtc: Instant,
    private val isEmpty: Boolean) {

  def getPartitionId: String = partitionId
  def getEventHubPath: String = eventHubPath
  def getBeginSequenceNumber: Long = beginSequenceNumber
  def getLastEnqueuedSequenceNumber: Long = lastEnqueuedSequenceNumber
  def getLastEnqueuedTimeUtc: Instant = lastEnqueuedTimeUtc
  def getIsEmpty: Boolean = isEmpty
}

class EventHubRuntimeInformation(private val partitionCount: Int) {
  def getPartitionCount: Int = partitionCount
}

class PartitionSender(private val partitionId: String) {
  def getPartitionId: String = partitionId
  def send(event: EventData): CompletableFuture[Void] = CompletableFuture.completedFuture(null)
  def closeSync(): Unit = ()
}

class PartitionReceiver(private var eventPosition: LegacyEventPosition = new LegacyEventPosition()) {
  def setReceiveTimeout(timeout: Duration): Unit = ()
  def receive(maxEventCount: Int): CompletableFuture[java.lang.Iterable[EventData]] =
    CompletableFuture.completedFuture(java.util.Collections.emptyList[EventData]())
  def close(): CompletableFuture[Void] = CompletableFuture.completedFuture(null)
  def getEventPosition: LegacyEventPosition = eventPosition
  def getIsOpen: Boolean = true
}

class ReceiverDisconnectedException(message: String) extends RuntimeException(message)

class EventHubException(private val isTransientFlag: Boolean, message: String)
    extends RuntimeException(message) {
  def getIsTransient: Boolean = isTransientFlag
}

object EventHubClientImpl {
  @volatile var USER_AGENT: String = "SparkConnector"
}

class EventHubClient(private val eventHubName: String = "eventhub") {
  def getEventHubName: String = eventHubName

  def createPartitionSenderSync(partitionId: String): PartitionSender =
    new PartitionSender(partitionId)

  def send(event: EventData): CompletableFuture[Void] = CompletableFuture.completedFuture(null)

  def send(event: EventData, partitionKey: String): CompletableFuture[Void] =
    CompletableFuture.completedFuture(null)

  def createEpochReceiver(consumerGroup: String,
                          partitionId: String,
                          position: EventPosition,
                          epoch: Long,
                          receiverOptions: ReceiverOptions): CompletableFuture[PartitionReceiver] =
    CompletableFuture.completedFuture(new PartitionReceiver())

  def createReceiver(consumerGroup: String,
                     partitionId: String,
                     position: EventPosition,
                     receiverOptions: ReceiverOptions): CompletableFuture[PartitionReceiver] =
    CompletableFuture.completedFuture(new PartitionReceiver())

  def getPartitionRuntimeInformation(partitionId: String): CompletableFuture[PartitionRuntimeInformation] =
    CompletableFuture.completedFuture(
      new PartitionRuntimeInformation(partitionId,
                                      eventHubName,
                                      0L,
                                      0L,
                                      Instant.now(),
                                      isEmpty = true))

  def getRuntimeInformation: CompletableFuture[EventHubRuntimeInformation] =
    CompletableFuture.completedFuture(new EventHubRuntimeInformation(1))

  def close(): Unit = ()
}

object EventHubClient {
  def createWithAzureActiveDirectory(endpoint: java.net.URI,
                                     eventHubName: String,
                                     authCallback: AadAuthenticationCallback,
                                     authority: String,
                                     executor: ScheduledExecutorService,
                                     options: EventHubClientOptions): CompletableFuture[EventHubClient] =
    CompletableFuture.completedFuture(new EventHubClient(eventHubName))

  def createFromConnectionString(connectionString: String,
                                 retryPolicy: RetryPolicy,
                                 executor: ScheduledExecutorService,
                                 customEndpointAddress: String,
                                 maximumSilentTime: Duration): CompletableFuture[EventHubClient] = {
    val eventHubName = {
      val marker = "EntityPath="
      val idx = connectionString.indexOf(marker)
      if (idx >= 0) {
        val remaining = connectionString.substring(idx + marker.length)
        val delimiter = remaining.indexOf(';')
        if (delimiter >= 0) remaining.substring(0, delimiter) else remaining
      } else {
        "eventhub"
      }
    }
    CompletableFuture.completedFuture(new EventHubClient(eventHubName))
  }
}
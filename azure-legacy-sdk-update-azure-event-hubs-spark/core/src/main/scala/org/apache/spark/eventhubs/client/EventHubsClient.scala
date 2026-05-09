/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.eventhubs.client

import com.azure.messaging.eventhubs._
import com.azure.messaging.eventhubs.models.SendOptions
import org.apache.spark.SparkEnv
import org.apache.spark.eventhubs.EventHubsConf
import org.apache.spark.eventhubs.utils.RetryUtils._
import org.apache.spark.internal.Logging
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{ ArrayBuffer, ListBuffer }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.util.{ Failure, Success }

/**
 * A [[Client]] which connects to an event hub instance. All interaction
 * between Spark and Event Hubs will happen within this client.
 * Track 2: Uses EventHubsProducerClient and EventHubsConsumerClient.
 */
@SerialVersionUID(1L)
private[spark] class EventHubsClient(private val ehConf: EventHubsConf)
    extends Serializable
    with Client
    with Logging {

  import org.apache.spark.eventhubs._
  import EventHubsClient._

  ehConf.validate

  private implicit val formats = Serialization.formats(NoTypeHints)

  private var pendingWorks = new ListBuffer[Future[Any]]

  private var producerClient: EventHubProducerClient = _
  private var consumerClient: EventHubConsumerClient = _

  private var partitionCountCache: Int = 0
  private var partitionCountCacheUpdateTimestamp: Long = 0

  private def getProducerClient: EventHubProducerClient = synchronized {
    if (producerClient == null) {
      producerClient = ClientConnectionPool.getProducerClient(ehConf)
    }
    producerClient
  }

  private def getConsumerClient: EventHubConsumerClient = synchronized {
    if (consumerClient == null) {
      consumerClient = ClientConnectionPool.getConsumerClient(ehConf)
    }
    consumerClient
  }

  // Track 2: Batching for partition-specific sends
  private var partitionProducerBatch: EventDataBatch = _
  private var partitionId: Int = -1

  override def createPartitionSender(partition: Int): Unit = {
    synchronized {
      if (partitionId != partition || partitionProducerBatch == null) {
        logInfo(s"Creating batch for partition $partition")
        partitionId = partition
        // Create a batch with partition key corresponding to the partition ID
        // Note: In Track 2, we don't have partition-specific senders.
        // Instead, we use partition key for routing to a specific partition
      }
    }
  }

  override def send(event: EventData,
                    partition: Option[Rate] = None,
                    partitionKey: Option[String] = None,
                    properties: Option[Map[String, String]] = None): Unit = {
    if (properties.isDefined) {
      val p = event.getProperties
      p.putAll(properties.get.asJava)
    }

    val sendTask = Future {
      if (partition.isDefined) {
        if (partitionId != partition.get) {
          logInfo("Recreating batch for new partition.")
          createPartitionSender(partition.get)
        }
        val pKey = partition.get.toString
        getProducerClient.send(List(event).asJava, new SendOptions().setPartitionKey(pKey))
      } else if (partitionKey.isDefined) {
        getProducerClient.send(List(event).asJava, new SendOptions().setPartitionKey(partitionKey.get))
      } else {
        getProducerClient.send(List(event).asJava)
      }
    }

    pendingWorks += sendTask
  }

  /**
   * Retrieves partition properties for a specific partition.
   * Track 2: Uses PartitionProperties instead of PartitionRuntimeInformation.
   *
   * @param partitionId the partition to be queried.
   * @return Future with [[PartitionProperties]] for the partition
   */
  private def getPartitionPropertiesF(partitionId: PartitionId): Future[PartitionProperties] = {
    // In Track 2, we need to get properties from the producer client
    // The producer client can be used to get partition properties
    val future = Future {
      try {
        getProducerClient.getPartitionProperties(partitionId.toString)
      } catch {
        case e: Exception =>
          logError(s"Error getting partition properties for partition $partitionId", e)
          throw e
      }
    }
    future
  }

  /**
   * Same as boundedSeqNos, but for all partitions in the Event Hub.
   *
   * @return the earliest and latest sequence numbers for all partitions in the Event Hub
   */
  override def allBoundedSeqNos: Map[PartitionId, (SequenceNumber, SequenceNumber)] = {
    val futures = for (i <- 0 until partitionCount)
      yield
        getPartitionPropertiesF(i) map { props =>
          val earliest =
            if (props.getBeginningSequenceNumber == -1L) 0L
            else {
              if (props.isEmpty) props.getLastEnqueuedSequenceNumber + 1 else props.getBeginningSequenceNumber
            }
          val latest = props.getLastEnqueuedSequenceNumber + 1
          i -> ((earliest, latest): (Long, Long))
        }
    Await
      .result(Future.sequence(futures), ehConf.internalOperationTimeout)
      .toMap
  }

  /**
   * Provides a [[Future]] containing the earliest (lowest) sequence number
   * that exists in the EventHubs instance for the given partition.
   *
   * @param partition the partition that will be queried
   * @return A [[Future]] containing the earliest sequence number for the specified partition
   */
  private def earliestSeqNoF(partition: PartitionId): Future[SequenceNumber] = {
    getPartitionPropertiesF(partition).map { props =>
      val seqNo =
        if (props.isEmpty) props.getLastEnqueuedSequenceNumber + 1 else props.getBeginningSequenceNumber
      if (seqNo == -1L) 0L else seqNo
    }
  }

  /**
   * Provides a [[Future]] containing the latest (highest) sequence number that
   * exists in the EventHubs instance for the given partition.
   *
   * @param partition the partition that will be queried
   * @return a [[Future]] containing the latest sequence number for the specified partition
   */
  private def latestSeqNoF(partition: PartitionId): Future[SequenceNumber] =
    getPartitionPropertiesF(partition).map(_.getLastEnqueuedSequenceNumber + 1)

  /**
   * The number of partitions in the EventHubs instance.
   *
   * @return partition count
   */
  override def partitionCount: Int = {
    try {
      if (ehConf.dynamicPartitionDiscovery) {
        partitionCountDynamic
      } else {
        partitionCountLazyVal
      }
    } catch {
      case e: Exception => throw e
    }
  }

  lazy val partitionCountLazyVal: Int = {
    try {
      logDebug(
        s"partitionCountLazyVal makes a call to getPartitionIds to read the number of partitions")
      // Track 2: Get partition IDs from the producer client
      getProducerClient.getPartitionIds.stream().count().toInt
    } catch {
      case e: Exception => throw e
    }
  }

  def partitionCountDynamic: Int = {
    try {
      val currentTimeStamp = System.currentTimeMillis()
      if ((currentTimeStamp - partitionCountCacheUpdateTimestamp > UpdatePartitionCountIntervalMS) || (partitionCountCache == 0)) {
        // Track 2: Get partition count from producer client
        partitionCountCache = getProducerClient.getPartitionIds.stream().count().toInt
        partitionCountCacheUpdateTimestamp = currentTimeStamp
        logDebug(
          s"partitionCountDynamic made a call to getPartitionIds to read the number of partitions = ${partitionCountCache}" +
            s" at timestamp = ${partitionCountCacheUpdateTimestamp}")
      }
      partitionCountCache
    } catch {
      case e: Exception => throw e
    }
  }

  /**
   * Cleans up all open connections and links.
   *
   * Track 2: Producer and consumer clients are managed by [[ClientConnectionPool]].
   */
  override def close(): Unit = {
    logInfo(s"close is called. ${EventHubsUtils.getTaskContextSlim}")

    val future = Future.sequence(pendingWorks)
    future.onComplete {
      case Success(_) => cleanup()
      case Failure(e) =>
        logError(
          s"failed to complete pending tasks. event hubs: ${ehConf.name}, ${EventHubsUtils.getTaskContextSlim}",
          e)
        cleanup()

        throw e
    }

    Await.result(future, ehConf.internalOperationTimeout)
  }

  private def cleanup(): Unit = {
    pendingWorks.clear()

    // Track 2: Clients are released back to the pool but not closed here
    // The pool manages their lifecycle
    producerClient = null
    consumerClient = null
  }

  /**
   * Translates all [[EventPosition]]s provided in the [[EventHubsConf]] to
   * sequence numbers. Sequence numbers are zero-based indices. The 5th event
   * in an Event Hubs partition will have a sequence number of 4.
   *
   * This allows us to exclusively use sequence numbers to generate and manage
   * batches within Spark (rather than coding for many different filter types).
   *
   * @param ehConf         the [[EventHubsConf]] containing starting (or ending positions)
   * @param partitionCount the number of partitions in the Event Hub instance
   * @param useStart       translates starting positions when true and ending positions
   *                       when false
   * @return mapping of partitions to starting positions as sequence numbers
   */
  override def translate(ehConf: EventHubsConf,
                         partitionCount: Int,
                         useStart: Boolean = true): Map[PartitionId, SequenceNumber] = {

    val completed = mutable.Map[PartitionId, SequenceNumber]()
    val needsTranslation = ArrayBuffer[(NameAndPartition, EventPosition)]()
    val NamespaceAndEhName: String = ehConf.namespaceUri + ":" + ehConf.name

    logInfo(s"translate: NsAndEhName: $NamespaceAndEhName useStart is set to $useStart.")
    val positions = if (useStart) {
      ehConf.startingPositions.getOrElse(Map.empty).par
    } else {
      ehConf.endingPositions.getOrElse(Map.empty).par
    }
    val defaultPos = if (useStart) {
      ehConf.startingPosition.getOrElse(DefaultEventPosition)
    } else {
      ehConf.endingPosition.getOrElse(DefaultEndingPosition)
    }
    logInfo(s"translate: NsAndEhName: $NamespaceAndEhName PerPartitionPositions = $positions")
    logInfo(s"translate: NsAndEhName: $NamespaceAndEhName Default position = $defaultPos")

    (0 until partitionCount).par.foreach { id =>
      val nAndP = NameAndPartition(ehConf.name, id)
      val position = positions.getOrElse(nAndP, defaultPos)
      if (position.seqNo >= 0L) {
        // We don't need to translate a sequence number.
        // Put it straight into the results.
        synchronized(completed.put(id, position.seqNo))
      } else {
        val tuple = (nAndP, position)
        synchronized(needsTranslation += tuple)
      }
    }
    logInfo(s"translate: NsAndEhName: $NamespaceAndEhName needsTranslation = $needsTranslation")

    val consumerGroup = ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)
    val futures = for ((nAndP, pos) <- needsTranslation)
      yield
        pos.offset match {
          case StartOfStream => (nAndP.partitionId, earliestSeqNoF(nAndP.partitionId))
          case EndOfStream   => (nAndP.partitionId, latestSeqNoF(nAndP.partitionId))
          case _ =>
            val partitionProps =
              Await.result(getPartitionPropertiesF(nAndP.partitionId), ehConf.internalOperationTimeout)
            val seqNo =
              if (partitionProps.isEmpty || (pos.enqueuedTime != null &&
                  partitionProps.getLastEnqueuedTime.isBefore(pos.enqueuedTime.toInstant))) {
                Future.successful(partitionProps.getLastEnqueuedSequenceNumber + 1)
              } else {
                logInfo(
                  s"translate: creating receiver for Event Hub ${nAndP.ehName} on partition ${nAndP.partitionId}. filter: ${pos.convert}")

                // Track 2: Use EventHubsConsumerClient to receive events
                val eventPosition = pos.convert
                val receiver = retryJava(
                  EventHubsUtils.createReceiverInner(getConsumerClient,
                                                     consumerGroup,
                                                     nAndP.partitionId.toString,
                                                     eventPosition),
                  "translate: receiver creation."
                )
                receiver
                  .flatMap { events =>
                    val iter = events.iterator
                    if (events != null && iter.hasNext) {
                      Future.successful(iter.next.getSequenceNumber: SequenceNumber)
                    } else {
                      Future.successful(partitionProps.getLastEnqueuedSequenceNumber + 1: SequenceNumber)
                    }
                  }
              }

            (nAndP.partitionId, seqNo)
        }

    val future = Future
      .traverse(futures) {
        case (p, f) =>
          f.map { seqNo =>
            (p, seqNo)
          }
      }
      .map(x => (x.toMap ++ completed): Map[PartitionId, SequenceNumber])
    Await.result(future, ehConf.internalOperationTimeout)
  }
}

private[spark] object EventHubsClient {

  private var _userAgent: String = ""

  private[spark] def apply(ehConf: EventHubsConf): EventHubsClient =
    new EventHubsClient(ehConf)

  /**
   * @return the currently set user agent
   */
  def userAgent: String = {
    _userAgent
  }

  /**
   * A user agent is set whenever the connector is initialized.
   * In the case of the connector, the user agent is the Spark
   * version in use.
   *
   * @param user_agent the user agent
   */
  def userAgent_=(user_agent: String) {
    _userAgent = user_agent
  }
}

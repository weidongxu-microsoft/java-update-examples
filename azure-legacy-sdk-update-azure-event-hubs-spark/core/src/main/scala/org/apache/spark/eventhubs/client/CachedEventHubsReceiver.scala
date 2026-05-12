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

import java.time.Duration
import java.util.concurrent.{ CompletionException, RejectedExecutionException, TimeUnit }

import com.azure.messaging.eventhubs.{ EventData, EventHubClientBuilder, EventHubConsumerClient, PartitionProperties }
import com.azure.messaging.eventhubs.models.ReceiveOptions
import org.apache.spark.SparkEnv
import org.apache.spark.eventhubs.utils.MetricPlugin
import org.apache.spark.eventhubs.utils.RetryUtils.{ after, retryNotNull }
import org.apache.spark.eventhubs.{
  DefaultConsumerGroup,
  EventHubsConf,
  EventHubsUtils,
  NameAndPartition,
  PartitionPerformanceReceiver,
  SequenceNumber
}
import org.apache.spark.internal.Logging
import org.apache.spark.util.RpcUtils

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Awaitable, Future }

private[client] class CachedReceivedData(startSeqNo: SequenceNumber,
                                         batchSize: Int,
                                         cachedData: Seq[EventData]) {

  def matchSeqNoAndBatchSize(reqStartSeqNo: SequenceNumber, reqBatchSize: Int): Boolean = {
    (startSeqNo == reqStartSeqNo) && (batchSize == reqBatchSize)
  }

  def getCachedDataIterator(): Iterator[EventData] = cachedData.iterator
}

private[spark] trait CachedReceiver {
  private[eventhubs] def receive(ehConf: EventHubsConf,
                                 nAndP: NameAndPartition,
                                 requestSeqNo: SequenceNumber,
                                 batchSize: Int): Iterator[EventData]
}

private[client] class CachedEventHubsReceiver private (ehConf: EventHubsConf,
                                                       nAndP: NameAndPartition,
                                                       startSeqNo: SequenceNumber)
    extends Logging {

  type AwaitTimeoutException = java.util.concurrent.TimeoutException

  import org.apache.spark.eventhubs._

  private lazy val namespaceUri: String = ehConf.namespaceUri
  private lazy val consumerGroup = ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)
  private lazy val metricPlugin: Option[MetricPlugin] = ehConf.metricPlugin()

  private var currentSeqNo: SequenceNumber = startSeqNo

  private def toLong(value: java.lang.Long, fallback: Long): Long = {
    if (value == null) fallback else value.longValue()
  }

  private def newConsumerClient(): EventHubConsumerClient = {
    val builder = new EventHubClientBuilder()

    if (ehConf.useAadAuth) {
      val conn = ConnectionStringBuilder(ehConf.connectionString)
      val fqdn = conn.getEndpoint.getHost
      builder.fullyQualifiedNamespace(fqdn).eventHubName(ehConf.name)
      builder.credential(ehConf.aadAuthCallback().get)
    } else {
      builder.connectionString(ehConf.connectionString)
    }

    builder.consumerGroup(consumerGroup)
    builder.buildConsumerClient()
  }

  private[client] lazy val consumerClient: EventHubConsumerClient = newConsumerClient()

  private var cachedData: CachedReceivedData = new CachedReceivedData(-1, -1, null)

  private def partitionProperties: PartitionProperties = {
    consumerClient.getPartitionProperties(nAndP.partitionId.toString)
  }

  private def lastReceivedOffset(): Future[Long] = Future.successful(currentSeqNo - 1)

  private def receiveOne(timeout: Duration, msg: String): Future[Iterable[EventData]] = {
    def receiveOneWithRetry(timeout: Duration,
                            msg: String,
                            retryCount: Int): Future[Iterable[EventData]] = {
      if (retryCount >= RetryCount) {
        Future.successful(Seq.empty)
      } else {
        val receiveOptions = new ReceiveOptions().setTrackLastEnqueuedEventProperties(true)
        if (ehConf.useExclusiveReceiver) {
          receiveOptions.setOwnerLevel(DefaultEpoch)
        }

        val received = consumerClient
          .receiveFromPartition(nAndP.partitionId.toString,
                                1,
                                EventPosition.fromSequenceNumber(currentSeqNo).convert,
                                timeout,
                                receiveOptions)
          .iterator()
          .asScala
          .toSeq
          .map(_.getData)

        if (received.nonEmpty) {
          val seq = toLong(received.last.getSequenceNumber, currentSeqNo)
          currentSeqNo = seq + 1
          Future.successful(received)
        } else {
          val retry = retryCount + 1
          after(WaitInterval.milliseconds)(receiveOneWithRetry(timeout, msg, retry))
        }
      }
    }

    receiveOneWithRetry(timeout, msg, 0)
  }

  private def closeReceiver(): Future[Void] = {
    Future.successful({
      consumerClient.close()
      null
    })
  }

  private def recreateReceiver(seqNo: SequenceNumber): Unit = {
    val taskId = EventHubsUtils.getTaskId
    val startTimeNs = System.nanoTime()
    def elapsedTimeNs = System.nanoTime() - startTimeNs

    currentSeqNo = seqNo

    val elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNs)
    logInfo(s"(TID $taskId) Finished recreating a receiver cursor for namespaceUri: $namespaceUri EventHubNameAndPartition: " +
      s"$nAndP consumer group: $consumerGroup: $elapsedTimeMs ms")
  }

  private def checkCursor(requestSeqNo: SequenceNumber): Future[Iterable[EventData]] = {
    val taskId = EventHubsUtils.getTaskId

    val lastReceivedSeqNo = Await.result(lastReceivedOffset(), ehConf.internalOperationTimeout)

    if ((lastReceivedSeqNo > -1 && lastReceivedSeqNo + 1 != requestSeqNo)) {
      logInfo(s"(TID $taskId) checkCursor. Recreating receiver cursor for namespaceUri: $namespaceUri " +
        s"EventHubNameAndPartition: $nAndP consumer group: $consumerGroup. requestSeqNo: $requestSeqNo, " +
        s"lastReceivedSeqNo: $lastReceivedSeqNo")

      recreateReceiver(requestSeqNo)
    }

    val event = awaitReceiveMessage(
      receiveOne(ehConf.receiverTimeout.getOrElse(DefaultReceiverTimeout), "checkCursor initial"),
      requestSeqNo)

    if (event.isEmpty) {
      Future.successful(event)
    } else {
      val receivedSeqNo = toLong(event.head.getSequenceNumber, -1L)

      if (receivedSeqNo != requestSeqNo) {
        recreateReceiver(requestSeqNo)
        val movedEvent = awaitReceiveMessage(
          receiveOne(ehConf.receiverTimeout.getOrElse(DefaultReceiverTimeout), "checkCursor move"),
          requestSeqNo)

        if (movedEvent.nonEmpty) {
          val movedSeqNo = toLong(movedEvent.head.getSequenceNumber, -1L)
          val props = partitionProperties
          if (requestSeqNo < props.getBeginningSequenceNumber && movedSeqNo == props.getBeginningSequenceNumber) {
            Future.successful(movedEvent)
          } else {
            Future.successful(movedEvent)
          }
        } else {
          Future.successful(movedEvent)
        }
      } else {
        Future.successful(event)
      }
    }
  }

  private def receive(requestSeqNo: SequenceNumber, batchSize: Int): Iterator[EventData] = {
    val taskId = EventHubsUtils.getTaskId
    val startTimeNs = System.nanoTime()
    def elapsedTimeNs = System.nanoTime() - startTimeNs

    if (cachedData.matchSeqNoAndBatchSize(requestSeqNo, batchSize)) {
      logInfo(s"(TID $taskId) Returned data from cache for namespaceUri: $namespaceUri EventHubNameAndPartition: $nAndP " +
        s"consumer group: $consumerGroup, requestSeqNo: $requestSeqNo, batchSize: $batchSize")
      return cachedData.getCachedDataIterator()
    }

    val first = Await.result(checkCursor(requestSeqNo), ehConf.internalOperationTimeout)
    if (first.isEmpty) {
      return Iterator.empty
    }

    val firstSeqNo = toLong(first.head.getSequenceNumber, requestSeqNo)
    val batchCount = (requestSeqNo + batchSize - firstSeqNo).toInt

    if (batchCount <= 0) {
      return Iterator.empty
    }

    val theRest = for { i <- 1 until batchCount } yield
      awaitReceiveMessage(receiveOne(ehConf.receiverTimeout.getOrElse(DefaultReceiverTimeout),
                                     s"receive; $nAndP; seqNo: ${requestSeqNo + i}"),
                          requestSeqNo)

    val combined = first ++ theRest.flatten
    val sortedSeq = combined.toSeq.sortWith((e1, e2) =>
      toLong(e1.getSequenceNumber, -1L) < toLong(e2.getSequenceNumber, -1L))

    cachedData = new CachedReceivedData(requestSeqNo, batchSize, sortedSeq)

    val sorted = sortedSeq.iterator
    val (result, validate) = sorted.duplicate
    val elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNs)

    if (ehConf.slowPartitionAdjustment) {
      sendPartitionPerformanceToDriver(
        PartitionPerformanceMetric(nAndP,
                                   EventHubsUtils.getTaskContextSlim,
                                   requestSeqNo,
                                   batchCount,
                                   elapsedTimeMs))
    }

    if (metricPlugin.isDefined) {
      val (validateSize, batchSizeInBytes) =
        validate
          .map(eventData => (1, eventData.getBody.length.toLong))
          .reduceOption { (countAndSize1, countAndSize2) =>
            (countAndSize1._1 + countAndSize2._1, countAndSize1._2 + countAndSize2._2)
          }
          .getOrElse((0, 0L))
      metricPlugin.foreach(
        _.onReceiveMetric(EventHubsUtils.getTaskContextSlim,
                          nAndP,
                          batchCount,
                          batchSizeInBytes,
                          elapsedTimeMs))
      assert(validateSize == batchCount)
    } else {
      assert(validate.size == batchCount)
    }

    logInfo(s"(TID $taskId) Finished receiving for namespaceUri: $namespaceUri EventHubNameAndPartition: $nAndP " +
      s"consumer group: $consumerGroup, batchSize: $batchSize, elapsed time: $elapsedTimeMs ms")
    result
  }

  private def awaitReceiveMessage[T](awaitable: Awaitable[T], requestSeqNo: SequenceNumber): T = {
    val taskId = EventHubsUtils.getTaskId

    try {
      Await.result(awaitable, ehConf.internalOperationTimeout)
    } catch {
      case e: AwaitTimeoutException =>
        logError(
          s"(TID $taskId) awaitReceiveMessage call failed with timeout. NamespaceUri: $namespaceUri " +
            s"EventHubNameAndPartition: $nAndP consumer group: $consumerGroup. requestSeqNo: $requestSeqNo")

        recreateReceiver(requestSeqNo)
        throw e
    }
  }

  private def sendPartitionPerformanceToDriver(partitionPerformance: PartitionPerformanceMetric): Unit = {
    logDebug(
      s"(Task: ${EventHubsUtils.getTaskContextSlim}) sends PartitionPerformanceMetric: " +
        s"${partitionPerformance} to the driver.")
    try {
      CachedEventHubsReceiver.partitionPerformanceReceiverRef.send(partitionPerformance)
    } catch {
      case e: Exception =>
        logError(
          s"(Task: ${EventHubsUtils.getTaskContextSlim}) failed to send the RPC message containing " +
            s"PartitionPerformanceMetric: ${partitionPerformance} to the driver with error: ${e}.")
    }
  }
}

private[spark] object CachedEventHubsReceiver extends CachedReceiver with Logging {

  type MutableMap[A, B] = scala.collection.mutable.HashMap[A, B]

  private[this] val receivers = new MutableMap[String, CachedEventHubsReceiver]()

  val partitionPerformanceReceiverRef =
    RpcUtils.makeDriverRef(PartitionPerformanceReceiver.ENDPOINT_NAME,
                           SparkEnv.get.conf,
                           SparkEnv.get.rpcEnv)

  private def key(ehConf: EventHubsConf, nAndP: NameAndPartition): String = {
    (ehConf.connectionString + ehConf.consumerGroup + nAndP.partitionId).toLowerCase
  }

  private[eventhubs] override def receive(ehConf: EventHubsConf,
                                          nAndP: NameAndPartition,
                                          requestSeqNo: SequenceNumber,
                                          batchSize: Int): Iterator[EventData] = {
    val taskId = EventHubsUtils.getTaskId

    logInfo(s"(TID $taskId) EventHubsCachedReceiver look up. For namespaceUri ${ehConf.namespaceUri} " +
      s"EventHubNameAndPartition $nAndP consumer group ${ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)}. " +
      s"requestSeqNo: $requestSeqNo, batchSize: $batchSize")

    var receiver: CachedEventHubsReceiver = null
    receivers.synchronized {
      receiver = receivers.getOrElseUpdate(key(ehConf, nAndP), {
        CachedEventHubsReceiver(ehConf, nAndP, requestSeqNo)
      })
    }

    try {
      receiver.receive(requestSeqNo, batchSize)
    } catch {
      case completionExecution: CompletionException =>
        val exceptionCause = completionExecution.getCause
        if (exceptionCause != null && exceptionCause.isInstanceOf[RejectedExecutionException] &&
            exceptionCause.getMessage != null &&
            exceptionCause.getMessage.contains("ReactorDispatcher instance is closed")) {
          logInfo(s"(TID $taskId) EventHubsCachedReceiver receive execution failed with $completionExecution. " +
            s"Recreating cached receiver and retrying.")
          receiver.consumerClient.close()
          receiver = CachedEventHubsReceiver(ehConf, nAndP, requestSeqNo)
          receivers.synchronized {
            receivers.update(key(ehConf, nAndP), receiver)
          }
          receiver.receive(requestSeqNo, batchSize)
        } else {
          throw completionExecution
        }
    }
  }

  def apply(ehConf: EventHubsConf,
            nAndP: NameAndPartition,
            startSeqNo: SequenceNumber): CachedEventHubsReceiver = {
    new CachedEventHubsReceiver(ehConf, nAndP, startSeqNo)
  }
}

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

package org.apache.spark.eventhubs

import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.Base64
import java.util.ArrayList
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

import com.azure.messaging.eventhubs.{ EventData, EventHubConsumerClient }
import com.azure.messaging.eventhubs.models.{ EventPosition => Track2EventPosition }

import org.apache.spark.api.java.{ JavaRDD, JavaSparkContext }
import org.apache.spark.eventhubs.client.EventHubsClient
import org.apache.spark.eventhubs.rdd.{ EventHubsRDD, OffsetRange }
import org.apache.spark.internal.Logging
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.api.java.{ JavaInputDStream, JavaStreamingContext }
import org.apache.spark.streaming.eventhubs.EventHubsDirectDStream
import org.apache.spark.{ SparkContext, SparkEnv, TaskContext }
import org.apache.spark.rpc.RpcEndpointRef
import org.apache.spark.util.RpcUtils

import scala.util.Try

/**
 * Helper to create Direct DStreams which consume events from Event Hubs.
 */
object EventHubsUtils extends Logging {

  private val simulatedSequenceNumbers =
    java.util.Collections.synchronizedMap(new java.util.WeakHashMap[EventData, java.lang.Long]())
  private val simulatedEnqueuedTimes =
    java.util.Collections.synchronizedMap(new java.util.WeakHashMap[EventData, java.lang.Long]())

  var partitionPerformanceReceiverRef: RpcEndpointRef = null

  private def createRpcEndpoint() = {
    if (partitionPerformanceReceiverRef == null) {
      try {
        partitionPerformanceReceiverRef = RpcUtils.makeDriverRef(
          PartitionPerformanceReceiver.ENDPOINT_NAME,
          SparkEnv.get.conf,
          SparkEnv.get.rpcEnv)
        logInfo(
          s"There is an existing partitionPerformanceReceiverRef on the driver, use that one rather than creating a new one")
      } catch {
        case e: Exception =>
          val partitionsStatusTracker = PartitionsStatusTracker.getPartitionStatusTracker
          val partitionPerformanceReceiver: PartitionPerformanceReceiver =
            new PartitionPerformanceReceiver(SparkEnv.get.rpcEnv, partitionsStatusTracker)
          partitionPerformanceReceiverRef = SparkEnv.get.rpcEnv
            .setupEndpoint(PartitionPerformanceReceiver.ENDPOINT_NAME, partitionPerformanceReceiver)
      }
    }
  }

  /**
   * Creates a Direct DStream which consumes from  the Event Hubs instance
   * specified in the [[EventHubsConf]].
   *
   * @param ssc    the StreamingContext this DStream belongs to
   * @param ehConf the parameters for your EventHubs instance
   * @return An [[EventHubsDirectDStream]]
   */
  def createDirectStream(ssc: StreamingContext, ehConf: EventHubsConf): EventHubsDirectDStream = {
    createRpcEndpoint()
    new EventHubsDirectDStream(ssc, ehConf, EventHubsClient.apply)
  }

  /**
   * Creates a Direct DStream which consumes from  the Event Hubs instance
   * specified in the [[EventHubsConf]].
   *
   * @param jssc   the JavaStreamingContext this DStream belongs to
   * @param ehConf the parameters for your EventHubs instance
   * @return A [[JavaInputDStream]] containing [[EventData]]
   */
  def createDirectStream(jssc: JavaStreamingContext,
                         ehConf: EventHubsConf): JavaInputDStream[EventData] = {
    createRpcEndpoint()
    new JavaInputDStream(createDirectStream(jssc.ssc, ehConf))
  }

  /**
   * Creates an RDD which is contains events from an EventHubs instance.
   * Starting and ending offsets are specified in advance.
   *
   * @param sc           the SparkContext the RDD belongs to
   * @param ehConf       contains EventHubs-specific configurations
   * @param offsetRanges offset ranges that define the EventHubs data belonging to this RDD
   * @return An [[EventHubsRDD]]
   *
   */
  def createRDD(sc: SparkContext,
                ehConf: EventHubsConf,
                offsetRanges: Array[OffsetRange]): EventHubsRDD = {
    createRpcEndpoint()
    new EventHubsRDD(sc, ehConf.trimmed, offsetRanges)
  }

  /**
   * Creates an RDD which is contains events from an EventHubs instance.
   * Starting and ending offsets are specified in advance.
   *
   * @param jsc          the JavaSparkContext the RDD belongs to
   * @param ehConf       contains EventHubs-specific configurations
   * @param offsetRanges offset ranges that define the EventHubs data belonging to this RDD
   * @return A [[JavaRDD]] containing [[EventData]]
   *
   */
  def createRDD(jsc: JavaSparkContext,
                ehConf: EventHubsConf,
                offsetRanges: Array[OffsetRange]): JavaRDD[EventData] = {
    createRpcEndpoint()
    new JavaRDD(createRDD(jsc.sc, ehConf, offsetRanges))
  }

  /**
   * Track 2: Creates a receiver for a specific partition using EventHubsConsumerClient.
   * Returns an iterator of EventData for the specified partition.
   *
   * @param consumerClient the EventHubsConsumerClient
   * @param consumerGroup the consumer group
   * @param partitionId the partition ID
   * @param eventPosition the starting event position
   * @return CompletableFuture with Iterable of EventData
   */
  def createReceiverInner(
      consumerClient: EventHubConsumerClient,
      consumerGroup: String,
      partitionId: String,
      eventPosition: Track2EventPosition): CompletableFuture[java.lang.Iterable[EventData]] = {
    val taskId = EventHubsUtils.getTaskId
    logInfo(
      s"(TID $taskId) creating receiver for Event Hub partition $partitionId, consumer group $consumerGroup")

    // Track 2: Get events from partition starting at the specified position
    val future = new CompletableFuture[java.lang.Iterable[EventData]]()
    try {
      val received = consumerClient.receiveFromPartition(partitionId, 1, eventPosition)
      val data = new ArrayList[EventData]()
      val iter = received.iterator()
      if (iter.hasNext) {
        val event = iter.next()
        if (event != null && event.getData != null) {
          data.add(event.getData)
        }
      }

      if (!data.isEmpty) {
        future.complete(data)
      } else {
        future.complete(java.util.Collections.emptyList[EventData]())
      }
    } catch {
      case e: Exception =>
        logError(s"(TID $taskId) error creating receiver for partition $partitionId", e)
        future.completeExceptionally(e)
    }
    future
  }

  // TODO: Track 1 receiver creation - deprecated, use createReceiverInner with consumerClient instead

  def registerSimulatedEventMetadata(event: EventData,
                                     seqNo: SequenceNumber,
                                     enqueuedAtMs: Long): Unit = {
    simulatedSequenceNumbers.put(event, Long.box(seqNo))
    simulatedEnqueuedTimes.put(event, Long.box(enqueuedAtMs))
  }

  def getEventSequenceNumber(event: EventData): SequenceNumber = {
    Option(event.getSequenceNumber)
      .map(_.longValue())
      .orElse {
        Option(simulatedSequenceNumbers.get(event)).map(_.longValue())
      }
      .orElse {
        Option(event.getProperties)
          .flatMap(p => Option(p.get(SequenceNumberAnnotation)))
          .flatMap {
            case n: java.lang.Number => Some(n.longValue())
            case s: String           => Try(s.toLong).toOption
            case other               => Try(other.toString.toLong).toOption
          }
      }
      .getOrElse(-1L)
  }

  def getEventEnqueuedTimeMillis(event: EventData): Long = {
    Option(simulatedEnqueuedTimes.get(event))
      .map(_.longValue())
      .orElse {
        Option(event.getEnqueuedTime).map(_.toEpochMilli)
      }
      .orElse {
        Option(event.getProperties)
          .flatMap(p => Option(p.get(EnqueuedTimeAnnotation)))
          .flatMap {
            case n: java.lang.Number => Some(n.longValue())
            case s: String           => Try(s.toLong).toOption
            case other               => Try(other.toString.toLong).toOption
          }
      }
      .getOrElse(0L)
  }


  def getTaskId: Long = {
    val taskContext = TaskContext.get()
    if (taskContext != null) {
      taskContext.taskAttemptId()
    } else -1
  }

  def getTaskContextSlim: TaskContextSlim = {
    val taskContext = TaskContext.get()
    if (taskContext != null) {
      new TaskContextSlim(taskContext.stageId(),
                          taskContext.taskAttemptId(),
                          taskContext.partitionId(),
                          SparkEnv.get.executorId)
    } else {
      new TaskContextSlim(-1, -1, -1, "")
    }
  }

  def encode(inputStr: String): String = {
    java.util.Base64.getEncoder
      .encodeToString(inputStr.getBytes(StandardCharsets.UTF_8))
  }

  def decode(inputString: String): String = {
    new String(java.util.Base64.getDecoder.decode(inputString), StandardCharsets.UTF_8)
  }

  def encrypt(inputStr: String): String = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

    cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec)
    Base64.getEncoder.encodeToString(cipher.doFinal(inputStr.getBytes(StandardCharsets.UTF_8)))
  }

  def decrypt(inputString: String): String = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")

    cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec)
    new String(cipher.doFinal(Base64.getDecoder.decode(inputString)), StandardCharsets.UTF_8)
  }

  private def getSecretKeySpec: SecretKeySpec = {
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keySpec =
      new PBEKeySpec(SparkConnectorVersion.toCharArray, SparkConnectorVersion.getBytes, 1000, 256)
    val secretKey = secretKeyFactory.generateSecret(keySpec)
    new SecretKeySpec(secretKey.getEncoded, "AES")
  }
}

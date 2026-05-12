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
import java.util

import com.azure.messaging.eventhubs._
import com.azure.messaging.eventhubs.models.CreateBatchOptions
import org.apache.spark.eventhubs.EventHubsConf
import org.apache.spark.internal.Logging
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * A [[Client]] which connects to an event hub instance. All interaction
 * between Spark and Event Hubs will happen within this client.
 */
@SerialVersionUID(1L)
private[spark] class EventHubsClient(private val ehConf: EventHubsConf)
    extends Serializable
    with Client
    with Logging {

  import org.apache.spark.eventhubs._

  ehConf.validate

  private implicit val formats = Serialization.formats(NoTypeHints)

  private var _consumerClient: EventHubConsumerClient = _
  private var _producerClient: EventHubProducerClient = _
  private var partitionSenderId: Option[String] = None

  private var partitionCountCache: Int = 0
  private var partitionCountCacheUpdateTimestamp: Long = 0

  private def toLong(value: java.lang.Long, fallback: Long): Long = {
    if (value == null) fallback else value.longValue()
  }

  private def newBuilder(consumerGroup: Option[String]): EventHubClientBuilder = {
    val builder = new EventHubClientBuilder()

    if (ehConf.useAadAuth) {
      val conn = ConnectionStringBuilder(ehConf.connectionString)
      val fqdn = conn.getEndpoint.getHost
      builder.fullyQualifiedNamespace(fqdn).eventHubName(ehConf.name)
      builder.credential(ehConf.aadAuthCallback().get)
    } else {
      builder.connectionString(ehConf.connectionString)
    }

    consumerGroup.foreach(builder.consumerGroup)
    builder
  }

  private def consumerClient: EventHubConsumerClient = synchronized {
    if (_consumerClient == null) {
      val cg = ehConf.consumerGroup.getOrElse(DefaultConsumerGroup)
      _consumerClient = newBuilder(Some(cg)).buildConsumerClient()
    }
    _consumerClient
  }

  private def producerClient: EventHubProducerClient = synchronized {
    if (_producerClient == null) {
      _producerClient = newBuilder(None).buildProducerClient()
    }
    _producerClient
  }

  override def createPartitionSender(partition: Int): Unit = {
    partitionSenderId = Some(partition.toString)
  }

  override def send(event: EventData,
                    partition: Option[Rate] = None,
                    partitionKey: Option[String] = None,
                    properties: Option[Map[String, String]] = None): Unit = {
    properties.foreach { props: Map[String, String] =>
      event.getProperties.putAll(props.asJava)
    }

    val targetPartition = partition.map(_.toString).orElse(partitionSenderId)

    if (targetPartition.isDefined || partitionKey.isDefined) {
      val options = new CreateBatchOptions()
      targetPartition.foreach(options.setPartitionId)
      partitionKey.foreach(options.setPartitionKey)

      val batch = producerClient.createBatch(options)
      if (!batch.tryAdd(event)) {
        throw new IllegalStateException("Unable to add event to Event Hubs batch.")
      }
      producerClient.send(batch)
    } else {
      producerClient.send(util.Arrays.asList(event))
    }
  }

  private def partitionProperties(partitionId: PartitionId): PartitionProperties = {
    consumerClient.getPartitionProperties(partitionId.toString)
  }

  override def allBoundedSeqNos: Map[PartitionId, (SequenceNumber, SequenceNumber)] = {
    (0 until partitionCount).map { i =>
      val p = partitionProperties(i)
      val earliest = if (p.isEmpty) p.getLastEnqueuedSequenceNumber + 1 else p.getBeginningSequenceNumber
      val latest = p.getLastEnqueuedSequenceNumber + 1
      i -> ((earliest, latest): (Long, Long))
    }.toMap
  }

  override def partitionCount: Int = {
    if (ehConf.dynamicPartitionDiscovery) {
      partitionCountDynamic
    } else {
      partitionCountLazyVal
    }
  }

  lazy val partitionCountLazyVal: Int = consumerClient.getPartitionIds.asScala.size

  def partitionCountDynamic: Int = {
    val currentTimeStamp = System.currentTimeMillis()
    if ((currentTimeStamp - partitionCountCacheUpdateTimestamp > UpdatePartitionCountIntervalMS) ||
        (partitionCountCache == 0)) {
      partitionCountCache = consumerClient.getPartitionIds.asScala.size
      partitionCountCacheUpdateTimestamp = currentTimeStamp
    }
    partitionCountCache
  }

  override def close(): Unit = {
    if (_producerClient != null) {
      _producerClient.close()
      _producerClient = null
    }

    if (_consumerClient != null) {
      _consumerClient.close()
      _consumerClient = null
    }
  }

  override def translate(ehConf: EventHubsConf,
                         partitionCount: Int,
                         useStart: Boolean = true): Map[PartitionId, SequenceNumber] = {

    val completed = mutable.Map[PartitionId, SequenceNumber]()
    val needsTranslation = ArrayBuffer[(NameAndPartition, EventPosition)]()

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

    (0 until partitionCount).par.foreach { id =>
      val nAndP = NameAndPartition(ehConf.name, id)
      val position = positions.getOrElse(nAndP, defaultPos)
      if (position.seqNo >= 0L) {
        synchronized(completed.put(id, position.seqNo))
      } else {
        synchronized(needsTranslation += ((nAndP, position)))
      }
    }

    val translated = needsTranslation.map {
      case (nAndP, pos) =>
        val props = partitionProperties(nAndP.partitionId)

        val seqNo = pos.offset match {
          case StartOfStream =>
            if (props.isEmpty) toLong(props.getLastEnqueuedSequenceNumber, -1L) + 1
            else toLong(props.getBeginningSequenceNumber, -1L)
          case EndOfStream =>
            toLong(props.getLastEnqueuedSequenceNumber, -1L) + 1
          case _ =>
            if (props.isEmpty ||
                (pos.enqueuedTime != null && props.getLastEnqueuedTime.isBefore(pos.enqueuedTime.toInstant))) {
              toLong(props.getLastEnqueuedSequenceNumber, -1L) + 1
            } else {
              val events = consumerClient
                .receiveFromPartition(nAndP.partitionId.toString,
                                      1,
                                      pos.convert,
                                      ehConf.receiverTimeout.getOrElse(DefaultReceiverTimeout))
                .iterator()
                .asScala
                .toSeq
              events.headOption.map(e => toLong(e.getData.getSequenceNumber, -1L)).getOrElse {
                toLong(props.getLastEnqueuedSequenceNumber, -1L) + 1
              }
            }
        }

        nAndP.partitionId -> seqNo
    }.toMap

    translated ++ completed.toMap
  }
}

private[spark] object EventHubsClient {

  private[spark] def apply(ehConf: EventHubsConf): EventHubsClient =
    new EventHubsClient(ehConf)

  @volatile private var currentUserAgent: String = "SparkConnector"

  def userAgent: String = currentUserAgent

  def userAgent_=(user_agent: String): Unit = {
    currentUserAgent = user_agent
  }
}

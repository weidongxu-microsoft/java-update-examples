/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.metrics.source;

import org.apache.hadoop.metrics2.MetricsCollector;
import org.apache.ranger.metrics.RangerMetricsInfo;

/**
 * Simplified stub for RangerMetricsContainerSource - no embedded server dependencies
 */
public class RangerMetricsContainerSource extends RangerMetricsSource {
    private final String context;

    public RangerMetricsContainerSource(String context) {
        this.context = context;
    }

    @Override
    protected void refresh() {
        // Stub implementation - no metrics to refresh
    }

    @Override
    protected void update(MetricsCollector collector, boolean all) {
        collector.addRecord("RangerWebContainer")
                .setContext(this.context)
                .addCounter(new RangerMetricsInfo("MaxConnectionsCount", "Ranger max configured container connections"), 0)
                .addCounter(new RangerMetricsInfo("ActiveConnectionsCount", "Ranger active container connections"), 0)
                .addCounter(new RangerMetricsInfo("ConnectionAcceptCount", "Ranger accept connections count"), 0)
                .addCounter(new RangerMetricsInfo("ConnectionTimeout", "Ranger connection timeout"), 0)
                .addCounter(new RangerMetricsInfo("KeepAliveTimeout", "Ranger connection keepAlive timeout"), 0)
                .addCounter(new RangerMetricsInfo("MaxWorkerThreadsCount", "Ranger container worker threads count"), 0)
                .addCounter(new RangerMetricsInfo("MinSpareWorkerThreadsCount", "Ranger container minimum spare worker threads count"), 0)
                .addCounter(new RangerMetricsInfo("ActiveWorkerThreadsCount", "Ranger container active worker threads count"), 0)
                .addCounter(new RangerMetricsInfo("TotalWorkerThreadsCount", "Ranger container total worker threads count"), 0);
    }
}

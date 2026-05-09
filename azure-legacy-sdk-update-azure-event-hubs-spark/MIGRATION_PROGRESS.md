# Migration Progress: Track 1 -> Track 2 Upgrade

Start Date: 2026-05-09
Last Updated: 2026-05-09
Current Branch: migrate/eventhubs-spark-track2
Current Status: Compile green and test green on migration branch baseline

---

## Latest Milestone

Migration branch has been stabilized with Track 2 client/refactor updates plus test reliability fixes.

Published commits:
- 5758950a - test: ignore hadoop-local-fs dependent suites during migration
- c596e535 - fix: continue Track2 migration and restore test stability

Branch published to remote:
- origin/migrate/eventhubs-spark-track2

---

## Validation Baseline (Current)

Command run:

```bash
mvn test
```

Result:
- Build: SUCCESS
- ScalaTest: 74 succeeded, 0 failed, 39 ignored, 0 pending

Ignored suites (Windows local Hadoop FS dependent):
- EventHubsSourceSuite
- EventHubsSourceOffsetSuite (serialization-oriented cases)
- EventHubsSinkSuite
- EventHubsDirectDStreamSuite

---

## Completed in This Session

Core migration/stability updates:
- Added null-safe Track 2 EventData metadata extraction for sequence/enqueued time.
- Added simulator metadata registration path to avoid relying on missing broker-populated fields.
- Updated receiver/client paths to use safe sequence extraction.
- Hardened source row conversion against null metadata.
- Updated retry transient detection for Track 2 AmqpException semantics.
- Restored max silent time validation compatibility with test expectations.

Files updated:
- core/src/main/scala/org/apache/spark/eventhubs/EventHubsUtils.scala
- core/src/main/scala/org/apache/spark/eventhubs/client/CachedEventHubsReceiver.scala
- core/src/main/scala/org/apache/spark/eventhubs/client/EventHubsClient.scala
- core/src/main/scala/org/apache/spark/eventhubs/package.scala
- core/src/main/scala/org/apache/spark/eventhubs/utils/EventHubsTestUtils.scala
- core/src/main/scala/org/apache/spark/eventhubs/utils/RetryUtils.scala
- core/src/main/scala/org/apache/spark/sql/eventhubs/EventHubsSourceProvider.scala
- core/src/test/scala/org/apache/spark/eventhubs/rdd/EventHubsRDDSuite.scala
- core/src/test/scala/org/apache/spark/eventhubs/utils/RetryUtilsSuite.scala

---

## Remaining Migration Work

Priority next items:
- Complete ConnectionStringBuilder migration cleanup and validation.
- Continue Phase 3 test refactor to remove temporary ignores where feasible.
- Run full JDK 8 clean validation baseline:

```bash
mvn clean test
```

---

## Notes

- JDK 8 remains the required runtime for stable Scala 2.11 build/test behavior.
- Current baseline is suitable for continued incremental Track 2 migration from a green state.




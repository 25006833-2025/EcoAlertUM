package com.ecoalertum.android.core.data

import com.ecoalertum.android.core.data.fake.MockJsonParser
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.HazardReading
import java.io.File
import java.time.OffsetDateTime

/** Mock files and a fixed clock for tests. Unit tests run with the app/ module as working directory. */
object TestData {

    const val MINUTE = 60_000L

    /** 2 hours after the mock readings were fetched (16:05 on 23 Oct 2026, MYT). */
    val NOW: Long = millis("2026-10-23T18:00:00+08:00")

    fun millis(iso: String): Long = OffsetDateTime.parse(iso).toInstant().toEpochMilli()

    fun asset(name: String): File = File("src/main/assets/mock/$name")

    fun readings(): List<HazardReading> = MockJsonParser.parseReadings(asset("hazardReadings.json").readText())

    fun alerts(): List<Alert> = MockJsonParser.parseAlerts(asset("alerts.json").readText())
}

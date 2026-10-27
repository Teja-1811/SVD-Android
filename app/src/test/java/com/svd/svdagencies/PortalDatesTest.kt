package com.svd.svdagencies

import com.svd.svdagencies.utils.PortalDates
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class PortalDatesTest {
    @Test fun formatsLeapDayForStatements() {
        assertEquals("29 Feb 2024", PortalDates.display("2024-02-29", Locale.US))
    }
    @Test fun invalidDatesArePreservedInsteadOfRollingIntoAnotherMonth() {
        assertEquals("2025-02-29", PortalDates.display("2025-02-29", Locale.US))
        assertEquals("2026-04-31", PortalDates.display("2026-04-31", Locale.US))
    }
    @Test fun nonDateLabelsAndTrailingDataArePreserved() {
        listOf("", "Opening balance", "2026-10-24 extra", "2026-2-3").forEach {
            assertEquals(it, PortalDates.display(it, Locale.US))
        }
    }
}

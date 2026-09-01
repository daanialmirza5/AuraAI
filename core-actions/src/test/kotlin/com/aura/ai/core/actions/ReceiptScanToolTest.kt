package com.aura.ai.core.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [parseReceipt] is pure text-in/struct-out — no ML Kit, no Android, no `imageUri` needed to
 *  verify the parsing heuristic itself. See `docs/VISION_RUNTIME.md` §3 for why this is
 *  documented as a heuristic, not a guarantee. */
class ReceiptScanToolTest {
    @Test
    fun `finds a total on a line explicitly labeled total`() {
        val text =
            """
            COFFEE HOUSE
            Latte              4.50
            Muffin              3.25
            TOTAL              $7.75
            07/29/2026
            """.trimIndent()

        val receipt = parseReceipt(text)

        assertEquals("$7.75", receipt.total)
    }

    @Test
    fun `falls back to the largest currency amount when no line says total`() {
        val text = "Item A $3.00\nItem B $12.50\nItem C $1.20"

        val receipt = parseReceipt(text)

        assertEquals("$12.50", receipt.total)
    }

    @Test
    fun `extracts a date in common slash format`() {
        val receipt = parseReceipt("STORE\nTotal $5.00\n07/29/2026")
        assertEquals("07/29/2026", receipt.date)
    }

    @Test
    fun `returns null date when none is present`() {
        val receipt = parseReceipt("STORE\nTotal $5.00")
        assertNull(receipt.date)
    }

    @Test
    fun `picks the first plausible text-only line as the merchant`() {
        val receipt = parseReceipt("Joe's Diner\n123 Main St\nTotal $9.99")
        assertEquals("Joe's Diner", receipt.merchant)
    }

    @Test
    fun `describe reports low confidence honestly when nothing was found`() {
        val receipt = parseReceipt("")
        assertTrue(receipt.describe().contains("Couldn't confidently parse"))
    }

    @Test
    fun `describe combines whatever fields were actually found`() {
        val receipt = ReceiptSummary(merchant = "Joe's Diner", total = "$9.99", date = "07/29/2026")
        val description = receipt.describe()
        assertTrue(description.contains("Joe's Diner"))
        assertTrue(description.contains("9.99"))
        assertTrue(description.contains("07/29/2026"))
    }
}

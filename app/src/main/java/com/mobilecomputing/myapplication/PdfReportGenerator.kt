package com.mobilecomputing.myapplication

import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    fun generateCustomReportPdf(
        outputStream: OutputStream,
        currencySymbol: String,
        currencyCode: String,
        dateRangeTitle: String,
        totalIncome: Double,
        totalExpense: Double,
        categoryBreakdown: Map<String, Double>,
        expenses: List<Expense>
    ) {
        val document = PdfDocument()

        val titlePaint = Paint().apply {
            color = AndroidColor.parseColor("#008744")
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = AndroidColor.parseColor("#212121")
            textSize = 13f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val subPaint = Paint().apply {
            color = AndroidColor.parseColor("#757575")
            textSize = 10f
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = AndroidColor.parseColor("#212121")
            textSize = 10f
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = AndroidColor.parseColor("#212121")
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = AndroidColor.parseColor("#008744")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = AndroidColor.parseColor("#D32F2F")
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val boxPaint = Paint().apply {
            color = AndroidColor.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        val linePaint = Paint().apply {
            color = AndroidColor.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        val itemsPerPage = 28
        val pagesCount = if (expenses.isEmpty()) 1 else ((expenses.size - 1) / itemsPerPage) + 1

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val generatedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

        for (pageIndex in 0 until pagesCount) {
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            var y = 45f

            // Top Header
            canvas.drawText("FINANCIAL STATEMENT & BUDGET REPORT", 40f, y, titlePaint)
            y += 18f
            canvas.drawText("Period: $dateRangeTitle | Generated: $generatedAt", 40f, y, subPaint)
            y += 18f
            canvas.drawLine(40f, y, 555f, y, linePaint)
            y += 18f

            if (pageIndex == 0) {
                // Summary Box on Page 1
                canvas.drawRoundRect(40f, y, 555f, y + 60f, 8f, 8f, boxPaint)
                val colWidth = 515f / 3f

                canvas.drawText("Total Income", 55f, y + 24f, subPaint)
                canvas.drawText("+$currencySymbol ${String.format("%,.0f", totalIncome)}", 55f, y + 44f, greenPaint)

                canvas.drawText("Total Expense", 55f + colWidth, y + 24f, subPaint)
                canvas.drawText("-$currencySymbol ${String.format("%,.0f", totalExpense)}", 55f + colWidth, y + 44f, redPaint)

                val netBalance = totalIncome - totalExpense
                canvas.drawText("Net Balance", 55f + colWidth * 2, y + 24f, subPaint)
                canvas.drawText(
                    "${if (netBalance >= 0) "+" else ""}$currencySymbol ${String.format("%,.0f", netBalance)}",
                    55f + colWidth * 2,
                    y + 44f,
                    if (netBalance >= 0) greenPaint else redPaint
                )

                y += 80f

                // Category Breakdown Section
                if (categoryBreakdown.isNotEmpty()) {
                    canvas.drawText("Category Summary", 40f, y, headerPaint)
                    y += 16f

                    val totalCat = categoryBreakdown.values.sum().takeIf { it > 0 } ?: 1.0
                    categoryBreakdown.entries.sortedByDescending { it.value }.take(5).forEach { (catName, amount) ->
                        val pct = (amount / totalCat * 100).toInt()
                        canvas.drawText("• $catName", 50f, y + 10f, textPaint)
                        canvas.drawText("$pct%", 300f, y + 10f, subPaint)
                        canvas.drawText("$currencySymbol ${String.format("%,.0f", amount)}", 450f, y + 10f, boldTextPaint)
                        y += 16f
                    }
                    y += 10f
                    canvas.drawLine(40f, y, 555f, y, linePaint)
                    y += 18f
                }
            }

            // Transactions Table
            canvas.drawText("Transactions (${expenses.size} Total)", 40f, y, headerPaint)
            y += 16f

            // Table Header Bar
            canvas.drawRect(40f, y - 12f, 555f, y + 6f, boxPaint)
            canvas.drawText("Date", 50f, y, boldTextPaint)
            canvas.drawText("Title", 130f, y, boldTextPaint)
            canvas.drawText("Category", 280f, y, boldTextPaint)
            canvas.drawText("Wallet", 390f, y, boldTextPaint)
            canvas.drawText("Amount", 480f, y, boldTextPaint)
            y += 14f

            val startIdx = pageIndex * itemsPerPage
            val endIdx = minOf(startIdx + itemsPerPage, expenses.size)

            if (expenses.isEmpty()) {
                canvas.drawText("No transactions found for the selected period.", 50f, y + 12f, subPaint)
            } else {
                for (i in startIdx until endIdx) {
                    val exp = expenses[i]
                    val dStr = dateFormat.format(Date(exp.dateMillis))
                    val isIncome = exp.type == ExpenseType.INCOME
                    val amtStr = "${if (isIncome) "+" else "-"}$currencySymbol ${String.format("%,.0f", exp.amount)}"

                    val titleClipped = if (exp.title.length > 20) exp.title.take(18) + ".." else exp.title
                    val catClipped = if (exp.categoryName.length > 16) exp.categoryName.take(14) + ".." else exp.categoryName

                    canvas.drawText(dStr, 50f, y + 6f, subPaint)
                    canvas.drawText(titleClipped, 130f, y + 6f, textPaint)
                    canvas.drawText(catClipped, 280f, y + 6f, subPaint)
                    canvas.drawText(exp.wallet.displayName.split(" ").first(), 390f, y + 6f, subPaint)
                    canvas.drawText(amtStr, 480f, y + 6f, if (isIncome) greenPaint else redPaint)

                    y += 16f
                    canvas.drawLine(40f, y, 555f, y, linePaint)
                    y += 4f
                }
            }

            // Footer
            canvas.drawText("Generated by Smart Expense Tracker Android App", 40f, 815f, subPaint)
            canvas.drawText("Page ${pageIndex + 1} of $pagesCount", 500f, 815f, subPaint)

            document.finishPage(page)
        }

        document.writeTo(outputStream)
        outputStream.flush()
        document.close()
    }
}

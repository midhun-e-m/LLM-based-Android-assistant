package com.example.llmosassistant.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateStructuredPDF(
        context: Context,
        title: String,
        content: String
    ): File? {

        val document = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val paint = Paint()

        var y = margin
        val lineHeight = 24f
        var pageNumber = 1

        // ===== Draw Title =====

        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText(title, margin, y, paint)

        paint.textSize = 12f
        canvas.drawText("Page $pageNumber", pageWidth - 120f, pageHeight - 20f, paint)

        y += 40

        paint.textSize = 14f
        paint.isFakeBoldText = false

        val lines = content.split("\n")

        for (rawLine in lines) {

            var line = rawLine.trim()

            if (line.isEmpty()) {
                y += lineHeight
                continue
            }

            // ===== Heading Detection =====

            if (line.startsWith("# ")) {



                line = line.removePrefix("# ")

                paint.textSize = 20f
                paint.isFakeBoldText = true

                if (y + lineHeight > pageHeight - margin) {


                    val result = createNewPage(
                        document,
                        pageInfo,
                        title,
                        pageNumber + 1
                    )
                    page = result.first
                    canvas = result.second
                    pageNumber++
                    y = margin + 40
                }

                canvas.drawText(line, margin, y, paint)

                y += 30

                paint.textSize = 14f
                paint.isFakeBoldText = false

                continue
            }

            if (line.startsWith("## ")) {



                line = line.removePrefix("## ")

                paint.textSize = 17f
                paint.isFakeBoldText = true

                if (y + lineHeight > pageHeight - margin) {
                    document.finishPage(page)
                    val result = createNewPage(
                        document,
                        pageInfo,
                        title,
                        pageNumber + 1
                    )
                    page = result.first
                    canvas = result.second
                    pageNumber++
                    y = margin + 40
                }

                canvas.drawText(line, margin, y, paint)

                y += 25

                paint.textSize = 14f
                paint.isFakeBoldText = false

                continue
            }

            val wrappedLines = wrapText(line, 80)

            for (wrapped in wrappedLines) {

                if (y + lineHeight > pageHeight - margin) {
                    document.finishPage(page)

                    val result = createNewPage(
                        document,
                        pageInfo,
                        title,
                        pageNumber + 1
                    )

                    page = result.first
                    canvas = result.second
                    pageNumber++

                    y = margin + 40
                }

                canvas.drawText(wrapped, margin, y, paint)

                y += lineHeight
            }
        }

        document.finishPage(page)

        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "${title.replace(" ", "_")}.pdf"
        )

        document.writeTo(FileOutputStream(file))
        document.close()

        return file
    }

    // ===== PAGE CREATION =====

    private fun createNewPage(
        document: PdfDocument,
        pageInfo: PdfDocument.PageInfo,
        title: String,
        pageNumber: Int
    ): Pair<PdfDocument.Page, android.graphics.Canvas> {

        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()

        // Only show title on first page
        if (pageNumber == 1) {

            paint.textSize = 22f
            paint.isFakeBoldText = true

            canvas.drawText(title, 40f, 40f, paint)
        }

        paint.textSize = 12f
        paint.isFakeBoldText = false

        canvas.drawText("Page $pageNumber", 475f, 820f, paint)

        return Pair(page, canvas)
    }

    // ===== WORD WRAP =====

    private fun wrapText(
        text: String,
        maxChars: Int
    ): List<String> {

        val words = text.split(" ")

        val lines = mutableListOf<String>()

        var current = ""

        for (word in words) {

            val test = if (current.isEmpty()) word else "$current $word"

            if (test.length < maxChars) {
                current = test
            } else {
                lines.add(current)
                current = word
            }
        }

        if (current.isNotEmpty()) {
            lines.add(current)
        }

        return lines
    }
}
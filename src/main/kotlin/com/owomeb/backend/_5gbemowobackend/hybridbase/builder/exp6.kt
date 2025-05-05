package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.*
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import java.io.FileInputStream
import javax.swing.*
import javax.swing.border.EmptyBorder

fun XWPFRun.getAllDrawingUris2(): List<String> {
    return this.getCTR().drawingList.flatMap { drawing ->
        drawing.inlineList.map { it.graphic.graphicData.uri } +
                drawing.anchorList.map { it.graphic.graphicData.uri }
    }
}

fun XWPFRun.containsGraphic(): Boolean {
    return this.embeddedPictures.isNotEmpty() || this.getAllDrawingUris2().any { uri ->
        listOf("picture", "chart", "diagram", "graphic").any { uri.contains(it, ignoreCase = true) }
    }
}

fun collectAllRunsWithContext(doc: XWPFDocument): List<Pair<String, XWPFRun>> {
    val result = mutableListOf<Pair<String, XWPFRun>>()

    fun processRuns(runs: List<XWPFRun>, context: String) {
        for ((i, run) in runs.withIndex()) {
            result.add("[$context] run#$i" to run)
        }
    }

    fun processParagraphs(paragraphs: List<XWPFParagraph>, context: String) {
        for ((i, p) in paragraphs.withIndex()) {
            processRuns(p.runs, "$context → paragraph#$i")
        }
    }

    // Body
    processParagraphs(doc.paragraphs, "body")

    // Tables in body
    for ((tIdx, table) in doc.tables.withIndex()) {
        for ((rIdx, row) in table.rows.withIndex()) {
            for ((cIdx, cell) in row.tableCells.withIndex()) {
                processParagraphs(cell.paragraphs, "body → table$tIdx → row$rIdx → cell$cIdx")
            }
        }
    }

    // Headers
    for ((hIdx, header) in doc.headerList.withIndex()) {
        processParagraphs(header.paragraphs, "header$hIdx")
        for ((tIdx, table) in header.tables.withIndex()) {
            for ((rIdx, row) in table.rows.withIndex()) {
                for ((cIdx, cell) in row.tableCells.withIndex()) {
                    processParagraphs(cell.paragraphs, "header$hIdx → table$tIdx → row$rIdx → cell$cIdx")
                }
            }
        }
    }

    // Footers
    for ((fIdx, footer) in doc.footerList.withIndex()) {
        processParagraphs(footer.paragraphs, "footer$fIdx")
        for ((tIdx, table) in footer.tables.withIndex()) {
            for ((rIdx, row) in table.rows.withIndex()) {
                for ((cIdx, cell) in row.tableCells.withIndex()) {
                    processParagraphs(cell.paragraphs, "footer$fIdx → table$tIdx → row$rIdx → cell$cIdx")
                }
            }
        }
    }

    return result
}

fun main() {
    val docxPath = "src/main/resources/data/53/norm.docx"
    val doc = XWPFDocument(FileInputStream(docxPath))

    val allRunsWithContext = collectAllRunsWithContext(doc)
    val graphicRuns = allRunsWithContext.filter { it.second.containsGraphic() }

    SwingUtilities.invokeLater {
        val frame = JFrame("Skaner grafik w DOCX")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(1000, 700)
        frame.layout = BorderLayout()

        val textArea = JTextArea()
        textArea.font = Font("Monospaced", Font.PLAIN, 16)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val scrollPane = JScrollPane(textArea)
        frame.add(scrollPane, BorderLayout.CENTER)

        val infoArea = JTextArea(12, 30)
        infoArea.font = Font("SansSerif", Font.PLAIN, 12)
        infoArea.isEditable = false
        infoArea.border = EmptyBorder(10, 10, 10, 10)
        val infoPanel = JPanel(BorderLayout())
        infoPanel.add(JScrollPane(infoArea), BorderLayout.CENTER)
        frame.add(infoPanel, BorderLayout.EAST)

        val controlPanel = JPanel()
        val prevButton = JButton("Bliżej")
        val nextButton = JButton("Dalej")
        val indexField = JTextField(5)
        val goButton = JButton("Przejdź")
        controlPanel.add(prevButton)
        controlPanel.add(nextButton)
        controlPanel.add(JLabel("Index:"))
        controlPanel.add(indexField)
        controlPanel.add(goButton)
        frame.add(controlPanel, BorderLayout.SOUTH)

        var currentIndex = 0

        fun updateDisplay() {
            if (graphicRuns.isEmpty()) return
            val (context, run) = graphicRuns[currentIndex]
            val text = run.text() ?: "[PUSTY RUN]"
            textArea.text = text

            val props = StringBuilder()
            props.appendLine("Index: $currentIndex / ${graphicRuns.size - 1}")
            props.appendLine("Kontekst: $context")
            props.appendLine("Font: ${run.getFontFamily() ?: "brak"}")
            props.appendLine("Size: ${run.fontSize.takeIf { it > 0 } ?: "brak"}")
            props.appendLine("Bold: ${run.isBold}")
            props.appendLine("Italic: ${run.isItalic}")
            props.appendLine("Underline: ${run.underline.name}")
            props.appendLine("Color: ${run.color ?: "brak"}")
            props.appendLine("Obrazków (embedded): ${run.embeddedPictures.size}")
            props.appendLine("Drawing URIs:")
            run.getAllDrawingUris2().forEach { props.appendLine("• $it") }

            infoArea.text = props.toString()
        }

        prevButton.addActionListener {
            if (currentIndex > 0) {
                currentIndex--
                updateDisplay()
            }
        }

        nextButton.addActionListener {
            if (currentIndex < graphicRuns.size - 1) {
                currentIndex++
                updateDisplay()
            }
        }

        goButton.addActionListener {
            val idx = indexField.text.toIntOrNull()
            if (idx != null && idx in graphicRuns.indices) {
                currentIndex = idx
                updateDisplay()
            } else {
                JOptionPane.showMessageDialog(frame, "Nieprawidłowy indeks (0-${graphicRuns.size - 1})")
            }
        }

        indexField.addActionListener { goButton.doClick() }

        updateDisplay()
        frame.isVisible = true
    }
}
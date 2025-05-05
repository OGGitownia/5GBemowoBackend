package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFRun
import java.awt.BorderLayout
import java.awt.Font
import java.awt.GridLayout
import java.io.FileInputStream
import javax.swing.*
import javax.swing.border.EmptyBorder

fun XWPFRun.getAllDrawingUris(): List<String> {
    return this.getCTR().drawingList.flatMap { drawing ->
        drawing.inlineList.map { it.graphic.graphicData.uri } +
                drawing.anchorList.map { it.graphic.graphicData.uri }
    }
}

fun XWPFRun.containsGraphicElement(): Boolean {
    return this.getAllDrawingUris().any { uri ->
        listOf("picture", "chart", "diagram", "graphic").any { keyword ->
            uri.contains(keyword, ignoreCase = true)
        }
    }
}

fun main() {
    val docxPath = "src/main/resources/data/53/norm.docx"
    val runs = mutableListOf<XWPFRun>()
    val doc = XWPFDocument(FileInputStream(docxPath))
    for (para in doc.paragraphs) {
        runs.addAll(para.runs)
    }

    if (runs.isEmpty()) {
        println("Nie znaleziono żadnych runów w dokumencie.")
        return
    }

    SwingUtilities.invokeLater {
        val frame = JFrame("Podgląd runów w DOCX")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(900, 600)
        frame.layout = BorderLayout()

        val imageOnlyBox = JCheckBox("Tylko runy z grafiką (obrazki, wykresy, diagramy)")

        val topPanel = JPanel()
        topPanel.add(imageOnlyBox)
        frame.add(topPanel, BorderLayout.NORTH)

        val textArea = JTextArea()
        textArea.font = Font("Monospaced", Font.PLAIN, 16)
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
        val scrollPane = JScrollPane(textArea)
        frame.add(scrollPane, BorderLayout.CENTER)

        val infoArea = JTextArea(10, 30)
        infoArea.font = Font("SansSerif", Font.PLAIN, 12)
        infoArea.isEditable = false
        infoArea.border = EmptyBorder(10, 10, 10, 10)
        val infoPanel = JPanel(BorderLayout())
        infoPanel.add(JScrollPane(infoArea), BorderLayout.CENTER)
        frame.add(infoPanel, BorderLayout.EAST)

        val buttonPanel = JPanel()
        val prevButton = JButton("Bliżej")
        val nextButton = JButton("Dalej")
        val indexField = JTextField(5)
        val goButton = JButton("Przejdź")
        buttonPanel.add(prevButton)
        buttonPanel.add(nextButton)
        buttonPanel.add(JLabel("Index:"))
        buttonPanel.add(indexField)
        buttonPanel.add(goButton)

        val filterPanel = JPanel()
        val minLengthField = JTextField(4)
        val maxLengthField = JTextField(4)
        val confirmButton = JButton("Filtruj")
        filterPanel.add(JLabel("Min:"))
        filterPanel.add(minLengthField)
        filterPanel.add(JLabel("Max:"))
        filterPanel.add(maxLengthField)
        filterPanel.add(confirmButton)

        val southPanel = JPanel(GridLayout(2, 1))
        southPanel.add(filterPanel)
        southPanel.add(buttonPanel)
        frame.add(southPanel, BorderLayout.SOUTH)

        var currentIndex = 0
        var filteredRuns = runs.toList()

        fun refilterRuns() {
            val min = minLengthField.text.toIntOrNull() ?: 0
            val max = maxLengthField.text.toIntOrNull() ?: Int.MAX_VALUE

            filteredRuns = runs.filter {
                val lengthOk = it.text().length in min..max
                val imageOk = !imageOnlyBox.isSelected || it.embeddedPictures.isNotEmpty() || it.containsGraphicElement()
                lengthOk && imageOk
            }

            if (filteredRuns.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Brak runów spełniających warunki.")
                filteredRuns = runs.toList()
            }
            currentIndex = 0
        }

        fun updateDisplay() {
            if (filteredRuns.isEmpty()) return
            val run = filteredRuns[currentIndex]
            val text = run.text() ?: "[PUSTY RUN]"
            textArea.text = text

            val props = StringBuilder()
            props.appendLine("Index: $currentIndex / ${filteredRuns.size - 1}")
            props.appendLine("Font: ${run.getFontFamily() ?: "brak"}")
            props.appendLine("Length: ${run.text().length}")
            props.appendLine("Size: ${run.fontSize.takeIf { it > 0 } ?: "brak"}")
            props.appendLine("Bold: ${run.isBold}")
            props.appendLine("Italic: ${run.isItalic}")
            props.appendLine("Underline: ${run.underline.name}")
            props.appendLine("Strike: ${run.isStrike}")
            props.appendLine("Capitalized: ${run.isCapitalized}")
            props.appendLine("Color: ${run.color ?: "brak"}")
            props.appendLine("Highlight: ${run.textHightlightColor?.toString() ?: "brak"}")
            props.appendLine("Style: ${run.style ?: "brak"}")
            props.appendLine("Obrazków (embedded): ${run.embeddedPictures.size}")
            val uris = run.getAllDrawingUris()
            props.appendLine("Drawing URIs:")
            uris.forEach { props.appendLine("• $it") }

            infoArea.text = props.toString()
        }

        prevButton.addActionListener {
            if (currentIndex > 0) {
                currentIndex--
                updateDisplay()
            }
        }

        nextButton.addActionListener {
            if (currentIndex < filteredRuns.size - 1) {
                currentIndex++
                updateDisplay()
            }
        }

        goButton.addActionListener {
            val input = indexField.text.trim()
            val idx = input.toIntOrNull()
            if (idx != null && idx in filteredRuns.indices) {
                currentIndex = idx
                updateDisplay()
            } else {
                JOptionPane.showMessageDialog(frame, "Nieprawidłowy indeks (0-${filteredRuns.size - 1})")
            }
        }

        indexField.addActionListener {
            goButton.doClick()
        }

        confirmButton.addActionListener {
            refilterRuns()
            updateDisplay()
        }

        imageOnlyBox.addActionListener {
            refilterRuns()
            updateDisplay()
        }

        refilterRuns()
        updateDisplay()
        frame.isVisible = true
    }
}
package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.*
import java.awt.*
import java.io.FileInputStream
import javax.swing.*
import javax.swing.border.EmptyBorder





fun main() {
    val docxPath = "src/main/resources/data/53/norm.docx"

    val doc = XWPFDocument(FileInputStream(docxPath))
    val elements = mutableListOf<Any>()

    elements.addAll(doc.paragraphs)
    elements.addAll(doc.tables)
    elements.addAll(doc.allPictures)

    SwingUtilities.invokeLater {
        val frame = JFrame("Podgląd elementów DOCX")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.setSize(1000, 700)
        frame.layout = BorderLayout()

        val typePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val paragraphBox = JCheckBox("XWPFParagraph", true)
        val tableBox = JCheckBox("XWPFTable", true)
        val pictureBox = JCheckBox("XWPFPictureData", true)
        typePanel.add(paragraphBox)
        typePanel.add(tableBox)
        typePanel.add(pictureBox)
        frame.add(typePanel, BorderLayout.NORTH)

        val textArea = JTextArea()
        textArea.font = Font("Monospaced", Font.PLAIN, 14)
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

        fun getFilteredElements(): List<Any> {
            return elements.filter {
                (paragraphBox.isSelected && it is XWPFParagraph) ||
                        (tableBox.isSelected && it is XWPFTable) ||
                        (pictureBox.isSelected && it is XWPFPictureData)
            }
        }

        var filteredElements = getFilteredElements()
        var currentIndex = 0

        fun updateDisplay() {
            filteredElements = getFilteredElements()
            if (filteredElements.isEmpty()) {
                textArea.text = ""
                infoArea.text = "Brak elementów do wyświetlenia"
                return
            }
            if (currentIndex >= filteredElements.size) currentIndex = filteredElements.size - 1
            val element = filteredElements[currentIndex]
            val info = StringBuilder()
            info.appendLine("Typ elementu: ${element::class.simpleName}")
            info.appendLine("Index: $currentIndex / ${filteredElements.size - 1}")

            when (element) {
                is XWPFParagraph -> {
                    val text = element.text ?: "[PUSTY PARAGRAF]"
                    textArea.text = text
                    info.appendLine("Style: ${element.style ?: "brak"}")
                    info.appendLine("Alignment: ${element.alignment}")
                    info.appendLine("Runs: ${element.runs.size}")
                }
                is XWPFTable -> {
                    textArea.text = buildString {
                        for (row in element.rows) {
                            for (cell in row.tableCells) {
                                append(cell.text.trim()).append(" | ")
                            }
                            appendLine()
                        }
                    }
                    info.appendLine("Liczba wierszy: ${element.rows.size}")
                }
                is XWPFPictureData -> {
                    textArea.text = "[OBRAZEK: ${element.suggestFileExtension()}]"
                    info.appendLine("Typ: ${element.pictureType}")
                    info.appendLine("Rozmiar: ${element.data.size} bajtów")
                    info.appendLine("Nazwa sugerowana: ${element.fileName}")

                    val position = findPicturePositionInDocument(doc, element)
                    if (position != null) {
                        info.appendLine("Pozycja w dokumencie: $position")
                    } else {
                        info.appendLine("Pozycja: nieznaleziona w treści (obraz może być w nagłówku, stopce lub polu specjalnym)")
                    }
                }


            }

            infoArea.text = info.toString()
        }

        prevButton.addActionListener {
            if (currentIndex > 0) {
                currentIndex--
                updateDisplay()
            }
        }

        nextButton.addActionListener {
            if (currentIndex < filteredElements.size - 1) {
                currentIndex++
                updateDisplay()
            }
        }

        goButton.addActionListener {
            val input = indexField.text.trim()
            val idx = input.toIntOrNull()
            if (idx != null && idx in filteredElements.indices) {
                currentIndex = idx
                updateDisplay()
            } else {
                JOptionPane.showMessageDialog(frame, "Nieprawidłowy indeks (0-${filteredElements.size - 1})")
            }
        }

        indexField.addActionListener {
            goButton.doClick()
        }

        listOf(paragraphBox, tableBox, pictureBox).forEach {
            it.addActionListener {
                currentIndex = 0
                updateDisplay()
            }
        }

        updateDisplay()
        frame.isVisible = true
    }

}
fun findPicturePositionInDocument(doc: XWPFDocument, pictureData: XWPFPictureData): String? {
    var count = 0
    for (bodyElement in doc.bodyElements) {
        when (bodyElement) {
            is XWPFParagraph -> {
                for (run in bodyElement.runs) {
                    for (drawing in run.embeddedPictures) {
                        if (drawing.pictureData == pictureData) {
                            return "Akapit $count"
                        }
                    }
                }
                count++
            }

            is XWPFTable -> {
                for (row in bodyElement.rows) {
                    for (cell in row.tableCells) {
                        for (para in cell.paragraphs) {
                            for (run in para.runs) {
                                for (drawing in run.embeddedPictures) {
                                    if (drawing.pictureData == pictureData) {
                                        return "Tabela $count, komórka"
                                    }
                                }
                            }
                        }
                    }
                }
                count++
            }
        }
    }
    return null
}

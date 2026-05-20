package com.github.tjlee.huaweijdbcdriverspack

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

class LicenseAgreementDialog(project: Project?, private val licenses: List<LicenseInfo>) : DialogWrapper(project) {
    private val logger = Logger.getInstance(LicenseAgreementDialog::class.java)
    private val licensesByVendor = licenses.groupBy { it.vendorName }
    private val licenseTextArea = JTextArea()
    private val vendorList: JBList<String>

    init {
        title = "Huawei JDBC Drivers Pack — License Agreement"

        val vendorNames = licensesByVendor.keys.sorted()
        vendorList = JBList(vendorNames)
        vendorList.selectionMode = ListSelectionModel.SINGLE_SELECTION

        if (vendorNames.isNotEmpty()) {
            vendorList.selectedIndex = 0
        }

        vendorList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                updateLicenseDisplay()
            }
        }

        init()
        updateLicenseDisplay()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(900, 600)

        val headerPanel = JPanel()
        headerPanel.layout = BoxLayout(headerPanel, BoxLayout.Y_AXIS)
        headerPanel.border = BorderFactory.createEmptyBorder(0, 0, 10, 0)

        val desc1Label = JLabel("This plugin bundles JDBC drivers from various vendors with different licenses.")
        desc1Label.alignmentX = Component.LEFT_ALIGNMENT

        val desc2Label = JLabel("Please review the licenses below and click Accept to continue using this plugin.")
        desc2Label.alignmentX = Component.LEFT_ALIGNMENT

        val statsLabel = JLabel("${licensesByVendor.size} database vendors with ${licenses.size} license files found.")
        statsLabel.font = statsLabel.font.deriveFont(Font.BOLD)
        statsLabel.alignmentX = Component.LEFT_ALIGNMENT

        headerPanel.add(desc1Label)
        headerPanel.add(Box.createVerticalStrut(4))
        headerPanel.add(desc2Label)
        headerPanel.add(Box.createVerticalStrut(8))
        headerPanel.add(statsLabel)

        panel.add(headerPanel, BorderLayout.NORTH)

        val splitter = JBSplitter(false, 0.3f)

        val leftPanel = JPanel(BorderLayout())
        leftPanel.add(JLabel("Database Vendors:"), BorderLayout.NORTH)
        leftPanel.add(JBScrollPane(vendorList), BorderLayout.CENTER)
        splitter.firstComponent = leftPanel

        val rightPanel = JPanel(BorderLayout())
        rightPanel.add(JLabel("License Text:"), BorderLayout.NORTH)

        licenseTextArea.isEditable = false
        licenseTextArea.lineWrap = true
        licenseTextArea.wrapStyleWord = true
        licenseTextArea.font = licenseTextArea.font.deriveFont(12f)

        val scrollPane = JBScrollPane(licenseTextArea)
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        rightPanel.add(scrollPane, BorderLayout.CENTER)

        splitter.secondComponent = rightPanel
        panel.add(splitter, BorderLayout.CENTER)

        return panel
    }

    private fun updateLicenseDisplay() {
        val selectedVendor = vendorList.selectedValue ?: return
        val vendorLicenses = licensesByVendor[selectedVendor] ?: emptyList()

        val text = buildString {
            appendLine("═".repeat(80))
            appendLine("Vendor: $selectedVendor")
            appendLine("═".repeat(80))
            appendLine()

            vendorLicenses.forEach { license ->
                appendLine("─".repeat(80))
                appendLine("Driver Folder: ${license.driverName}")
                appendLine("Version: ${license.version}")
                appendLine("License File: ${license.fileName}")
                appendLine("─".repeat(80))
                appendLine()
                appendLine(license.content)
                appendLine()
                appendLine()
            }
        }

        licenseTextArea.text = text
        licenseTextArea.caretPosition = 0
    }

    override fun createActions(): Array<Action> {
        val acceptAction = object : DialogWrapperAction("Accept") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                logger.info("User accepted license agreement")
                close(OK_EXIT_CODE)
            }
        }

        val declineAction = object : DialogWrapperAction("Decline") {
            override fun doAction(e: java.awt.event.ActionEvent?) {
                logger.info("User declined license agreement")
                close(CANCEL_EXIT_CODE)
            }
        }

        return arrayOf(acceptAction, declineAction)
    }
}

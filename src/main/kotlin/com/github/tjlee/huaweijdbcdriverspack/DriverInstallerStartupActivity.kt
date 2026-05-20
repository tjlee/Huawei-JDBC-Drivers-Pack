package com.github.tjlee.huaweijdbcdriverspack

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DriverInstallerStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(DriverInstallerStartupActivity::class.java)
    private val LICENSE_ACCEPTED_KEY = "huawei.jdbc.drivers.pack.license.accepted"
    private val LICENSE_ACCEPTED_VERSION_KEY = "huawei.jdbc.drivers.pack.license.accepted.version"
    private val CURRENT_LICENSE_VERSION = "1.0"

    override suspend fun execute(project: Project) {
        val properties = PropertiesComponent.getInstance()
        val isAccepted = properties.getBoolean(LICENSE_ACCEPTED_KEY, false)
        val acceptedVersion = properties.getValue(LICENSE_ACCEPTED_VERSION_KEY, "")

        if (!isAccepted || acceptedVersion != CURRENT_LICENSE_VERSION) {
            logger.info("License not accepted or version mismatch, showing license dialog")

            val accepted = showLicenseDialog(project)

            if (!accepted) {
                logger.warn("User declined license agreement, cleaning up any existing drivers")
                service<DriverInstaller>().cleanupDrivers()
                return
            }

            properties.setValue(LICENSE_ACCEPTED_KEY, true)
            properties.setValue(LICENSE_ACCEPTED_VERSION_KEY, CURRENT_LICENSE_VERSION)
            logger.info("License accepted and stored")
        } else {
            logger.info("License already accepted (version: $acceptedVersion)")
        }

        service<DriverInstaller>().installDrivers()
        project.service<DriverSwitcher>().switchDriversForProject()
    }

    private suspend fun showLicenseDialog(project: Project): Boolean {
        val licenses = LicenseCollector().collectAllLicenses()

        if (licenses.isEmpty()) {
            logger.warn("No licenses found, proceeding without agreement")
            return true
        }

        logger.info("Found ${licenses.size} license files")

        var accepted = false
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = LicenseAgreementDialog(project, licenses)
            accepted = dialog.showAndGet()
        }

        return accepted
    }
}

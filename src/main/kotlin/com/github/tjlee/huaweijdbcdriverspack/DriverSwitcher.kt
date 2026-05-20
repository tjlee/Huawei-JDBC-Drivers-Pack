package com.github.tjlee.huaweijdbcdriverspack

import com.intellij.database.dataSource.LocalDataSource
import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.validation.DatabaseDriverValidator
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class DriverSwitcher(private val project: Project) {
    private val logger = Logger.getInstance(DriverSwitcher::class.java)

    fun switchDriversForProject() {
        try {
            val dataSourceManager = LocalDataSourceManager.getInstance(project)
            val dataSources = dataSourceManager.dataSources

            if (dataSources.isEmpty()) {
                logger.info("No data sources found in project, skipping driver switch")
                return
            }

            logger.info("Found ${dataSources.size} data sources, triggering driver switch")
            dataSources.forEach { switchDriverForDataSource(it) }
        } catch (e: Exception) {
            logger.error("Failed to switch drivers for project", e)
        }
    }

    private fun switchDriverForDataSource(dataSource: LocalDataSource) {
        try {
            val driver = dataSource.databaseDriver
            if (driver == null) {
                logger.info("No driver found for data source: ${dataSource.name}")
                return
            }

            val versions = driver.artifacts.filterIsInstance<DatabaseArtifactList.ArtifactVersion>()

            if (versions.isEmpty()) {
                logger.info("No artifact versions to download for data source: ${dataSource.name}")
                return
            }

            ProgressManager.getInstance().run(
                object : Task.Backgroundable(project, "Download Driver Files for ${dataSource.name}", false) {
                    override fun run(indicator: ProgressIndicator) {
                        for (version in versions) {
                            @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
                            val task = DatabaseDriverValidator.createDownloadTask(version, driver, null)
                            task.run(indicator)
                        }
                    }

                    override fun onSuccess() {
                        logger.info("Successfully switched driver for data source: ${dataSource.name}")
                    }

                    override fun onThrowable(error: Throwable) {
                        logger.warn("Failed to switch driver for data source: ${dataSource.name}", error)
                    }
                }
            )
        } catch (e: NoSuchMethodError) {
            logger.info("Driver switching API not available in this DataGrip version for: ${dataSource.name}")
        } catch (e: Exception) {
            logger.warn("Failed to switch driver for data source: ${dataSource.name}", e)
        }
    }
}

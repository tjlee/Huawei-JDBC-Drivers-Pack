package com.github.tjlee.huaweijdbcdriverspack

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.nio.file.*
import kotlin.io.path.*

@Service
class DriverInstaller {
    private val logger = Logger.getInstance(DriverInstaller::class.java)
    private val RESOURCE_BASE = "jdbc-drivers"
    private val SKIP_FILES = setOf(".DS_Store")

    fun installDrivers() {
        try {
            val configPath = PathManager.getConfigPath()
            val jdbcDriversPath = Path.of(configPath, "jdbc-drivers")

            logger.info("Installing JDBC drivers to: $jdbcDriversPath")

            Files.createDirectories(jdbcDriversPath)

            val classLoader = javaClass.classLoader
            installAllDrivers(classLoader, jdbcDriversPath)

            logger.info("JDBC drivers installation completed")
        } catch (e: Exception) {
            logger.error("Failed to install JDBC drivers", e)
        }
    }

    fun cleanupDrivers() {
        try {
            val configPath = PathManager.getConfigPath()
            val jdbcDriversPath = Path.of(configPath, "jdbc-drivers")

            if (!jdbcDriversPath.exists()) {
                logger.info("JDBC drivers directory does not exist, nothing to clean up")
                return
            }

            logger.info("Cleaning up JDBC drivers from: $jdbcDriversPath")
            jdbcDriversPath.toFile().deleteRecursively()
            logger.info("JDBC drivers cleanup completed")
        } catch (e: Exception) {
            logger.error("Failed to cleanup JDBC drivers", e)
        }
    }

    private fun installAllDrivers(classLoader: ClassLoader, targetBasePath: Path) {
        try {
            val resourceUrl = classLoader.getResource(RESOURCE_BASE)
            if (resourceUrl == null) {
                logger.warn("Resource directory '$RESOURCE_BASE' not found")
                return
            }

            when (resourceUrl.protocol) {
                "jar" -> installFromJar(resourceUrl, targetBasePath)
                "file" -> installFromFileSystem(Path.of(resourceUrl.toURI()), targetBasePath)
                else -> logger.warn("Unsupported protocol: ${resourceUrl.protocol}")
            }
        } catch (e: Exception) {
            logger.error("Failed to discover driver resources", e)
        }
    }

    private fun installFromJar(resourceUrl: java.net.URL, targetBasePath: Path) {
        val jarUri = URI.create(resourceUrl.toString().substringBefore("!") + "!/")

        FileSystems.newFileSystem(jarUri, mapOf<String, Any>()).use { fs ->
            val resourcePath = fs.getPath(RESOURCE_BASE)
            if (!Files.exists(resourcePath)) {
                logger.warn("Resource path does not exist in jar: $RESOURCE_BASE")
                return
            }
            copyRecursively(resourcePath, targetBasePath)
        }
    }

    private fun installFromFileSystem(sourcePath: Path, targetBasePath: Path) {
        copyRecursively(sourcePath, targetBasePath)
    }

    private fun copyRecursively(source: Path, target: Path) {
        Files.walk(source).forEach { sourcePath ->
            try {
                if (sourcePath == source) return@forEach

                val relativePath = source.relativize(sourcePath)
                if (shouldSkipFile(relativePath)) return@forEach

                val targetPath = target.resolve(relativePath.toString())

                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath)
                } else {
                    Files.createDirectories(targetPath.parent)

                    if (shouldCopyFile(sourcePath, targetPath)) {
                        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
                        logger.info("Installed: ${target.relativize(targetPath)}")
                    } else {
                        logger.debug("Skipping ${target.relativize(targetPath)} (already exists)")
                    }
                }
            } catch (e: Exception) {
                logger.warn("Failed to copy $sourcePath", e)
            }
        }
    }

    private fun shouldSkipFile(path: Path): Boolean {
        return path.any { component -> component.toString() in SKIP_FILES }
    }

    private fun shouldCopyFile(source: Path, target: Path): Boolean {
        if (!target.exists()) return true
        return try {
            Files.size(source) != Files.size(target)
        } catch (e: Exception) {
            true
        }
    }
}

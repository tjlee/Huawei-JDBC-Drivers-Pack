package com.github.tjlee.huaweijdbcdriverspack

import com.intellij.openapi.diagnostic.Logger
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

data class LicenseInfo(
    val driverName: String,
    val vendorName: String,
    val version: String,
    val fileName: String,
    val content: String
)

class LicenseCollector {
    private val logger = Logger.getInstance(LicenseCollector::class.java)
    private val RESOURCE_BASE = "jdbc-drivers"

    private val vendorNameMapping = mapOf(
        "Apache Derby" to "Apache Derby",
        "Arrow Flight" to "Apache Arrow Flight",
        "Athena" to "Amazon Athena",
        "AWS Aurora MySQL" to "Amazon Aurora MySQL",
        "BigQuery" to "Google BigQuery",
        "Cassandra" to "Apache Cassandra",
        "ClickHouse" to "ClickHouse",
        "Couchbase" to "Couchbase Query",
        "DB2" to "IBM Db2",
        "Dynamo" to "Amazon DynamoDB",
        "Elasticsearch" to "Elasticsearch",
        "Exasol" to "Exasol",
        "Firebird" to "Firebird",
        "GaussDB" to "GaussDB",
        "H2" to "H2",
        "HSQLDB" to "HSQLDB",
        "Hive" to "Apache Hive",
        "Informix" to "IBM Informix",
        "InterSystems IRIS" to "InterSystems IRIS",
        "JetBrains SQL Server" to "Microsoft SQL Server",
        "jTDS SQL Server and Sybase" to "Microsoft SQL Server / Sybase ASE",
        "jna-platform" to "JNA Platform (library)",
        "junixsocket-mysql" to "junixsocket MySQL (library)",
        "MariaDB Connector J" to "MariaDB",
        "MonetDB" to "MonetDB",
        "MongoDB" to "MongoDB",
        "MySQL ConnectorJ" to "MySQL",
        "OceanBase" to "OceanBase",
        "Oracle" to "Oracle Database",
        "PostgreSQL" to "PostgreSQL",
        "Presto" to "Presto",
        "Redis" to "Redis",
        "Redshift" to "Amazon Redshift",
        "SAP Hana" to "SAP Hana",
        "SAP jConnect" to "SAP Hana / Sybase ASE",
        "SingleStore" to "SingleStore",
        "Snowflake" to "Snowflake",
        "SQL Server" to "Microsoft SQL Server",
        "Tarantool" to "Tarantool",
        "Tibero" to "Tibero",
        "Trino" to "Trino",
        "Vertica" to "Vertica",
        "Xerial SQLiteJDBC" to "SQLite",
        "YugabyteDB" to "YugabyteDB"
    )

    fun collectAllLicenses(): List<LicenseInfo> {
        val licenses = mutableListOf<LicenseInfo>()

        try {
            val classLoader = javaClass.classLoader
            val resourceUrl = classLoader.getResource(RESOURCE_BASE)

            if (resourceUrl == null) {
                logger.warn("Resource directory '$RESOURCE_BASE' not found")
                return emptyList()
            }

            when (resourceUrl.protocol) {
                "jar" -> collectFromJar(resourceUrl, licenses)
                "file" -> collectFromFileSystem(Path.of(resourceUrl.toURI()), licenses)
                else -> logger.warn("Unsupported protocol: ${resourceUrl.protocol}")
            }
        } catch (e: Exception) {
            logger.error("Failed to collect licenses", e)
        }

        return licenses.sortedWith(compareBy({ it.vendorName }, { it.version }, { it.fileName }))
    }

    private fun collectFromJar(resourceUrl: java.net.URL, licenses: MutableList<LicenseInfo>) {
        val jarUri = URI.create(resourceUrl.toString().substringBefore("!") + "!/")

        FileSystems.newFileSystem(jarUri, mapOf<String, Any>()).use { fs ->
            val resourcePath = fs.getPath(RESOURCE_BASE)
            if (!Files.exists(resourcePath)) {
                logger.warn("Resource path does not exist in jar: $RESOURCE_BASE")
                return
            }
            scanForLicenses(resourcePath, licenses)
        }
    }

    private fun collectFromFileSystem(sourcePath: Path, licenses: MutableList<LicenseInfo>) {
        scanForLicenses(sourcePath, licenses)
    }

    private fun scanForLicenses(basePath: Path, licenses: MutableList<LicenseInfo>) {
        try {
            Files.walk(basePath)
                .filter { it.isRegularFile() }
                .filter { isLicenseFile(it) }
                .forEach { licensePath ->
                    try {
                        val relativePath = basePath.relativize(licensePath)
                        val parts = relativePath.toString().split("/")

                        if (parts.size >= 3) {
                            val driverName = parts[0]
                            val vendorName = vendorNameMapping[driverName] ?: driverName
                            val version = parts[1]
                            val fileName = parts.last()
                            val content = licensePath.readText()

                            licenses.add(LicenseInfo(driverName, vendorName, version, fileName, content))
                            logger.info("Found license: $vendorName ($driverName) $version - $fileName")
                        }
                    } catch (e: Exception) {
                        logger.warn("Failed to read license file: $licensePath", e)
                    }
                }
        } catch (e: Exception) {
            logger.error("Failed to scan for licenses", e)
        }
    }

    private fun isLicenseFile(path: Path): Boolean {
        val name = path.name.lowercase()
        val extension = path.extension.lowercase()

        return (name.startsWith("license") || name.contains("license")) ||
               (extension == "license") ||
               (name == "copying" || name == "copyright")
    }

    fun groupByDriver(licenses: List<LicenseInfo>): Map<String, List<LicenseInfo>> {
        return licenses.groupBy { it.vendorName }
    }
}

package com.github.tjlee.huaweijdbcdriverspack.auth

import com.intellij.credentialStore.OneTimeString
import com.intellij.database.access.DatabaseCredentials
import com.intellij.database.dataSource.DatabaseAuthProvider
import com.intellij.database.dataSource.DatabaseAuthProvider.ApplicabilityLevel
import com.intellij.database.dataSource.DatabaseAuthProvider.ApplicabilityLevel.PREFERRED
import com.intellij.database.dataSource.DatabaseAuthProvider.ApplicabilityLevel.Result
import com.intellij.database.dataSource.DatabaseConnectionConfig
import com.intellij.database.dataSource.DatabaseConnectionInterceptor.ProtoConnection
import com.intellij.database.dataSource.DatabaseConnectionPoint
import com.intellij.database.dataSource.DatabaseCredentialsAuthProvider
import com.intellij.database.dataSource.ui.AuthWidgetBuilder
import com.intellij.database.dataSource.ui.AuthWidgetBuilder.Serialiser
import com.intellij.database.dataSource.ui.AuthWidgetBuilder.adapt
import com.intellij.database.dataSource.ui.AuthWidgetBuilder.additionalPropertySerializer
import com.intellij.database.dataSource.ui.AuthWidgetBuilder.removeParameterHandler
import com.intellij.openapi.project.Project

private const val ACCESS_KEY_ID     = "AccessKeyID"
private const val SECRET_ACCESS_KEY = "SecretAccessKey"
private const val AUTO_CREATE       = "AutoCreate"
private const val DB_USER_PROP      = "DbUser"

class GaussDbIamAuthProvider : DatabaseAuthProvider {

    override fun getId(): String = "gaussdb.iam"

    override fun getDisplayName(): String = "GaussDB IAM (Access Key)"

    override fun getApplicability(point: DatabaseConnectionPoint, level: ApplicabilityLevel): Result {
        val url = point.url ?: return Result.NOT_APPLICABLE
        return when {
            url.startsWith("jdbc:dws:iam://") ->
                if (level < PREFERRED) Result.DEFAULT.clamp(level) else Result.PREFERRED
            url.startsWith("jdbc:gaussdb://") ->
                Result.DEFAULT.clamp(level)
            else -> Result.NOT_APPLICABLE
        }
    }

    override fun loadAuthConfig(
        point: DatabaseConnectionPoint,
        credentials: DatabaseCredentials?,
        external: Boolean,
    ): Any? {
        val accessKeyId = point.getAdditionalProperty(ACCESS_KEY_ID) ?: return null
        val secretKey   = if (external) credentials?.loadPassword(point, SECRET_ACCESS_KEY)?.toString() else null
        val dbUser      = point.getAdditionalProperty(DB_USER_PROP)
        val autoCreate  = point.getAdditionalProperty(AUTO_CREATE)?.toBoolean() ?: false
        return GaussDbIamConfig(accessKeyId, secretKey, dbUser, autoCreate)
    }

    override fun saveAuthConfig(
        config: DatabaseConnectionConfig,
        credentials: DatabaseCredentials?,
        data: Any?,
        external: Boolean,
    ) {
        val cfg = data as? GaussDbIamConfig ?: return
        config.setAdditionalProperty(ACCESS_KEY_ID, cfg.accessKeyId)
        config.setAdditionalProperty(DB_USER_PROP, cfg.dbUser)
        config.setAdditionalProperty(AUTO_CREATE, if (cfg.autoCreate) "true" else null)
        if (external) {
            credentials?.storePassword(config, SECRET_ACCESS_KEY, cfg.secretAccessKey?.let { OneTimeString(it) })
        }
    }

    override fun AuthWidgetBuilder.configureWidget(
        project: Project?,
        credentials: DatabaseCredentials,
        config: DatabaseConnectionConfig,
    ) {
        addTextField(
            { "Access Key ID" },
            additionalPropertySerializer(ACCESS_KEY_ID),
            removeParameterHandler(ACCESS_KEY_ID),
        )
        addPasswordField(
            { "Secret Access Key" },
            object : Serialiser<OneTimeString?> {
                override fun save(v: OneTimeString?, config: DatabaseConnectionConfig, creds: DatabaseCredentials?): Unit {
                    creds?.storePassword(config, SECRET_ACCESS_KEY, v)
                }
                override fun load(point: DatabaseConnectionPoint, creds: DatabaseCredentials?): OneTimeString? =
                    creds?.loadPassword(point, SECRET_ACCESS_KEY)
            },
            true,
            adapt(removeParameterHandler(SECRET_ACCESS_KEY), null as OneTimeString?),
        )
        addTextField(
            { "IAM Username (DbUser)" },
            additionalPropertySerializer(DB_USER_PROP),
            removeParameterHandler(DB_USER_PROP),
        )
        addCheckBox(
            { "Auto-create user" },
            adapt(additionalPropertySerializer(AUTO_CREATE), false),
            adapt(removeParameterHandler(AUTO_CREATE), false),
        )
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override suspend fun handleNullConnection(proto: ProtoConnection, silent: Boolean, attempt: Int): Boolean = false

    override suspend fun interceptConnection(proto: ProtoConnection, silent: Boolean): Boolean {
        DatabaseCredentialsAuthProvider.applyInitialCredentials(proto, this, { p, _ ->
            val point       = p.connectionPoint
            val accessKeyId = point.getAdditionalProperty(ACCESS_KEY_ID)
            val secretKey   = p.credentials.loadPassword(point, SECRET_ACCESS_KEY)?.toString()
            val dbUser      = point.getAdditionalProperty(DB_USER_PROP)
            val autoCreate  = point.getAdditionalProperty(AUTO_CREATE)?.toBoolean() ?: false

            if (!accessKeyId.isNullOrEmpty()) p.connectionProperties[ACCESS_KEY_ID]     = accessKeyId
            if (!secretKey.isNullOrEmpty())   p.connectionProperties[SECRET_ACCESS_KEY] = secretKey
            if (!dbUser.isNullOrEmpty())      p.connectionProperties[DB_USER_PROP]       = dbUser
            if (autoCreate)                   p.connectionProperties[AUTO_CREATE]        = "true"

            secretKey.isNullOrEmpty()
        }, silent)
        return true
    }
}

data class GaussDbIamConfig(
    val accessKeyId: String?,
    val secretAccessKey: String?,
    val dbUser: String?,
    val autoCreate: Boolean,
)

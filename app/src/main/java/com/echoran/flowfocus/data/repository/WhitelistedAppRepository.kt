package com.echoran.flowfocus.data.repository

import com.echoran.flowfocus.data.dao.WhitelistedAppDao
import com.echoran.flowfocus.data.model.WhitelistedAppEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhitelistedAppRepository @Inject constructor(
    private val dao: WhitelistedAppDao
) {
    fun getAllWhitelistedApps(): Flow<List<WhitelistedAppEntity>> {
        return dao.getAllWhitelistedApps()
    }

    suspend fun isAppWhitelisted(packageName: String): Boolean {
        return dao.isAppWhitelisted(packageName)
    }

    suspend fun addAppToWhitelist(app: WhitelistedAppEntity) {
        dao.insert(app)
    }

    suspend fun removeAppFromWhitelist(app: WhitelistedAppEntity) {
        dao.delete(app)
    }
}

package com.echoran.flowfocus.data.repository

import com.echoran.flowfocus.data.dao.FocusSessionDao
import com.echoran.flowfocus.data.model.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusSessionRepository @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) {

    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>> {
        // Default to very large range if needed or keep for backward compatibility
        return focusSessionDao.getSessionsInRange(0, Long.MAX_VALUE)
    }

    fun getSessionsInRange(start: Long, end: Long): Flow<List<FocusSessionEntity>> {
        return focusSessionDao.getSessionsInRange(start, end)
    }

    suspend fun insertFocusSession(session: FocusSessionEntity) {
        focusSessionDao.insert(session)
    }
}

package com.kairo.player.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.kairo.player.data.repository.LibraryRepository
import kotlinx.coroutines.CancellationException

class LibrarySyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            LibrarySyncWorkerEntryPoint::class.java,
        )
        return try {
            val result = dependencies.libraryRepository().triggerSync()
            if (result.succeeded) Result.success() else Result.retry()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LibrarySyncWorkerEntryPoint {
    fun libraryRepository(): LibraryRepository
}

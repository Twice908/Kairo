package com.kairo.player

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kairo.player.data.repository.LibraryRepository
import com.kairo.player.data.sync.LibrarySyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class KairoApplication : Application() {
	@Inject
	lateinit var libraryRepository: LibraryRepository

	override fun onCreate() {
		super.onCreate()
		val constraints = Constraints.Builder()
			.setRequiredNetworkType(NetworkType.CONNECTED)
			.build()
		val request = PeriodicWorkRequestBuilder<LibrarySyncWorker>(24, TimeUnit.HOURS)
			.setConstraints(constraints)
			.build()
		WorkManager.getInstance(this).enqueueUniquePeriodicWork(
			LIBRARY_SYNC_WORK_NAME,
			ExistingPeriodicWorkPolicy.KEEP,
			request,
		)
		CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
			libraryRepository.resyncIfStale()
		}
	}

	private companion object {
		const val LIBRARY_SYNC_WORK_NAME = "kairo_navidrome_library_sync"
	}
}
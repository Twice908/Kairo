package com.kairo.player.source.local

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.kairo.player.domain.model.AlbumArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SafLocalAudioDocumentStore(context: Context) : LocalAudioDocumentStore {
    private val appContext = context.applicationContext
    private val contentResolver = appContext.contentResolver
    private val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun persistTree(uri: String) {
        withContext(Dispatchers.IO) {
            val parsedUri = Uri.parse(uri)
            contentResolver.takePersistableUriPermission(
                parsedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            addSelection(TREE_SELECTIONS, uri)
        }
    }

    override suspend fun persistDocument(uri: String) {
        withContext(Dispatchers.IO) {
            val parsedUri = Uri.parse(uri)
            contentResolver.takePersistableUriPermission(
                parsedUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            addSelection(DOCUMENT_SELECTIONS, uri)
        }
    }

    override suspend fun removeSelection(uri: String) {
        withContext(Dispatchers.IO) {
            removeSelection(TREE_SELECTIONS, uri)
            removeSelection(DOCUMENT_SELECTIONS, uri)
            runCatching {
                contentResolver.releasePersistableUriPermission(
                    Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
        }
    }

    override suspend fun listDocuments(): List<LocalAudioDocument> = withContext(Dispatchers.IO) {
        val documents = linkedMapOf<String, LocalAudioDocument>()
        listBundledDemoDocuments().forEach { documents[it.uri] = it }
        selectedUris(TREE_SELECTIONS).forEach { treeUriString ->
            runCatching { addTreeDocuments(Uri.parse(treeUriString), documents) }
        }
        selectedUris(DOCUMENT_SELECTIONS).forEach { documentUri ->
            getDocumentOnIo(documentUri)?.let { documents[it.uri] = it }
        }
        documents.values.toList()
    }

    override suspend fun getDocument(uri: String): LocalAudioDocument? = withContext(Dispatchers.IO) {
        getBundledDemoDocument(uri) ?: getDocumentOnIo(uri)
    }

    override suspend fun getMetadata(uri: String): LocalAudioMetadata? = withContext(Dispatchers.IO) {
        if (Uri.parse(uri).scheme == DEMO_ASSET_SCHEME) return@withContext null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(appContext, Uri.parse(uri))
            LocalAudioMetadata(
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull(),
                albumArt = retriever.embeddedPicture?.let { AlbumArt(data = it.copyOf()) },
            )
        } catch (_: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun addTreeDocuments(
        treeUri: Uri,
        result: MutableMap<String, LocalAudioDocument>,
    ) {
        val visitedDocumentIds = mutableSetOf<String>()

        fun visit(parentDocumentId: String) {
            if (!visitedDocumentIds.add(parentDocumentId)) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
            contentResolver.query(
                childrenUri,
                TREE_PROJECTION,
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(idColumn) ?: continue
                    val mimeType = cursor.getString(mimeColumn)
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        visit(documentId)
                    } else {
                        val displayName = cursor.getString(nameColumn) ?: continue
                        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                        result[documentUri.toString()] = LocalAudioDocument(
                            uri = documentUri.toString(),
                            displayName = displayName,
                            mimeType = mimeType,
                        )
                    }
                }
            }
        }

        visit(DocumentsContract.getTreeDocumentId(treeUri))
    }

    private fun getDocumentOnIo(uriString: String): LocalAudioDocument? {
        val uri = Uri.parse(uriString)
        val displayName = contentResolver.query(
            uri,
            DOCUMENT_PROJECTION,
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (column < 0) null else cursor.getString(column)
        } ?: return null
        val mimeColumnValue = contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) null else cursor.getString(0)
        }
        return LocalAudioDocument(
            uri = uriString,
            displayName = displayName,
            mimeType = mimeColumnValue ?: contentResolver.getType(uri),
        )
    }

    private fun listBundledDemoDocuments(): List<LocalAudioDocument> =
        appContext.assets.list(DEMO_AUDIO_ASSET_DIRECTORY).orEmpty()
            .filter { SupportedAudioFormats.mimeType(it, null) != null }
            .map { displayName ->
                LocalAudioDocument(
                    uri = Uri.parse(DEMO_ASSET_URI_BASE).buildUpon().appendPath(displayName).build().toString(),
                    displayName = displayName,
                    mimeType = null,
                )
            }

    private fun getBundledDemoDocument(uriString: String): LocalAudioDocument? {
        val uri = Uri.parse(uriString)
        if (uri.scheme != DEMO_ASSET_SCHEME) return null
        val assetPath = uri.path?.removePrefix("/") ?: return null
        val fileName = assetPath.removePrefix("$DEMO_AUDIO_ASSET_DIRECTORY/")
        if (fileName.isEmpty() || fileName == assetPath || '/' in fileName) return null
        if (fileName !in appContext.assets.list(DEMO_AUDIO_ASSET_DIRECTORY).orEmpty()) return null
        return LocalAudioDocument(uriString, fileName, null)
    }

    private fun addSelection(key: String, uri: String) {
        preferences.edit()
            .putStringSet(key, selectedUris(key) + uri)
            .apply()
    }

    private fun removeSelection(key: String, uri: String) {
        preferences.edit()
            .putStringSet(key, selectedUris(key) - uri)
            .apply()
    }

    private fun selectedUris(key: String): Set<String> =
        preferences.getStringSet(key, emptySet()).orEmpty().toSet()

    private companion object {
        const val DEMO_AUDIO_ASSET_DIRECTORY = "demo_music"
        const val DEMO_ASSET_SCHEME = "asset"
        const val DEMO_ASSET_URI_BASE = "asset:///demo_music/"
        const val PREFERENCES_NAME = "local_audio_selections"
        const val TREE_SELECTIONS = "tree_uris"
        const val DOCUMENT_SELECTIONS = "document_uris"
        val TREE_PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        val DOCUMENT_PROJECTION = arrayOf(OpenableColumns.DISPLAY_NAME)
    }
}
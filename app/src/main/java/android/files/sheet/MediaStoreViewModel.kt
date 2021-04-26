package android.files.sheet

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import media.provider.AndroidMediaStore
import media.provider.constant.MediaStoreParam
import media.provider.handler.AndroidMediaHandler

class MediaStoreViewModel(private val app: Application) : AndroidViewModel(app) {

    private val androidMediaStore: AndroidMediaStore = AndroidMediaHandler(app)

    private var contentObserver: ContentObserver? = null

    fun loadImages(type: MediaStoreParam) {

        viewModelScope.launch {
            val loadImages = androidMediaStore.loadFiles(type)
            Log.i("loadImages---->>>> ", "$loadImages")
        }
    }

    fun loadFolders(type: MediaStoreParam) {

        viewModelScope.launch {
            val fileFolders = androidMediaStore.getFileFolders(type)
            Log.i("fileFolders---->>>> ", "$fileFolders")
        }
    }
}

/**
 * Convenience extension method to register a [ContentObserver] given a lambda.
 */
private fun ContentResolver.registerObserver(
    uri: Uri,
    observer: (selfChange: Boolean) -> Unit
): ContentObserver {
    val contentObserver = object : ContentObserver(Handler()) {
        override fun onChange(selfChange: Boolean) {
            observer(selfChange)
        }
    }
    registerContentObserver(uri, true, contentObserver)
    return contentObserver
}
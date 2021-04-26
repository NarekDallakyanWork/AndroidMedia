package android.files.sheet

import android.app.Application
import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import media.provider.AndroidMediaStore
import media.provider.constant.MediaStoreParam
import media.provider.handler.AndroidMediaHandler
import media.provider.model.MediaStoreBaseModel

class MediaStoreViewModel(private val app: Application) : AndroidViewModel(app) {

    companion object {
        const val TAG = "MediaStoreViewModel"
    }

    private val androidMediaStore: AndroidMediaStore = AndroidMediaHandler(app)
    private var contentObserver: ContentObserver? = null


    // Live data
    private var imagesLiveData = MutableLiveData<ArrayList<MediaStoreBaseModel>>()

    init {
        // Registering content provider observer
        initProviderObserver()
    }

    private fun initProviderObserver() {

        contentObserver = app.contentResolver.registerObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ) { selfChange ->

            Log.i(TAG, "Added new image file.")
        }
    }

    fun loadImages(type: MediaStoreParam) {

        viewModelScope.launch {
            val loadImages = androidMediaStore.loadFiles(type)
            Log.i(TAG, "$loadImages")

            // Changing value in [imagesLiveData]
            imagesLiveData.postValue(loadImages)
        }
    }

    fun loadFolders(type: MediaStoreParam) {

        viewModelScope.launch {
            val fileFolders = androidMediaStore.getFileFolders(type)
            Log.i(TAG, "$fileFolders")
        }
    }

    fun imagesLiveData() = imagesLiveData
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
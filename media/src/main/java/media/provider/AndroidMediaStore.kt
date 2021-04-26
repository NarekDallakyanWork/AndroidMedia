package media.provider

import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import media.provider.constant.MediaStoreParam
import media.provider.model.MediaStoreBaseModel
import media.provider.model.MediaStoreImage

interface AndroidMediaStore {

    suspend fun loadFiles(type: MediaStoreParam): ArrayList<MediaStoreBaseModel>

    suspend fun loadImages(): ArrayList<MediaStoreBaseModel>

    suspend fun loadVideos(): ArrayList<MediaStoreBaseModel>

    suspend fun getFileFolders(type: MediaStoreParam): LinkedHashMap<String, ArrayList<MediaStoreBaseModel>>

    /**
     * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
     * or delete items in MediaStore directly, and explicit permission
     * must usually be obtained to do this.
     *
     * The way it works is the OS will throw a [RecoverableSecurityException],
     * which we can catch here. Inside there's an [IntentSender] which the
     * activity can use to prompt the user to grant permission to the item
     * so it can be either updated or deleted.
     */
    /**
     * In [Build.VERSION_CODES.Q] and above, it isn't possible to modify
     * or delete items in MediaStore directly, and explicit permission
     * must usually be obtained to do this.
     *
     * The way it works is the OS will throw a [RecoverableSecurityException],
     * which we can catch here. Inside there's an [IntentSender] which the
     * activity can use to prompt the user to grant permission to the item
     * so it can be either updated or deleted.
     */
    suspend fun deleteImage(image: MediaStoreImage)

    suspend fun deletePendingImage()
}
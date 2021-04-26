package media.provider.model

import android.net.Uri

open class MediaStoreBaseModel {
    var contentUri: Uri? = null
    var id: Long? = null
    var displayName: String? = null
}
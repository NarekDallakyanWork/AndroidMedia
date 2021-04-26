package media.provider.model

import java.util.*

/**
 * Data class to hold information about an image included in the device's MediaStore.
 */
data class MediaStoreImage(
    val dateAdded: Date
) : MediaStoreBaseModel()
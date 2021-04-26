package media.provider.model

import java.util.*

/**
 * Data class to hold information about an image included in the device's MediaStore.
 */
data class MediaStoreImage(
    val dateAdded: Date,
    val imageWidth: Double = 0.0,
    val imageHeight: Double = 0.0
) : MediaStoreBaseModel()
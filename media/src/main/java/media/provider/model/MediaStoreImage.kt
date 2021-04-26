package media.provider.model

import java.util.*

/**
 * Data class to hold information about an image included in the device's MediaStore.
 */
data class MediaStoreImage(
    val dateAdded: Date,
    var originalWidth: Double = 0.0,
    var originalHeight: Double = 0.0,
    var compressedWidth: Double = 0.0,
    var compressedHeight: Double = 0.0
) : MediaStoreBaseModel()
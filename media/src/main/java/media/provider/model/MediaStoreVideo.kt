package media.provider.model

import java.util.*

// Container for information about each video.
data class MediaStoreVideo(
    var dateAdded: Date
) : MediaStoreBaseModel()
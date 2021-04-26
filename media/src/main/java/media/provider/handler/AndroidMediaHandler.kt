package media.provider.handler

import android.app.RecoverableSecurityException
import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import media.provider.AndroidMediaStore
import media.provider.constant.MediaStoreParam
import media.provider.model.MediaStoreBaseModel
import media.provider.model.MediaStoreImage
import media.provider.model.MediaStoreVideo
import media.provider.utilities.MediaStoreUtils
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap

class AndroidMediaHandler(private var mContext: Context) : AndroidMediaStore {
    private var pendingDeleteImage: MediaStoreImage? = null

    override suspend fun deleteImage(image: MediaStoreImage) {
        withContext(Dispatchers.IO) {
            try {
                image.contentUri?.let {
                    mContext.contentResolver.delete(
                        it,
                        "${MediaStore.Images.Media._ID} = ?",
                        arrayOf(image.id.toString())
                    )
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException
                            ?: throw securityException
                    pendingDeleteImage = image
                    recoverableSecurityException.userAction.actionIntent.intentSender
                } else {
                    throw securityException
                }
            }
        }
    }

    override suspend fun loadFiles(type: MediaStoreParam): ArrayList<MediaStoreBaseModel> {

        return when (type) {

            MediaStoreParam.IMAGE_TYPE -> {
                loadImages()
            }

            MediaStoreParam.VIDEO_TYPE -> {
                loadVideos()
            }

            MediaStoreParam.IMAGE_VIDEO_TYPE -> {

                val images = loadImages()
                val videos = loadVideos()
                images.addAll(videos)
                images
            }
        }
    }

    override suspend fun loadImages(): ArrayList<MediaStoreBaseModel> {

        val images: ArrayList<MediaStoreBaseModel> = arrayListOf()

        /**
         * Working with [ContentResolver]s can be slow, so we'll do this off the main
         * thread inside a coroutine.
         */
        withContext(Dispatchers.IO) {

            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT
            )

            /**
             * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
             * to omit this by passing `null` in its place, and then all rows will be returned.
             * In this case we're using a selection based on the date the image was taken.
             *
             * Note that we've included a `?` in our selection. This stands in for a variable
             * which will be provided by the next variable.
             */
            val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

            /**
             * The `selectionArgs` is a list of values that will be filled in for each `?`
             * in the `selection`.
             */
            val selectionArgs = arrayOf(
                MediaStoreUtils.dateToTimestamp(day = 22, month = 10, year = 2008).toString()
            )

            /**
             * Sort order to use. This can also be null, which will use the default sort
             * order. For [MediaStore.Images], the default sort order is ascending by date taken.
             */
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

            mContext.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->

                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)

                // Getting Image width and height indexes
                val imageWidthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val imageHeightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)


                Log.i("AndroidMediaHandlerTag", "Found ${cursor.count} images")
                while (cursor.moveToNext()) {

                    // Here we'll use the column indexs that we found above.
                    val id = cursor.getLong(idColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val displayName = cursor.getString(displayNameColumn)

                    // Here we getting image width `original` values from table
                    val imageWidth = cursor.getDouble(imageWidthIndex)
                    val imageHeight = cursor.getDouble(imageHeightIndex)



                    /**
                     * This is one of the trickiest parts:
                     *
                     * Since we're accessing images (using
                     * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI], we'll use that
                     * as the base URI and append the ID of the image to it.
                     *
                     * This is the exact same way to do it when working with [MediaStore.Video] and
                     * [MediaStore.Audio] as well. Whatever `Media.EXTERNAL_CONTENT_URI` you
                     * query to get the items is the base, and the ID is the document to
                     * request there.
                     */
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val imageItemModel = MediaStoreImage(dateModified)
                    imageItemModel.contentUri = contentUri
                    imageItemModel.displayName = displayName
                    imageItemModel.id = id
                    imageItemModel.originalWidth = imageWidth
                    imageItemModel.originalHeight = imageHeight

                    images += imageItemModel

                    // For debugging, we'll output the image objects we create to logcat.
                    Log.v("AndroidMediaHandlerTag", "Added image: $imageItemModel")
                }
                cursor.close()
            }
        }

        Log.v("AndroidMediaHandlerTag", "Found ${images.size} images")
        return images
    }

    override suspend fun loadVideos(): ArrayList<MediaStoreBaseModel> {

        val videoList = ArrayList<MediaStoreBaseModel>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        // Show only videos that are at least 5 minutes in duration.
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

        /**
         * The `selectionArgs` is a list of values that will be filled in for each `?`
         * in the `selection`.
         */
        val selectionArgs = arrayOf(
            // Release day of the G1. :)
            MediaStoreUtils.dateToTimestamp(day = 22, month = 10, year = 2008).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        withContext(Dispatchers.IO) {

            val query = mContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    // Stores column values and the contentUri in a local object
                    // that represents the media file.

                    val videoItem = MediaStoreVideo(dateModified)
                    videoItem.id = id
                    videoItem.dateAdded = dateModified
                    videoItem.displayName = name
                    videoItem.contentUri = contentUri

                    videoList.add(videoItem)
                }
            }
            query?.close()
        }
        return videoList
    }

    override suspend fun getFileFolders(type: MediaStoreParam): LinkedHashMap<String, ArrayList<MediaStoreBaseModel>> {
        var folders = LinkedHashMap<String, ArrayList<MediaStoreBaseModel>>()
        when (type) {

            MediaStoreParam.IMAGE_TYPE -> {
                folders = getImagesFolders()
            }

            MediaStoreParam.VIDEO_TYPE -> {
                val videoInsideFolders: LinkedHashMap<String, ArrayList<MediaStoreBaseModel>> =
                    LinkedHashMap()

                videoInsideFolders["Videos"] = getVideos()
            }

            MediaStoreParam.IMAGE_VIDEO_TYPE -> {

                val filesFolders: LinkedHashMap<String, ArrayList<MediaStoreBaseModel>> =
                    LinkedHashMap()

                filesFolders["Videos"] = getVideos()
                filesFolders.putAll(getImagesFolders())

                folders = filesFolders
            }
        }
        return folders
    }

    override suspend fun deletePendingImage() {

        pendingDeleteImage?.let { image ->
            pendingDeleteImage = null
            deleteImage(image)
        }
    }

    private suspend fun getImagesFolders(): LinkedHashMap<String, ArrayList<MediaStoreBaseModel>> {

        val photoInsideFolders: LinkedHashMap<String, ArrayList<MediaStoreBaseModel>> =
            LinkedHashMap()
        val projectionPhotos = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            if (Build.VERSION.SDK_INT > 28) MediaStore.Images.Media.DATE_MODIFIED else MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        withContext(Dispatchers.IO) {
            val cursor = MediaStore.Images.Media.query(
                mContext.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projectionPhotos,
                null,
                null,
                MediaStore.Images.Media.DATE_MODIFIED + " DESC"
            )

            val widthColumn = cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val imageIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)

            while (cursor.moveToNext()) {

                val folderName: String =
                    cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val path = cursor.getString(dataColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val imageId = cursor.getLong(imageIdColumn)
                val imageUriPath = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    imageId
                )

                if (photoInsideFolders.containsKey(folderName)) {

                    val imageItemModel = MediaStoreImage(Date())
                    imageItemModel.id = imageId
                    imageItemModel.contentUri = imageUriPath
                    imageItemModel.displayName = "Display name"

                    photoInsideFolders[folderName]
                        ?.add(imageItemModel)
                } else {
                    val dataList = ArrayList<MediaStoreBaseModel>()

                    val imageItemModel = MediaStoreImage(Date())
                    imageItemModel.id = imageId
                    imageItemModel.contentUri = imageUriPath
                    imageItemModel.displayName = "Display name"
                    dataList.add(imageItemModel)
                    photoInsideFolders[folderName] = dataList
                }
            }

            cursor.close()
            photoInsideFolders
        }
        return photoInsideFolders
    }

    private suspend fun getVideos(): ArrayList<MediaStoreBaseModel> {

        val videoList = ArrayList<MediaStoreBaseModel>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED
        )

        // Show only videos that are at least 5 minutes in duration.
        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"

        /**
         * The `selectionArgs` is a list of values that will be filled in for each `?`
         * in the `selection`.
         */
        val selectionArgs = arrayOf(
            // Release day of the G1. :)
            MediaStoreUtils.dateToTimestamp(day = 22, month = 10, year = 2008).toString()
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Video.Media.DISPLAY_NAME} ASC"

        withContext(Dispatchers.IO) {

            val query = mContext.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            query?.use { cursor ->
                // Cache column indices.
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext()) {
                    // Get values of columns for a given video.
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    val dateModified =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    // Stores column values and the contentUri in a local object
                    // that represents the media file.

                    val videoItem = MediaStoreVideo(dateModified)
                    videoItem.id = id
                    videoItem.dateAdded = dateModified
                    videoItem.displayName = name
                    videoItem.contentUri = contentUri

                    videoList.add(videoItem)
                }
            }
            query?.close()
        }
        return videoList
    }

}
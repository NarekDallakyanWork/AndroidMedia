package android.files.sheet

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import media.provider.constant.MediaStoreParam

class MainActivity : AppCompatActivity() {

    private val mediaStoreViewModel: MediaStoreViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init observers
        initObservers()

        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    mediaStoreViewModel.loadImages(MediaStoreParam.IMAGE_TYPE)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    token.continuePermissionRequest()
                }
            })
            .check()
    }

    /**
     *  Listening corresponds live data's
     */
    private fun initObservers() {

        mediaStoreViewModel.imagesLiveData().observe(this, { mediaImages ->

            Toast.makeText(this, "Images size:  ${mediaImages.size}", Toast.LENGTH_LONG).show()
        })
    }
}
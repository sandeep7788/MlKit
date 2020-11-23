package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.myapplication.databinding.ActivityMainBinding
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.MultiProcessor
import com.google.android.gms.vision.Tracker
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.FaceDetector
import com.google.android.material.snackbar.Snackbar
import java.io.*
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "@@FaceTrackerDemo"
    private var mCameraSource: CameraSource? = null
    private var mPreview: CameraSurfacePreview? = null
    public lateinit var cameraOverlay: CameraOverlay
    val RC_HANDLE_GMS = 9001
    val RC_HANDLE_CAMERA_PERM = 2
    lateinit var binding: ActivityMainBinding
    var camer_status=true
    var pageNumber = 0
    var pdfFileName: String? = null
    val SAMPLE_FILE = "android_tutorial.pdf"

    var mProgressDialog: ProgressDialog? = null
    var context: Context? = null
    var samplePDF = "http://www.africau.edu/images/default/sample.pdf"
    val PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this@MainActivity
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        mPreview = findViewById<View>(R.id.preview) as CameraSurfacePreview
        cameraOverlay = findViewById<View>(R.id.faceOverlay) as CameraOverlay
        val rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        if (rc == PackageManager.PERMISSION_GRANTED) {

            createCameraSource()
            startCameraSource()
        } else {
            requestCameraPermission();
        }


        showCamera()


        mProgressDialog = ProgressDialog(context)
        checkAndroidVersionCertificate()

    }

    private fun downloadFile() {
        mProgressDialog!!.show()
        mProgressDialog!!.setMessage("downloading")
        mProgressDialog!!.max = 100
        mProgressDialog!!.isIndeterminate = true
        mProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        mProgressDialog!!.setCancelable(true)
        val task =
            DownloadFileTask(this@MainActivity, samplePDF, "/download/pdf_file.pdf")
        task.startTask()
    }

    fun onFileDownloaded() {
        if (mProgressDialog!!.isShowing()) {
            mProgressDialog!!.dismiss()
        }
        val file = File(
            Environment.getExternalStorageDirectory()
                .absolutePath + "/download/pdf_file.pdf"
        )
        if (file.exists()) {
            binding.pdfView.fromFile(file) //.pages(0, 2, 1, 3, 3, 3) // all pages are displayed by default
                .enableSwipe(true).swipeHorizontal(true).enableDoubletap(true).defaultPage(0)
                .enableAnnotationRendering(true).password(null).scrollHandle(null)
                .onLoad(OnLoadCompleteListener {
                    binding.pdfView.setMinZoom(1f)
                    binding.pdfView.setMidZoom(5f)
                    binding.pdfView.setMaxZoom(10f)
                    binding.pdfView.zoomTo(2f)
                    binding.pdfView.scrollTo(100, 0)
                    binding.pdfView.moveTo(0f, 0f)
                }).swipeHorizontal(false).load()


        }
    }

    class DownloadFileTask(
        private val context: MainActivity,
        private val url: String,
        private val fileName: String
    ) {
        private var contentTask: GetTask? = null
        fun startTask() {
            doRequest()
        }

        private fun doRequest() {
            contentTask = GetTask()
            contentTask!!.execute()
        }

        private inner class GetTask :
            AsyncTask<String?, Int?, String?>() {


            override fun onPostExecute(data: String?) {
                context.onFileDownloaded()
            }

            override fun doInBackground(vararg p0: String?): String? {
                var count: Int
                try {
                    val _url = URL(url)
                    val conection = _url.openConnection()
                    conection.connect()
                    val extension =
                        url.substring(url.lastIndexOf('.') + 1).trim { it <= ' ' }
                    val input: InputStream =
                        BufferedInputStream(_url.openStream(), 8192)
                    val output: OutputStream = FileOutputStream(
                        Environment.getExternalStorageDirectory().toString() + fileName
                    )
                    val data = ByteArray(1024)
                    while (input.read(data).also { count = it } != -1) {
                        output.write(data, 0, count)
                    }
                    output.flush()
                    output.close()
                    input.close()
                } catch (e: Exception) {
                    Log.e("Error: ", e.message!!)
                }
                return null
            }
        }


        companion object {
            const val TAG = "DownloadFileTask"
        }

    }

    private fun checkAndroidVersionCertificate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission_Certificate()
        } else {
            if (!samplePDF.equals("", ignoreCase = true)) {
                downloadFile()
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun checkPermission_Certificate() {
        if (ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) + ContextCompat.checkSelfPermission(
                context!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    (context as Activity?)!!,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) || ActivityCompat.shouldShowRequestPermissionRationale(
                    (context as Activity?)!!,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE
                )
            } else {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE
                )
            }
        } else {  // write your logic code if permission already granted
            if (!samplePDF.equals("", ignoreCase = true)) {
                downloadFile()
            }
        }
    }

    fun showCamera() {
        val cal: Calendar = Calendar.getInstance()
        val minut: Int = cal.get(Calendar.MINUTE)
        Log.d(TAG, "getTime: "+minut.toString())

        val newtimer: CountDownTimer = object : CountDownTimer(1000000000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val c = Calendar.getInstance()
                Log.d(TAG, "getTime: "+c[Calendar.HOUR].toString() + ":" + c[Calendar.MINUTE] + ":" + c[Calendar.SECOND])

                Log.d(TAG, "onTick: "+c[Calendar.SECOND])

                if (c[Calendar.MINUTE].toString().endsWith("1")) {
                    camer_status=true
                }

                if(binding.cameraSwitch.isChecked) {
                    binding.layoutCamera.visibility=View.VISIBLE
                } else {
                    if (c[Calendar.MINUTE].toString().endsWith("2") && camer_status) {
                        binding.layoutCamera.visibility = View.VISIBLE
                        Log.d(TAG, "1>> Show camera")
                        camer_status = false

                    } else if (c[Calendar.SECOND].toString().endsWith("9")) {
                        binding.layoutCamera.visibility = View.GONE
                        Log.d(TAG, "1>> don't Show camera")
                    }
                }

            }

            override fun onFinish() {}
        }
        newtimer.start()
    }

    fun requestCameraPermission() {
        val permissions =
                arrayOf(Manifest.permission.CAMERA)
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                )
        ) {
            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    RC_HANDLE_CAMERA_PERM
            )
        } else {
            val thisActivity: Activity = this
            val listener =
                    View.OnClickListener {
                        ActivityCompat.requestPermissions(
                                thisActivity, permissions,
                                RC_HANDLE_CAMERA_PERM
                        )
                    }
            Snackbar.make(
                    cameraOverlay, "Camera permission is required",
                    Snackbar.LENGTH_INDEFINITE
            )
                    .setAction("OK", listener)
                    .show()
        }
    }
    override fun onResume() {
        super.onResume()
        startCameraSource()
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE -> if (grantResults.size > 0) {
                val writePermission =
                    grantResults[1] == PackageManager.PERMISSION_GRANTED
                val readExternalFile =
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
                if (writePermission && readExternalFile) {
                    if (!samplePDF.equals("", ignoreCase = true)) {
                        downloadFile()
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(
                            permissions[0]
                        )
                    ) {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri =
                            Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    } else {
                        requestPermissions(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ), PERMISSIONS_MULTIPLE_REQUEST_CERTIFICATE
                        )
                    }
                }
            }
        }
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(
                    TAG,
                    "Got unexpected permission result: $requestCode"
            )
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            return
        }
        if (grantResults.size != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(
                    TAG,
                    "Camera permission granted - initialize the camera source"
            )
            val activity: Activity = this@MainActivity
            createCameraSource()
            return
        }
        Log.e(
                TAG,
                "Permission not granted: results len = " + grantResults.size +
                        " Result code = " + if (grantResults.size > 0) grantResults[0] else "(empty)"
        )
        val listener =
                DialogInterface.OnClickListener { dialog, id -> finish() }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("FaceTrackerDemo")
                .setMessage("Need Camera access permission!")
                .setPositiveButton("OK", listener)
                .show()
    }

    private fun startCameraSource() {
        val code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                applicationContext)
        if (code != ConnectionResult.SUCCESS) {
            val dlg = GoogleApiAvailability.getInstance()
                    .getErrorDialog(this, code, RC_HANDLE_GMS)
            dlg.show()
        }
        if (mCameraSource != null) {
            try {
                mPreview!!.start(mCameraSource, cameraOverlay)
            } catch (e: IOException) {
                Log.e(TAG, "Unable to start camera source.", e)
                mCameraSource!!.release()
                mCameraSource = null
            }
        }
    }
    fun createCameraSource() {

        val context = applicationContext

        val detector =
                FaceDetector.Builder(context)
                        .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                        .build()
        detector.setProcessor(MultiProcessor.Builder(GraphicFaceTrackerFactory()).build())

        if (!detector.isOperational) {
            Log.e(TAG, "Face detector dependencies are not yet available.")
        }

        mCameraSource = CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(640, 480)
                .setFacing(CameraSource.CAMERA_FACING_FRONT)
                .setRequestedFps(30.0f)
                .build()
    }

    private inner class GraphicFaceTrackerFactory :
            MultiProcessor.Factory<Face> {
        override fun create(face: Face): GraphicFaceTracker {
            return GraphicFaceTracker(cameraOverlay)
        }
    }

    private inner class GraphicFaceTracker internal constructor(private val mOverlay: CameraOverlay) :
            Tracker<Face?>() {
        private val OPEN_THRESHOLD = 0.85f
        private val CLOSE_THRESHOLD = 0.15f

        private var state = 0
        private val faceOverlayGraphics: FaceOverlayGraphics
        override fun onNewItem(
                faceId: Int,
                item: Face?
        ) {
            faceOverlayGraphics.setId(faceId)

        }

        override fun onUpdate(
                detectionResults: Detector.Detections<Face?>, face: Face?) {
            var  left = face?.getIsLeftEyeOpenProbability();
            var right = face?.getIsRightEyeOpenProbability();
            var smilefloat:Float = 0.505848523F

            if ((left == Face.UNCOMPUTED_PROBABILITY) ||
                    (right == Face.UNCOMPUTED_PROBABILITY)) {
                // At least one of the eyes was not detected.
                Log.e("@@f","At least one of the eyes was not detected")

                return;
            }

            when (2) {
                0->
                    if ((left!! > OPEN_THRESHOLD) && (right !!> OPEN_THRESHOLD)) {
                        // Both eyes are initially open
                        Log.e("@@f1","Both eyes are initially open")
                        state = 1;
                    }

                1->
                    if ((left!! < CLOSE_THRESHOLD) && (right !!< CLOSE_THRESHOLD)) {
                        Log.e("@@f1","Both eyes become closed")
                        state = 2;
                    }

                2->
                    if ((left !!< OPEN_THRESHOLD) && (right !!> CLOSE_THRESHOLD) && (face!!.isSmilingProbability > smilefloat)) {
                        Log.e("@@fl", "Blink")
                    }
                    else
                    {
                        Log.e("@@f1","else part")
                    }
            }

            mOverlay.add(faceOverlayGraphics)
            faceOverlayGraphics.updateFace(face)
        }

        override fun onMissing(detectionResults: Detector.Detections<Face?>) {
            mOverlay.remove(faceOverlayGraphics)
        }

        override fun onDone() {
            mOverlay.remove(faceOverlayGraphics)
        }

        init {
            faceOverlayGraphics = FaceOverlayGraphics(mOverlay)
        }
    }

}

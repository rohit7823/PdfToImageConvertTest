package com.rohit.pdftoimageconverttest

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.PatternsCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.rohit.pdftoimageconverttest.ui.theme.PdfToImageConvertTestTheme
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.toByteArray
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.absolutePathString

val client = HttpClient(Android) {
    engine {
        // this: AndroidEngineConfig
        connectTimeout = 100_000
        socketTimeout = 100_000
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            PdfToImageConvertTestTheme {
                // A surface container using the 'background' color from the theme
                Content(
                    context = this@MainActivity,
                    "Download",
                    coroutineScope = lifecycleScope
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun Content(
    context: Context,
    name: String,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope
) {
    val images = remember {
        mutableStateListOf<Bitmap>()
    }

    val inputUrl = remember {
        mutableStateOf("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
    }

    val downloadProgress = remember {
        MutableStateFlow<Double?>(null)
    }

    val downloadProgressState = downloadProgress.collectAsState()

    val isInputValid = remember {
        mutableStateOf(false)
    }

    val activityResultLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.CreateDocument("image/jpeg")) { result ->
            context.saveDevice(images = images, createdUri = result)
        }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.ime
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(12.dp)
        ) {

            TextField(
                value = inputUrl.value,
                onValueChange = { inputUrl.value = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                placeholder = { Text(text = "Enter URL") },
                singleLine = true
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2f),
                contentAlignment = Alignment.Center
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    stickyHeader {
                        if (images.isNotEmpty()) {
                            ElevatedButton(
                                onClick = {
                                    activityResultLauncher.launch(
                                        input = "pdf_to_img_${System.currentTimeMillis()}",
                                        options = ActivityOptionsCompat.makeBasic()
                                    )
                                },
                                modifier = Modifier,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Save as Image")
                            }
                        }
                    }
                    items(
                        key = { it.hashCode() },
                        items = images
                    ) { bitmap ->
                        val state = rememberUpdatedState(newValue = bitmap.asImageBitmap())

                        Image(
                            bitmap = state.value,
                            contentDescription = null,
                            modifier = Modifier
                                .border(
                                    width = 1.dp,
                                    color = Color.Red,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                    }
                }

                CircularProgressBar(
                    percentage = downloadProgressState.value?.toFloat() ?: 0f,
                    isAnimating = downloadProgressState.value != null,
                    colors = listOf(Color.Red, Color.Red.copy(alpha = 0.5F), Color(0xFF707070)),
                    textColor = Color.Black,
                    radius = 70.dp
                )
            }
            Button(
                onClick = {
                    images.clear()
                    downloadProgress.update { null }
                    context.convertPdfToBitmap(
                        target = inputUrl.value,
                        coroutineScope = coroutineScope,
                        onBitmapCreated = {
                            images.addAll(it)
                        },
                        downloadingListener = { sent, total ->
                            downloadProgress.update {
                                (sent * 100f / total).toDouble()
                            }
                        }
                    )
                },
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                enabled = isInputValid.value
            ) {
                Text(text = name)
            }

        }
    }

    LaunchedEffect(inputUrl.value) {
        snapshotFlow {
            inputUrl.value
        }.flowOn(Dispatchers.Main.immediate)
            .collectLatest { input ->
                isInputValid.value =
                    PatternsCompat.WEB_URL.matcher(input).matches() && input.endsWith(".pdf")
            }
    }
}


private fun Context.saveDevice(
    images: List<Bitmap>,
    createdUri: Uri?
) {

    val imgFiles = images.map { imgFile ->
        File("${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$packageName/${imgFile.generationId}")
    }

    val uris = imgFiles.mapIndexed { index, file ->
        if (!file.exists()) {
            val contentValue = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }

            return@mapIndexed contentResolver.insert(
                /* url = */ MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                /* values = */ contentValue
            )?.apply {
                images[index].compress(
                    /* format = */ Bitmap.CompressFormat.JPEG,
                    /* quality = */ 100,
                    /* stream = */ contentResolver.openOutputStream(
                        /* uri = */ createdUri!!,
                        /* mode = */ "rw"
                    )
                )
            }
        } else {
            return@mapIndexed null
        }
    }




    if (uris.all { it != null }) {
        Toast.makeText(this, "Saved to Device", Toast.LENGTH_SHORT).show()
    }
}

private fun Context.convertPdfToBitmap(
    target: String,
    coroutineScope: CoroutineScope,
    onBitmapCreated: (List<Bitmap>) -> Unit,
    downloadingListener: (suspend (sent: Long, total: Long) -> Unit)? = null
) {
    val bitmaps = mutableListOf<Bitmap>()
    val coroutineExceptionHandler = CoroutineExceptionHandler { context, ex ->
        Toast.makeText(
            this@convertPdfToBitmap,
            "${ex.message}",
            Toast.LENGTH_LONG
        ).show()
    }


    coroutineScope.launch(coroutineExceptionHandler) {
        val byteChannel = client.get(urlString = target) {
            onDownload(downloadingListener)
        }.bodyAsChannel()
        val byteArray = byteChannel.toByteArray()

        val path = kotlin.io.path.createTempFile(prefix = "sample", suffix = ".pdf")

        val file = File(path.absolutePathString())
        file.writeBytes(byteArray)

        val pdfRenderer = PdfRenderer(
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        )
        repeat(pdfRenderer.pageCount) { idx ->
            val page = pdfRenderer.openPage(idx)
            val w = resources.displayMetrics.densityDpi / 72 * page.width
            val h = resources.displayMetrics.densityDpi / 72 * page.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            val mainBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            Canvas(mainBitmap).apply {
                drawColor(android.graphics.Color.WHITE)
                drawBitmap(bitmap, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
            }
            mainBitmap?.let {
                bitmaps.add(it)
            }
            page.close()
        }

        onBitmapCreated(bitmaps)
        pdfRenderer.close()
    }
}

package com.rohit.pdftoimageconverttest

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.rohit.pdftoimageconverttest.ui.theme.PdfToImageConvertTestTheme
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.statement.request
import io.ktor.util.Identity.decode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.path.absolutePathString
import kotlin.io.path.outputStream

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
        setContent {
            PdfToImageConvertTestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Greeting(
                            context = this@MainActivity,
                            "Download",
                            coroutineScope = lifecycleScope
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColumnScope.Greeting(
    context: Context,
    name: String,
    modifier: Modifier = Modifier,
    coroutineScope: CoroutineScope
) {
    val images = remember {
        mutableStateListOf<Bitmap>()
    }
    val inputUrl = remember {
        mutableStateOf("https://www.africau.edu/images/default/sample.pdf")
    }
    OutlinedTextField(
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

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(images) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp)
                        )
                )
            }
        }
    }
    Button(
        onClick = {
            context.convertPdfToBitmap(target = inputUrl.value, coroutineScope = coroutineScope) {
                images.addAll(it)
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(text = name)
    }

}

private fun Context.convertPdfToBitmap(
    target: String = "https://www.africau.edu/images/default/sample.pdf",
    coroutineScope: CoroutineScope,
    onBitmapCreated: (List<Bitmap>) -> Unit
) {
    val bitmaps = mutableListOf<Bitmap>()
    coroutineScope.launch {
        try {
            val inputStream = client.get(urlString = target).body<InputStream>()
            val path = kotlin.io.path.createTempFile(prefix = "sample", suffix = ".pdf")
            inputStream.use { input ->
                path.outputStream().use {
                    input.copyTo(it)
                }
            }
            val file = File(path.absolutePathString())
            Log.d(
                "TESTING",
                "File ${path.absolutePathString()} ${file.isFile} ${file.name} ${file.path}"
            )
            val pdfRenderer = PdfRenderer(
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            )
            repeat(pdfRenderer.pageCount) { idx ->
                val page = pdfRenderer.openPage(idx)
                val w = resources.displayMetrics.densityDpi / 72 * page.width
                val h = resources.displayMetrics.densityDpi / 72 * page.height
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                bitmap?.let {
                    bitmaps.add(it)
                }
                page.close()
            }
            onBitmapCreated(bitmaps)
            pdfRenderer.close()
        } catch (ex: Exception) {
            Toast.makeText(
                this@convertPdfToBitmap,
                "${ex.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}

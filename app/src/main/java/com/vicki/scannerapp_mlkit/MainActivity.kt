package com.vicki.scannerapp_mlkit

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.vicki.scannerapp_mlkit.ui.theme.ScannerAppMLkitTheme
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = this.getColor(R.color.black)

        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(2)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .setScannerMode(SCANNER_MODE_FULL)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)

        setContent {
            ScannerAppMLkitTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {

                    var imageUriList by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }

                    val scannerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->

                            if (result.resultCode == RESULT_OK) {
                                val result =
                                    GmsDocumentScanningResult.fromActivityResultIntent(result.data)

                                imageUriList = result?.pages?.map { it.imageUri } ?: emptyList()

                                result?.pdf.let { pdf ->
                                    val pdfFile = File(filesDir, "downloaded.pdf")

                                    val fos = FileOutputStream(pdfFile)

                                    pdf?.uri?.let {
                                        contentResolver.openInputStream(it)?.use { it ->
                                            it.copyTo(fos)
                                        }
                                    }


                                    val context = this

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            pdfFile
                                        )
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Scanned PDF")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Please find the scanned PDF attached."
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant temporary read permission
                                    }

                                    this.startActivity(Intent.createChooser(intent, "Send email"))


                                }
                            }

                        })

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        imageUriList.forEach { it ->
                            AsyncImage(
                                model = it,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        Button(onClick = {
                            scanner.getStartScanIntent(this@MainActivity)
                                .addOnSuccessListener { intentSender ->
                                    scannerLauncher.launch(
                                        IntentSenderRequest.Builder(intentSender).build()
                                    )
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Some Error ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }) {
                            Text(text = "Scan Doc")
                        }
                    }

                }
            }
        }
    }


}




package com.android.afreakyelfpdfviewer


import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import io.reactivex.BackpressureStrategy
import io.reactivex.android.schedulers.AndroidSchedulers.*
import io.reactivex.disposables.Disposables
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import java.io.File
import java.util.concurrent.TimeUnit


//private var download_file_url = "https://firebasestorage.googleapis.com/v0/b/voghdev-pdfviewpager.appspot.com/o/Chapter%201-converted.pdf?alt=media&token=978304a3-911e-4f48-8070-94374dc2b4cb"

class MainActivity : AppCompatActivity() {

    companion object {
        private const val FILE_NAME = "TestPdf.pdf"
        private const val URL = "https://firebasestorage.googleapis.com/v0/b/voghdev-pdfviewpager.appspot.com/o/english%2010th%20jac%20question%20bank.pdf?alt=media&token=323f2217-b9b2-4003-844d-778344b455c4"
    }

    private var disposable = Disposables.disposed()
    private var pdfReader: PdfReader? = null

    private val fileDownloader by lazy {
        FileDownloader(
            OkHttpClient.Builder().build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        RxJavaPlugins.setErrorHandler {
            Log.e("Error", it.localizedMessage)
        }

        pdf_view_pager.adapter = PageAdaptor()

        val targetFile = File(cacheDir, FILE_NAME)

        disposable = fileDownloader.download(URL, targetFile)
            .throttleFirst(2, TimeUnit.SECONDS)
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(mainThread())
            .subscribe({
                Toast.makeText(this, "$it% Downloaded", Toast.LENGTH_SHORT).show()
            }, {
                Toast.makeText(this, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }, {
                Toast.makeText(this, "Complete Downloaded", Toast.LENGTH_SHORT).show()
                pdfReader = PdfReader(targetFile).apply {
                    (pdf_view_pager.adapter as PageAdaptor).setupPdfRenderer(this)
                }
            })

        TabLayoutMediator(pdf_page_tab, pdf_view_pager) { tab, position ->
            tab.text = (position + 1).toString()
        }.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
        pdfReader?.close()
    }
}
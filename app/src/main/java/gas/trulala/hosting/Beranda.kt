package gas.trulala.hosting

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*

class Beranda : AppCompatActivity() {
    private var fileYangDipilih: File? = null
    private var modePotong: Boolean = false
    private var folderSaatIni: File? = null
    private var server: FileServer? = null
    private var userCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.beranda)

        val liner = findViewById<LinearLayout>(R.id.liner)
        val grid = findViewById<GridLayout>(R.id.grid)
        val kembali = findViewById<ImageView>(R.id.kembali)
        val folderBaru = findViewById<ImageView>(R.id.folderBaru)
        val fileBaru = findViewById<ImageView>(R.id.fileBaru)
        val tekanLama = findViewById<LinearLayout>(R.id.tekanLama)
        val hapus = findViewById<TextView>(R.id.hapus)
        val potong = findViewById<TextView>(R.id.potong)
        val salin = findViewById<TextView>(R.id.salin)
        val paste = findViewById<TextView>(R.id.paste)
        
        val pengguna = findViewById<TextView>(R.id.pengguna)
        val alamat = findViewById<TextView>(R.id.alamat)
        val mulaiHost = findViewById<Switch>(R.id.mulaiHost)
        
        mulaiHost.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                server = FileServer()
                server?.start()
                alamat.text = "http://${getLocalIpAddress()}:8080"
            } else {
                server?.stop()
                alamat.text = "Server mati"
                userCount = 0
                pengguna.text = "0 Pengguna"
            }
        }
        
        folderSaatIni = filesDir

        findViewById<ImageView>(R.id.nav).setOnClickListener {
            liner.visibility = if (liner.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        fun tampilkanFile(directory: File) {
            folderSaatIni = directory
            grid.removeAllViews()

            kembali.visibility = if (directory != filesDir) View.VISIBLE else View.GONE

            directory.listFiles()?.forEach { file ->
                val item = LayoutInflater.from(this).inflate(R.layout.item, grid, false)
                val gambar = item.findViewById<ImageView>(R.id.gambar)
                val nama = item.findViewById<TextView>(R.id.nama)

                nama.text = file.name
                gambar.setImageResource(if (file.isDirectory) R.drawable.folder else R.drawable.file)

                item.setOnClickListener {
                    if (file.isDirectory) {
                        tampilkanFile(file)
                    } else {
                        Toast.makeText(this, "Membuka ${file.name}", Toast.LENGTH_SHORT).show()
                    }
                }

                item.setOnLongClickListener {
                    tekanLama.visibility = View.VISIBLE
                    fileYangDipilih = file
                    true
                }

                grid.addView(item)
            }
        }

        tampilkanFile(filesDir)

        kembali.setOnClickListener {
            folderSaatIni?.parentFile?.let { parent ->
                tampilkanFile(parent)
            }
        }

        folderBaru.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.item_beranda, null)
            val tulis = view.findViewById<EditText>(R.id.tulis)

            AlertDialog.Builder(this)
                .setTitle("Buat Folder Baru")
                .setView(view)
                .setPositiveButton("Buat") { _, _ ->
                    val namaFolder = tulis.text.toString()
                    if (namaFolder.isNotEmpty()) {
                        val folderBaru = File(folderSaatIni, namaFolder)
                        if (!folderBaru.exists()) folderBaru.mkdirs()
                        tampilkanFile(folderSaatIni!!)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        fileBaru.setOnClickListener {
            val view = LayoutInflater.from(this).inflate(R.layout.tulis, null)
            val tulis = view.findViewById<EditText>(R.id.tulis)

            AlertDialog.Builder(this)
                .setTitle("Buat File Baru")
                .setView(view)
                .setPositiveButton("Buat") { _, _ ->
                    val namaFile = tulis.text.toString()
                    if (namaFile.isNotEmpty()) {
                        val fileBaru = File(folderSaatIni, namaFile)
                        if (!fileBaru.exists()) fileBaru.createNewFile()
                        tampilkanFile(folderSaatIni!!)
                    }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        hapus.setOnClickListener {
            fileYangDipilih?.let {
                if (it.deleteRecursively()) {
                    Toast.makeText(this, "${it.name} dihapus", Toast.LENGTH_SHORT).show()
                    tampilkanFile(folderSaatIni!!)
                }
            }
            tekanLama.visibility = View.GONE
        }

        potong.setOnClickListener {
            fileYangDipilih?.let {
                modePotong = true
                paste.visibility = View.VISIBLE
            }
        }

        salin.setOnClickListener {
            fileYangDipilih?.let {
                modePotong = false
                paste.visibility = View.VISIBLE
            }
        }

        paste.setOnClickListener {
            fileYangDipilih?.let { file ->
                val targetFile = File(folderSaatIni, file.name)

                if (modePotong) {
                    if (file.renameTo(targetFile)) {
                        Toast.makeText(this, "Dipindahkan ke ${targetFile.path}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    file.copyTo(targetFile, overwrite = true)
                    Toast.makeText(this, "Disalin ke ${targetFile.path}", Toast.LENGTH_SHORT).show()
                }

                tampilkanFile(folderSaatIni!!)
            }

            paste.visibility = View.GONE
            tekanLama.visibility = View.GONE
        }
    }

    inner class FileServer : NanoHTTPD(8080) {
        override fun serve(session: IHTTPSession): Response {
            userCount++ // Menambah jumlah pengguna
            runOnUiThread {
                findViewById<TextView>(R.id.pengguna).text = "$userCount Pengguna"
            }

            val fileDir = filesDir
            val files = fileDir.listFiles()?.joinToString("<br>") { it.name } ?: "Tidak ada file"

            return newFixedLengthResponse("<html><body>$files</body></html>")
        }
    }

    private fun getLocalIpAddress(): String {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { !it.isLoopbackAddress && it is InetAddress }?.hostAddress ?: "0.0.0.0"
        } catch (ex: Exception) {
            ex.printStackTrace()
            "0.0.0.0"
        }
    }
}
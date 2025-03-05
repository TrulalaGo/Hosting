package gas.trulala.hosting

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.*
import android.content.Intent
import java.io.File
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.utama)

        findViewById<TextView>(R.id.baru).setOnClickListener {
            val inflate = LayoutInflater.from(this).inflate(R.layout.tulis, null)
            val tulis = inflate.findViewById<EditText>(R.id.tulis)
            val buat = inflate.findViewById<Button>(R.id.buat)

            val dialog = AlertDialog.Builder(this)
                .setView(inflate)
                .create()

            buat.setOnClickListener {
                val nama = tulis.text.toString()
                val folder = File(filesDir, nama)

                if (!folder.exists()) folder.mkdirs()

                startActivity(Intent(this, Beranda::class.java))
                dialog.dismiss()
            }

            dialog.show()
        }

        findViewById<TextView>(R.id.buka).setOnClickListener {
            startActivity(Intent(this, Beranda::class.java))
        }
    }
}

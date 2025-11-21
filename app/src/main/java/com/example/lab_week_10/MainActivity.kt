package com.example.lab_week_10

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.lab_week_10.database.Total
import com.example.lab_week_10.database.TotalDatabase
import com.example.lab_week_10.database.TotalObject
import com.example.lab_week_10.viewmodels.TotalViewModel
import java.util.Date

class MainActivity : AppCompatActivity() {

    private val db by lazy { prepareDatabase() }

    private val viewModel by lazy {
        ViewModelProvider(this)[TotalViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initializeValueFromDatabase()
        prepareViewModel()
    }

    override fun onStart() {
        super.onStart()
        // show last updated date if available
        val curr = viewModel.total.value
        if (curr != null && curr.date.isNotEmpty()) {
            Toast.makeText(this, "Last updated: ${curr.date}", Toast.LENGTH_LONG).show()
        }
    }

    private fun prepareDatabase(): TotalDatabase {
        return Room.databaseBuilder(
            applicationContext,
            TotalDatabase::class.java,
            "total-database"
        )
            .allowMainThreadQueries() // kept for simplicity per lab instructions
            .fallbackToDestructiveMigration() // handle schema change for the exercise
            .build()
    }

    private fun initializeValueFromDatabase() {
        val list = db.totalDao().getTotal(ID)
        if (list.isEmpty()) {
            val initial = Total(
                id = ID,
                total = TotalObject(value = 0, date = "")
            )
            db.totalDao().insert(initial)
            viewModel.setTotal(initial.total)
        } else {
            viewModel.setTotal(list.first().total)
        }
    }

    private fun updateText(total: TotalObject) {
        findViewById<TextView>(R.id.text_total).text =
            getString(R.string.text_total, total.value)
    }

    private fun prepareViewModel() {
        viewModel.total.observe(this) { total ->
            updateText(total)
        }

        findViewById<Button>(R.id.button_increment).setOnClickListener {
            viewModel.incrementTotal()
        }
    }

    override fun onPause() {
        super.onPause()
        val curr = viewModel.total.value ?: TotalObject(0, "")
        val updated = TotalObject(value = curr.value, date = Date().toString())
        // save to DB (replace row with same ID)
        db.totalDao().update(Total(ID, updated))
        // update ViewModel so UI/Fragment will show latest date next onStart
        viewModel.setTotal(updated)
    }

    companion object {
        const val ID: Long = 1
    }
}

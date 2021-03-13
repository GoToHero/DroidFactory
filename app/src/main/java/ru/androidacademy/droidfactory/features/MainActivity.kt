package ru.androidacademy.droidfactory.features

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.replace
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.features.memesScreen.MemesScreenFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            routeToMemesScreen()
        }
    }

    private fun routeToMemesScreen() {
        supportFragmentManager.beginTransaction()
            .apply {
                replace<MemesScreenFragment>(R.id.container)
                commit()
            }
    }
}
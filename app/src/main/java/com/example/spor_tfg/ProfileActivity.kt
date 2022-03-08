package com.example.spor_tfg

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.example.spor_tfg.databinding.ActivityPaintBinding
import com.example.spor_tfg.databinding.ActivityProfileBinding
import com.google.android.material.navigation.NavigationView

class ProfileActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityProfileBinding

    lateinit var drawerLayout: DrawerLayout
    lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hooks
        navigationView = findViewById(R.id.profile_navigation_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Init toolbar
        initToolbar()

        // Preselect
        preselectToolbar()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.profile_toolbar)
        toolbar.title = "Spor"
        toolbar.title
        setSupportActionBar(findViewById<View>(R.id.profile_toolbar) as Toolbar)

        drawerLayout = findViewById<View>(R.id.profile_drawer_layout) as DrawerLayout
        val toggle: ActionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout as DrawerLayout?,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        (drawerLayout as DrawerLayout?)?.addDrawerListener(toggle)
        toggle.syncState()
    }

    private fun preselectToolbar() {
        val menuItem: MenuItem = navigationView.menu.getItem(1)
        menuItem.isChecked = true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_whiteboard -> {
                println("h")
                val intent = Intent(this, PaintActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_profile -> {
                true
            }
            else -> true
        }
    }
}
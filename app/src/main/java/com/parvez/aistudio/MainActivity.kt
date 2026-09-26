package com.parvez.aistudio

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import java.io.File

/**
 * Android AI Studio - Home & Project Manager
 * Architect: Parvez Mosharof
 */
class MainActivity : AppCompatActivity() {

    private lateinit var containerRecent: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkStoragePermission()

        containerRecent = findViewById(R.id.container_recent_projects)

        findViewById<MaterialCardView>(R.id.card_create_project).setOnClickListener {
            showCreateProjectDialog()
        }

        findViewById<Button>(R.id.btn_open_project).setOnClickListener {
            Toast.makeText(this, "Select a project from recent list", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btn_clone_git).setOnClickListener {
            Toast.makeText(this, "Git Clone will be available in next update", Toast.LENGTH_SHORT).show()
        }

        loadRecentProjects()
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun showCreateProjectDialog() {
        val dialog = Dialog(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_new_project, null)
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etName = dialogView.findViewById<EditText>(R.id.input_project_name)
        val etPackage = dialogView.findViewById<EditText>(R.id.input_package_name)
        val rb3D = dialogView.findViewById<RadioButton>(R.id.rb_template_3d)

        dialogView.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btn_create).setOnClickListener {
            val name = etName.text.toString().trim()
            val pkg = etPackage.text.toString().trim()
            val is3D = rb3D.isChecked

            if (name.isNotEmpty() && pkg.isNotEmpty()) {
                val projectDir = createProjectOnDisk(name, pkg, is3D)
                dialog.dismiss()
                openEditor(projectDir)
            } else {
                Toast.makeText(this, "সব তথ্য পূরণ করুন", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createProjectOnDisk(projectName: String, packageName: String, is3D: Boolean): File {
        val baseDir = File(Environment.getExternalStorageDirectory(), "AndroidAIStudioProjects")
        val projectDir = File(baseDir, projectName)
        val srcDir = File(projectDir, "app/src/main/java/${packageName.replace('.', '/')}")
        val resDir = File(projectDir, "app/src/main/res/layout")

        srcDir.mkdirs()
        resDir.mkdirs()

        // Create MainActivity file
        val mainActivityFile = File(srcDir, "MainActivity.kt")
        mainActivityFile.writeText(
            """
            package $packageName

            import android.os.Bundle
            import androidx.appcompat.app.AppCompatActivity

            class MainActivity : AppCompatActivity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    setContentView(R.layout.activity_main)
                }
            }
            """.trimIndent()
        )

        // Create Layout XML file
        val layoutFile = File(resDir, "activity_main.xml")
        layoutFile.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:gravity="center"
                android:orientation="VERTICAL"
                android:background="#131314">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Built with Android AI Studio"
                    android:textColor="#FFFFFF"
                    android:textSize="20sp"
                    android:textStyle="bold" />
            </LinearLayout>
            """.trimIndent()
        )

        return projectDir
    }

    private fun loadRecentProjects() {
        containerRecent.removeAllViews()
        val baseDir = File(Environment.getExternalStorageDirectory(), "AndroidAIStudioProjects")
        if (!baseDir.exists()) baseDir.mkdirs()

        val projects = baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

        if (projects.isEmpty()) {
            val tv = TextView(this).apply {
                text = "কোনো পূর্বের প্রজেক্ট নেই। Create New Project এ চাপুন।"
                setTextColor(Color.parseColor("#8E918F"))
                textSize = 13spToPx()
                setPadding(0, 16, 0, 16)
            }
            containerRecent.addView(tv)
            return
        }

        for (project in projects) {
            val card = MaterialCardView(this).apply {
                radius = 14f
                cardElevation = 0f
                setCardBackgroundColor(Color.parseColor("#1E1F20"))
                strokeWidth = 1
                strokeColor = Color.parseColor("#333538")
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 12) }
                layoutParams = lp

                val inner = LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(18, 16, 18, 16)

                    val title = TextView(this@MainActivity).apply {
                        text = project.name
                        setTextColor(Color.WHITE)
                        textSize = 15f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val path = TextView(this@MainActivity).apply {
                        text = project.absolutePath
                        setTextColor(Color.parseColor("#8E918F"))
                        textSize = 11f
                    }
                    addView(title)
                    addView(path)
                }
                addView(inner)

                setOnClickListener {
                    openEditor(project)
                }
            }
            containerRecent.addView(card)
        }
    }

    private fun openEditor(projectDir: File) {
        val intent = Intent(this, EditorActivity::class.java).apply {
            putExtra("EXTRA_PROJECT_PATH", projectDir.absolutePath)
        }
        startActivity(intent)
    }

    private fun TextView.textSizeSp(sp: Float) {
        textSize = sp
    }

    private fun Int.spToPx(): Float = this * resources.displayMetrics.scaledDensity
}

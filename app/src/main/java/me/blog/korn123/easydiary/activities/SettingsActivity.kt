package me.blog.korn123.easydiary.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import com.google.android.gms.drive.GoogleDriveDownloader
import com.google.android.gms.drive.GoogleDriveUploader
import com.xw.repo.BubbleSeekBar
import io.github.aafactory.commons.activities.BaseWebViewActivity
import io.github.aafactory.commons.helpers.BaseConfig
import kotlinx.android.synthetic.main.activity_settings.*
import me.blog.korn123.commons.utils.FontUtils
import me.blog.korn123.easydiary.BuildConfig
import me.blog.korn123.easydiary.R
import me.blog.korn123.easydiary.adapters.FontItemAdapter
import me.blog.korn123.easydiary.extensions.*
import me.blog.korn123.easydiary.helper.*
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.util.*

/**
 * Created by CHO HANJOONG on 2017-11-04.
 */

class SettingsActivity : EasyDiaryActivity() {
    private var mAlertDialog: AlertDialog? = null

    private val mOnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.primaryColor -> TransitionHelper.startActivityWithTransition(this@SettingsActivity, Intent(this@SettingsActivity, CustomizationActivity::class.java))
            R.id.fontSetting -> if (checkPermission(EXTERNAL_STORAGE_PERMISSIONS)) {
                openFontSettingDialog()
            } else {
                confirmPermission(EXTERNAL_STORAGE_PERMISSIONS, REQUEST_CODE_EXTERNAL_STORAGE_WITH_FONT_SETTING)
            }
            R.id.sensitiveOption -> {
                sensitiveOptionSwitcher.toggle()
                config.diarySearchQueryCaseSensitive = sensitiveOptionSwitcher.isChecked
            }
            R.id.addTtfFontSetting -> {
                openGuideView()
            }
            R.id.appLockSetting -> {
                appLockSettingSwitcher.toggle()
                config.aafPinLockEnable = appLockSettingSwitcher.isChecked
            }
            R.id.lockNumberSetting -> {
                val lockSettingIntent = Intent(this@SettingsActivity, LockSettingActivity::class.java)
                startActivityForResult(lockSettingIntent, REQUEST_CODE_LOCK_SETTING)
            }
            R.id.restoreSetting -> {
                mTaskFlag = SETTING_FLAG_IMPORT_GOOGLE_DRIVE
                if (checkPermission(EXTERNAL_STORAGE_PERMISSIONS)) {
                    // API Level 22 이하이거나 API Level 23 이상이면서 권한취득 한경우
                    val downloadIntent = Intent(this@SettingsActivity, GoogleDriveDownloader::class.java)
                    startActivity(downloadIntent)
                } else {
                    // API Level 23 이상이면서 권한취득 안한경우
                    confirmPermission(EXTERNAL_STORAGE_PERMISSIONS, REQUEST_CODE_EXTERNAL_STORAGE)
                }
            }
            R.id.restorePhotoSetting -> {
                openGuideView()
            }
            R.id.backupSetting -> {
                mTaskFlag = SETTING_FLAG_EXPORT_GOOGLE_DRIVE
                if (checkPermission(EXTERNAL_STORAGE_PERMISSIONS)) {
                    // API Level 22 이하이거나 API Level 23 이상이면서 권한취득 한경우
                    openUploadIntent()
                } else {
                    // API Level 23 이상이면서 권한취득 안한경우
                    confirmPermission(EXTERNAL_STORAGE_PERMISSIONS, REQUEST_CODE_EXTERNAL_STORAGE)
                }
            }
            R.id.rateAppSetting -> openGooglePlayBy("me.blog.korn123.easydiary")
            R.id.licenseView -> {
                val licenseIntent = Intent(this, WebViewActivity::class.java)
                licenseIntent.putExtra(BaseWebViewActivity.OPEN_URL_INFO, "https://github.com/hanjoongcho/aaf-easydiary/blob/master/LICENSE.md")
                startActivity(licenseIntent)
            }
            R.id.easyPhotoMap -> openGooglePlayBy("me.blog.korn123.easyphotomap")
            R.id.easyPassword -> openGooglePlayBy("io.github.hanjoongcho.easypassword")
        }
    }
    
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setTitle(R.string.settings)
            setDisplayHomeAsUpEnabled(true)
        }

        bindEvent()
    }

    private fun bindEvent() {
        primaryColor.setOnClickListener(mOnClickListener)
        fontSetting.setOnClickListener(mOnClickListener)
        sensitiveOption.setOnClickListener(mOnClickListener)
        addTtfFontSetting.setOnClickListener(mOnClickListener)
        appLockSetting.setOnClickListener(mOnClickListener)
        lockNumberSetting.setOnClickListener(mOnClickListener)
        restoreSetting.setOnClickListener(mOnClickListener)
        backupSetting.setOnClickListener(mOnClickListener)
        rateAppSetting.setOnClickListener(mOnClickListener)
        licenseView.setOnClickListener(mOnClickListener)
        easyPhotoMap.setOnClickListener(mOnClickListener)
        easyPassword.setOnClickListener(mOnClickListener)
        restorePhotoSetting.setOnClickListener(mOnClickListener)

        fontLineSpacing.configBuilder
                .min(0.2F)
                .max(1.8F)
                .progress(config.lineSpacingScaleFactor)
                .floatType()
                .sectionCount(16)
                .sectionTextInterval(2)
                .showSectionText()
                .sectionTextPosition(BubbleSeekBar.TextPosition.BELOW_SECTION_MARK)
                .autoAdjustSectionMark()
                .build()


        val bubbleSeekBarListener = object : BubbleSeekBar.OnProgressChangedListener {
            override fun onProgressChanged(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {
                Log.i("progress", "$progress $progressFloat")
                config.lineSpacingScaleFactor = progressFloat
                setFontsStyle()
                Log.i("progress", "${config.lineSpacingScaleFactor}")
            }
            override fun getProgressOnActionUp(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float) {}
            override fun getProgressOnFinally(bubbleSeekBar: BubbleSeekBar?, progress: Int, progressFloat: Float, fromUser: Boolean) {}
        }
        fontLineSpacing.setOnProgressChangedListener(bubbleSeekBarListener)
    }

    override fun onResume() {
        super.onResume()
        initPreference()
        setFontsStyle()
        setupInvite()
        
        if (BaseConfig(this).isThemeChanged) {
            BaseConfig(this).isThemeChanged = false
            val readDiaryIntent = Intent(this, DiaryMainActivity::class.java)
            readDiaryIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(readDiaryIntent)
            this.overridePendingTransition(0, 0)
        }
    }

    private fun setupInvite() {
        inviteSummary.text = String.format(getString(R.string.invite_friends_summary), getString(R.string.app_name))
        invite.setOnClickListener {
            val text = String.format(getString(io.github.aafactory.commons.R.string.share_text), getString(R.string.app_name), getStoreUrl())
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
                startActivity(Intent.createChooser(this, getString(io.github.aafactory.commons.R.string.invite_via)))
            }
        }
    }

    private fun openGuideView() {
        val guideIntent = Intent(this, WebViewActivity::class.java)
        guideIntent.putExtra(BaseWebViewActivity.OPEN_URL_INFO, getString(R.string.add_ttf_fonts_info_url))
        startActivity(guideIntent)
    }
    
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            data?.let {
                val password = it.getStringExtra(APP_LOCK_REQUEST_PASSWORD)
                config.aafPinLockSavedPassword = password
                lockNumberSettingSummary.text = "${getString(R.string.lock_number)} $password"
            }
        }
        config.aafPinLockPauseMillis = System.currentTimeMillis()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_EXTERNAL_STORAGE -> if (checkPermission(EXTERNAL_STORAGE_PERMISSIONS)) {
                // 권한이 있는경우
                if (mTaskFlag == SETTING_FLAG_EXPORT_GOOGLE_DRIVE) {
                    //                            FileUtils.copyFile(new File(EasyDiaryDbHelper.getRealmInstance().getPath()), new File(Path.WORKING_DIRECTORY + Path.DIARY_DB_NAME));
                    openUploadIntent()
                } else if (mTaskFlag == SETTING_FLAG_IMPORT_GOOGLE_DRIVE) {
                    val downloadIntent = Intent(applicationContext, GoogleDriveDownloader::class.java)
                    startActivity(downloadIntent)
                }
            } else {
                // 권한이 없는경우
                makeSnackBar(findViewById(android.R.id.content), getString(R.string.guide_message_3))
            }
            REQUEST_CODE_EXTERNAL_STORAGE_WITH_FONT_SETTING -> if (checkPermission(EXTERNAL_STORAGE_PERMISSIONS)) {
                openFontSettingDialog()
            } else {
                makeSnackBar(findViewById(android.R.id.content), getString(R.string.guide_message_3))
            }
            else -> {
            }
        }
    }
    
    private fun openUploadIntent() {
        // delete unused compressed photo file 
        File(Environment.getExternalStorageDirectory().absolutePath + DIARY_PHOTO_DIRECTORY).listFiles()?.map {
//            Log.i("PHOTO-URI", "${it.absolutePath} | ${EasyDiaryDbHelper.countPhotoUriBy(FILE_URI_PREFIX + it.absolutePath)}")
            if (EasyDiaryDbHelper.countPhotoUriBy(FILE_URI_PREFIX + it.absolutePath) == 0) it.delete()
        }
        
        val uploadIntent = Intent(applicationContext, GoogleDriveUploader::class.java)
        startActivity(uploadIntent)
    }

    private fun openFontSettingDialog() {
        val builder = AlertDialog.Builder(this@SettingsActivity)
        builder.setNegativeButton(getString(android.R.string.cancel), null)
        builder.setTitle(getString(R.string.font_setting))
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val fontView = inflater.inflate(R.layout.dialog_fonts, null)
        val listView = fontView.findViewById<ListView>(R.id.listFont)

        val fontNameArray = resources.getStringArray(R.array.pref_list_fonts_title)
        val fontPathArray = resources.getStringArray(R.array.pref_list_fonts_values)
        val listFont = ArrayList<Map<String, String>>()
        var selectedIndex = 0
        for (i in fontNameArray.indices) {
            val map = HashMap<String, String>()
            map.put("disPlayFontName", fontNameArray[i])
            map.put("fontName", fontPathArray[i])
            listFont.add(map)
        }

        val fontDir = File(Environment.getExternalStorageDirectory().absolutePath + USER_CUSTOM_FONTS_DIRECTORY)
        fontDir.list()?.let {
            for (fontName in it) {
                if (FilenameUtils.getExtension(fontName).equals("ttf", ignoreCase = true)) {
                    val map = HashMap<String, String>()
                    map.put("disPlayFontName", FilenameUtils.getBaseName(fontName))
                    map.put("fontName", fontName)
                    listFont.add(map)
                }
            }
        }
        
        listFont.mapIndexed { index, map ->
            if (config.settingFontName == map["fontName"]) selectedIndex = index
        } 
        
        val arrayAdapter = FontItemAdapter(this@SettingsActivity, R.layout.item_font, listFont)
        listView.adapter = arrayAdapter
        listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            val fontInfo = parent.adapter.getItem(position) as HashMap<String, String>
            fontInfo["fontName"]?.let { 
                config.settingFontName = it
                FontUtils.setCommonTypeface(this@SettingsActivity, assets)
                initPreference()
                setFontsStyle()
            }
            mAlertDialog?.cancel()
        }

        builder.setView(fontView)
        mAlertDialog = builder.create()
        mAlertDialog?.show()
        listView.setSelection(selectedIndex)
    }

    private fun setFontsStyle() {
        FontUtils.setFontsTypeface(applicationContext, assets, null, findViewById<ViewGroup>(android.R.id.content))
    }

    private fun initPreference() {
        fontSettingSummary.text = FontUtils.fontFileNameToDisplayName(applicationContext, config.settingFontName)
        sensitiveOptionSwitcher.isChecked = config.diarySearchQueryCaseSensitive
        appLockSettingSwitcher.isChecked = config.aafPinLockEnable
        lockNumberSettingSummary.text = "${getString(R.string.lock_number)} ${config.aafPinLockSavedPassword}"
        rateAppSettingSummary.text = String.format("Easy Diary v %s", BuildConfig.VERSION_NAME)
    }

    private fun getStoreUrl() = "https://play.google.com/store/apps/details?id=$packageName"

    companion object {
        private var mTaskFlag = 0
    }
}
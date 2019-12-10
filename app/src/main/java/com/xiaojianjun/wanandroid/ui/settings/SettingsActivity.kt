package com.xiaojianjun.wanandroid.ui.settings

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.xiaojianjun.wanandroid.BuildConfig
import com.xiaojianjun.wanandroid.R
import com.xiaojianjun.wanandroid.common.SeekBarChangeListenerAdapter
import com.xiaojianjun.wanandroid.ext.setNavigationBarColor
import com.xiaojianjun.wanandroid.ext.showToast
import com.xiaojianjun.wanandroid.model.bean.Article
import com.xiaojianjun.wanandroid.model.store.SettingsStore
import com.xiaojianjun.wanandroid.ui.base.BaseVmActivity
import com.xiaojianjun.wanandroid.ui.detail.DetailActivity
import com.xiaojianjun.wanandroid.ui.detail.DetailActivity.Companion.PARAM_ARTICLE
import com.xiaojianjun.wanandroid.ui.login.LoginActivity
import com.xiaojianjun.wanandroid.util.clearCache
import com.xiaojianjun.wanandroid.util.core.ActivityManager
import com.xiaojianjun.wanandroid.util.getCacheSize
import com.xiaojianjun.wanandroid.util.isNightMode
import com.xiaojianjun.wanandroid.util.setNightMode
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.view_change_text_zoom.view.*

@SuppressLint("SetTextI18n")
class SettingsActivity : BaseVmActivity<SettingsViewModel>() {

    override fun layoutRes() = R.layout.activity_settings

    override fun viewModelClass() = SettingsViewModel::class.java

    override fun initView() {

        setNavigationBarColor(getColor(R.color.bgColorScondary))

        scDayNight.isChecked = isNightMode(this)
        tvFontSize.text = "${SettingsStore.getWebTextZoom()}%"
        tvClearCache.text = getCacheSize(this)
        tvAboutUs.text = getString(R.string.current_version, BuildConfig.VERSION_NAME)

        ivBack.setOnClickListener { ActivityManager.finish(SettingsActivity::class.java) }
        scDayNight.setOnCheckedChangeListener { _, checked ->
            setNightMode(checked)
            SettingsStore.setNightMode(checked)
        }
        llFontSize.setOnClickListener {
            setFontSize()
        }
        llClearCache.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.confirm_clear_cache)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    clearCache(this)
                    tvClearCache.text = getCacheSize(this)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }
        llCheckVersion.setOnClickListener {
            // TODO 检查版本
            showToast(getString(R.string.stay_tuned))
        }
        llAboutUs.setOnClickListener {
            ActivityManager.start(
                DetailActivity::class.java,
                mapOf(
                    PARAM_ARTICLE to Article(
                        title = getString(R.string.abount_us),
                        link = "https://github.com/xiaoyanger0825/wanandroid"
                    )
                )
            )
        }
        tvLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    mViewModel.logout()
                    ActivityManager.start(LoginActivity::class.java)
                    ActivityManager.finish(SettingsActivity::class.java)
                }
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .show()
        }
    }

    private fun setFontSize() {
        val textZoom = SettingsStore.getWebTextZoom()
        var tempTextZoom = textZoom
        AlertDialog.Builder(this)
            .setTitle(R.string.font_size)
            .setView(LayoutInflater.from(this).inflate(R.layout.view_change_text_zoom, null).apply {
                seekBar.progress = textZoom - 50
                seekBar.setOnSeekBarChangeListener(SeekBarChangeListenerAdapter(
                    onProgressChanged = { _, progress, _ ->
                        tempTextZoom = 50 + progress
                        tvFontSize.text = "$tempTextZoom%"
                    }
                ))
            })
            .setNegativeButton(R.string.cancel) { _, _ ->
                tvFontSize.text = "$textZoom%"
            }
            .setPositiveButton(R.string.confirm) { _, _ ->
                SettingsStore.setWebTextZoom(tempTextZoom)
            }
            .show()

    }

    override fun initData() {
        mViewModel.getLoginStatus()
    }

    override fun observe() {
        super.observe()
        mViewModel.isLogin.observe(this, Observer {
            tvLogout.isVisible = it
        })
    }
}

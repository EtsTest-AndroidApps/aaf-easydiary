package me.blog.korn123.easydiary.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import androidx.appcompat.app.AppCompatActivity
import io.github.aafactory.commons.utils.CommonUtils
import me.blog.korn123.commons.utils.FontUtils
import me.blog.korn123.easydiary.databinding.ActivityIntroBinding
import me.blog.korn123.easydiary.extensions.*
import me.blog.korn123.easydiary.helper.*
import java.util.*

/**
 * Created by CHO HANJOONG on 2016-12-31.
 */

class IntroActivity : AppCompatActivity() { 
    private lateinit var mBinding: ActivityIntroBinding
    private lateinit var mHandler: Handler
    override fun onResume() {
        super.onResume()
        mBinding.root.run {
            FontUtils.setFontsTypeface(this@IntroActivity, assets, null, this)
            initTextSize(this)
            setBackgroundColor(config.primaryColor)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityIntroBinding.inflate(layoutInflater)
        forceInitRealmLessThanOreo()
        setContentView(mBinding.root)
        rescheduleEnabledAlarms()

        FontUtils.checkFontSetting(this)

        mHandler = object: Handler(this.mainLooper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    START_MAIN_ACTIVITY -> {
                        TransitionHelper.startActivityWithTransition(this@IntroActivity, Intent(this@IntroActivity, DiaryMainActivity::class.java))
                        finish()
                    }
                    else -> {}
                }
            }
        }.apply { sendEmptyMessageDelayed(START_MAIN_ACTIVITY, 500) }
    }
}

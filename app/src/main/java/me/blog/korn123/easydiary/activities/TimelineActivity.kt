package me.blog.korn123.easydiary.activities

import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import io.github.aafactory.commons.utils.DateUtils
import me.blog.korn123.commons.utils.EasyDiaryUtils
import me.blog.korn123.commons.utils.FlavorUtils
import me.blog.korn123.easydiary.R
import me.blog.korn123.easydiary.adapters.TimelineItemAdapter
import me.blog.korn123.easydiary.databinding.ActivityTimelineBinding
import me.blog.korn123.easydiary.extensions.changeDrawableIconColor
import me.blog.korn123.easydiary.extensions.config
import me.blog.korn123.easydiary.extensions.initTextSize
import me.blog.korn123.easydiary.extensions.openFeelingSymbolDialog
import me.blog.korn123.easydiary.helper.*
import me.blog.korn123.easydiary.models.Diary
import java.util.*


/**
 * Created by hanjoong on 2017-07-16.
 */

class TimelineActivity : EasyDiaryActivity() {

    /***************************************************************************************************
     *   global properties
     *
     ***************************************************************************************************/
    private lateinit var mBinding: ActivityTimelineBinding
    private lateinit var mSDatePickerDialog: DatePickerDialog
    private lateinit var mEDatePickerDialog: DatePickerDialog
    private var mTimelineItemAdapter: TimelineItemAdapter? = null
    private var mDiaryList: ArrayList<Diary> = arrayListOf()
    private var mFirstTouch = 0F
    private val mCalendar = Calendar.getInstance(Locale.getDefault())
    private var mSymbolSequence = SYMBOL_SELECT_ALL

    
    /***************************************************************************************************
     *   override functions
     *
     ***************************************************************************************************/
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityTimelineBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.run {
            title = getString(R.string.timeline_title)
            setDisplayHomeAsUpEnabled(true)
        }

        changeDrawableIconColor(config.primaryColor, R.drawable.ic_calendar_4_w)

        mTimelineItemAdapter = TimelineItemAdapter(this, R.layout.item_timeline, mDiaryList)
        mBinding.timelineList.adapter = mTimelineItemAdapter

        setupTimelineSearch()

        bindEvent()
        initTextSize(mBinding.partialTimelineFilter.root)


        when (savedInstanceState) {
            null -> {
                mSDatePickerDialog = DatePickerDialog(this, mStartDateListener, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
                mEDatePickerDialog = DatePickerDialog(this, mEndDateListener, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
                refreshList()
                moveListViewScrollToBottom()
            }
            else -> {
                val filterSYear = savedInstanceState.getInt(FILTER_START_YEAR, mCalendar.get(Calendar.YEAR))
                val filterSMonth = savedInstanceState.getInt(FILTER_START_MONTH, mCalendar.get(Calendar.MONTH))
                val filterSDate = savedInstanceState.getInt(FILTER_START_DATE, mCalendar.get(Calendar.DAY_OF_MONTH))
                if (savedInstanceState.getBoolean(FILTER_START_ENABLE, false))  {
                    Log.i("aaf-t" , "get date $filterSYear $filterSMonth $filterSDate")
                    mBinding.partialTimelineFilter.startDate.text = DateUtils.getFullPatternDate(EasyDiaryUtils.datePickerToTimeMillis(filterSDate, filterSMonth, filterSYear))
                }

                val filterEYear = savedInstanceState.getInt(FILTER_START_YEAR, mCalendar.get(Calendar.YEAR))
                val filterEMonth = savedInstanceState.getInt(FILTER_START_MONTH, mCalendar.get(Calendar.MONTH))
                val filterEDate = savedInstanceState.getInt(FILTER_START_DATE, mCalendar.get(Calendar.DAY_OF_MONTH))
                if (savedInstanceState.getBoolean(FILTER_END_ENABLE, false))  {
                    mBinding.partialTimelineFilter.endDate.text = DateUtils.getFullPatternDate(EasyDiaryUtils.datePickerToTimeMillis(filterEDate, filterEMonth, filterEYear))
                }

                mSDatePickerDialog = DatePickerDialog(this, mStartDateListener, filterSYear, filterSMonth, filterSDate)
                mEDatePickerDialog = DatePickerDialog(this, mEndDateListener, filterEYear, filterEMonth, filterEDate)


                if (savedInstanceState.getBoolean(FILTER_VIEW_VISIBLE, false)) toggleFilterView(true)

                // refreshList call from onTextChanged listener
                mBinding.partialTimelineFilter.query.setText(savedInstanceState.getString(FILTER_QUERY, ""))

//                val itemIndex = EasyDiaryUtils.sequenceToPageIndex(mDiaryList, savedInstanceState.getInt(DIARY_SEQUENCE, -1))
//                if (itemIndex > 0) {
//                    Log.i("aaf-t" , "DIARY_SEQUENCE ${savedInstanceState.getInt(DIARY_SEQUENCE, -1)}")
//                    Log.i("aaf-t" , "index $itemIndex / ${mDiaryList.size}")
//                    Handler().post { timelineList.setSelection(itemIndex)}
//                }
            }
        }

        selectFeelingSymbol()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mDiaryList.isNotEmpty()) {
            outState.putInt(DIARY_SEQUENCE, mDiaryList[mBinding.timelineList.firstVisiblePosition].sequence)
            Log.i("aaf-t" , "firstVisiblePosition ${mBinding.timelineList.firstVisiblePosition}")
        }

        if (mBinding.partialTimelineFilter.startDate.text.isNotEmpty()) {
            outState.putBoolean(FILTER_START_ENABLE, true)
            outState.putInt(FILTER_START_YEAR, mSDatePickerDialog.datePicker.year)
            outState.putInt(FILTER_START_MONTH, mSDatePickerDialog.datePicker.month)
            outState.putInt(FILTER_START_DATE, mSDatePickerDialog.datePicker.dayOfMonth)
            Log.i("aaf-t" , "set date ${mSDatePickerDialog.datePicker.year} ${mSDatePickerDialog.datePicker.month} ${mSDatePickerDialog.datePicker.dayOfMonth}")
        }

        if (mBinding.partialTimelineFilter.endDate.text.isNotEmpty()) {
            outState.putBoolean(FILTER_END_ENABLE, true)
            outState.putInt(FILTER_END_YEAR, mEDatePickerDialog.datePicker.year)
            outState.putInt(FILTER_END_MONTH, mEDatePickerDialog.datePicker.month)
            outState.putInt(FILTER_END_DATE, mEDatePickerDialog.datePicker.dayOfMonth)
        }

        if (mBinding.partialTimelineFilter.filterView.translationY == 0F) outState.putBoolean(FILTER_VIEW_VISIBLE, true)

        outState.putString(FILTER_QUERY, mBinding.partialTimelineFilter.query.text.toString())

        Log.i("aaf-t" , "translationY ${mBinding.partialTimelineFilter.filterView.translationY}")

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        if (config.previousActivity == PREVIOUS_ACTIVITY_CREATE ) {
            refreshList()
            moveListViewScrollToBottom()
            config.previousActivity = -1
        } else {
            refreshList()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_timeline, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search -> {
                toggleFilterView(true)
//                toolbar.visibility = View.GONE
//                searchViewContainer.visibility = View.VISIBLE
//                searchView.requestFocus()
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    
    /***************************************************************************************************
     *   etc functions
     *
     ***************************************************************************************************/
    private fun bindEvent() {
        mBinding.partialTimelineFilter.run {
            mBinding.insertDiaryButton.setOnClickListener { _ ->
                val createDiary = Intent(this@TimelineActivity, DiaryWritingActivity::class.java)
                TransitionHelper.startActivityWithTransition(this@TimelineActivity, createDiary)
            }

            closeToolbar.setOnClickListener {
                toggleFilterView(false)
            }

            filterView.setOnTouchListener { _, motionEvent ->
                if (mFirstTouch == 0F || mFirstTouch < motionEvent.y) mFirstTouch = motionEvent.y

                Log.i("aaf-t", "${motionEvent.action} ${motionEvent.actionIndex} ${motionEvent.y}")
                if (motionEvent.action == MotionEvent.ACTION_UP && (mFirstTouch - motionEvent.y > 100)) {
                    toggleFilterView(false)
                }
                true
            }

            query.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                    refreshList()
                    moveListViewScrollToBottom()
                    Log.i("aaf-t", "onTextChanged")
                }

                override fun afterTextChanged(editable: Editable) {}
            })

            clearFilter.setOnClickListener {
                startDate.text = null
                endDate.text = null
                query.text = null
                mSymbolSequence = SYMBOL_SELECT_ALL
                FlavorUtils.initWeatherView(this@TimelineActivity, symbol, mSymbolSequence, false)
                refreshList()
            }

            startDatePicker.setOnClickListener { mSDatePickerDialog.show() }
            endDatePicker.setOnClickListener { mEDatePickerDialog.show() }

            feelingSymbolButton.setOnClickListener { openFeelingSymbolDialog(getString(R.string.diary_symbol_search_message)) { symbolSequence ->
                selectFeelingSymbol(symbolSequence)
                refreshList()
            }}
        }
    }

    private fun selectFeelingSymbol(index: Int = SYMBOL_SELECT_ALL) {
        mSymbolSequence = if (index == 0) SYMBOL_SELECT_ALL else index
        FlavorUtils.initWeatherView(this, mBinding.partialTimelineFilter.symbol, mSymbolSequence, false)
    }

    private var mStartDateListener: DatePickerDialog.OnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        val startMillis = EasyDiaryUtils.datePickerToTimeMillis(dayOfMonth, month, year)
        mBinding.partialTimelineFilter.startDate.text = DateUtils.getFullPatternDate(startMillis)
        refreshList()
        Log.i("aaf-t", "mStartDateListener")
    }
    
    private var mEndDateListener: DatePickerDialog.OnDateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
        val endMillis = EasyDiaryUtils.datePickerToTimeMillis(dayOfMonth, month, year)
        mBinding.partialTimelineFilter.endDate.text = DateUtils.getFullPatternDate(endMillis)
        refreshList()
        Log.i("aaf-t", "mEndDateListener")
    }

    private fun toggleFilterView(isVisible: Boolean) {
        mFirstTouch = 0F
        val height = if (isVisible) 0F else mBinding.partialTimelineFilter.filterView.height.toFloat().unaryMinus()
        if (!isVisible) {
            this.currentFocus?.let { focusView ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(focusView.windowToken, 0)
            }
        }
        ObjectAnimator.ofFloat(mBinding.partialTimelineFilter.filterView, "translationY", height).apply {
            duration = 700
            start()
        }
    }

    private fun setupTimelineSearch() {
        mBinding.timelineList.onItemClickListener = AdapterView.OnItemClickListener { adapterView, view, i, l ->
            val diaryDto = adapterView.adapter.getItem(i) as Diary
            val detailIntent = Intent(this@TimelineActivity, DiaryReadingActivity::class.java)
            detailIntent.putExtra(DIARY_SEQUENCE, diaryDto.sequence)
            detailIntent.putExtra(SELECTED_SEARCH_QUERY, mBinding.partialTimelineFilter.query.text.toString())
            detailIntent.putExtra(SELECTED_SYMBOL_SEQUENCE, mSymbolSequence)
            TransitionHelper.startActivityWithTransition(this@TimelineActivity, detailIntent)
        }
    }
    
    private fun refreshList() {
        var startMillis = 0L
        var endMillis = 0L

        if (mBinding.partialTimelineFilter.startDate.text.isNotEmpty()) startMillis = EasyDiaryUtils.datePickerToTimeMillis(mSDatePickerDialog.datePicker.dayOfMonth, mSDatePickerDialog.datePicker.month, mSDatePickerDialog.datePicker.year)
        if (mBinding.partialTimelineFilter.endDate.text.isNotEmpty()) endMillis = EasyDiaryUtils.datePickerToTimeMillis(mEDatePickerDialog.datePicker.dayOfMonth, mEDatePickerDialog.datePicker.month, mEDatePickerDialog.datePicker.year, true)


        Log.i("aaf-t", "input date ${DateUtils.timeMillisToDateTime(startMillis, DateUtils.DATE_TIME_PATTERN_WITHOUT_DELIMITER)}")
        Log.i("aaf-t", "query ${mBinding.partialTimelineFilter.query.text}")

        mDiaryList.run {
            clear()
            addAll(EasyDiaryDbHelper.findDiary(
                    mBinding.partialTimelineFilter.query.text.toString(),
                    config.diarySearchQueryCaseSensitive,
                    startMillis,
                    endMillis,
                    mSymbolSequence
            ))
            reverse()
        }

        Log.i("aaf-t", "query ${mDiaryList.size}")

        mTimelineItemAdapter?.run {
            currentQuery = mBinding.partialTimelineFilter.query.text.toString()
            notifyDataSetChanged()
        }
    }
    
    private fun moveListViewScrollToBottom() {
        Handler().post { mBinding.timelineList.setSelection(mDiaryList.size - 1) }
    }
}

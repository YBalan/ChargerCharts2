package com.example.chargercharts2.ui.home

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.example.chargercharts2.databinding.FragmentHomeBinding
import android.widget.CheckBox
import com.example.chargercharts2.R
import com.example.chargercharts2.utils.*
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.fragment.app.activityViewModels
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.analytics.DetectCycles
import com.example.chargercharts2.chartbuilders.LifeTimeChartBuilder
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue

private const val CHECK_BOX_ID = "checkBoxId_"
private const val OTHER_CHECK_BOX_ID = "other_"

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by activityViewModels()

    private val csvDataMap get() = homeViewModel.csvDataMap
    private val isIgnoreZeros: Boolean get() = !binding.ignoreZeroCheckBox.isChecked

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i("HomeFragment", "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("HomeFragment", "onViewCreated")

        updateControls()
        clearCheckBoxes()
        plotCsvChart(homeViewModel.dataSets.value, isIgnoreZeros)
        setupObservers()
        setupSettingsAndApplyButton()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateControls()
    }

    private fun updateControls() {
        binding.checkBoxContainer.orientation =
            if (isLandscape()) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL

        if (isLandscape()) {
            updateViewMarginBottom(binding.checkBoxContainer, 8, context)
        } else {
            updateViewMarginBottom(binding.checkBoxContainer, 64, context)
        }
    }

    private fun clearCheckBoxes(prefix: String = "") {
        binding.checkBoxContainer.children.filter { view ->
            Log.i("clearCheckBoxes", "${view.tag?.toString()}")
            view.tag?.toString()?.startsWith(CHECK_BOX_ID + prefix) == true
        }.toList().forEach { chkBox ->
            Log.i("clearCheckBoxes", "${(chkBox as? CheckBox)?.text}")
            binding.checkBoxContainer.removeView(chkBox)
        }
    }

    private fun setupObservers() {
        homeViewModel.dataSets.observe(viewLifecycleOwner, Observer { dataSets ->
            plotCsvChart(dataSets, isIgnoreZeros)
            invalidateChart()
        })

        binding.ignoreZeroCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.lineChart.data = null
            binding.lineChart.hideHighlight()
            plotCsvChart(homeViewModel.dataSets.value, !isChecked)
            invalidateChart()
        }

        homeViewModel.removedEntry.observe(viewLifecycleOwner, Observer { entry ->
            binding.lineChart.hideHighlight()
            binding.lineChart.data?.dataSets?.forEach { ds ->
                Log.i("removedEntry.observe", "${ds.label}: xAxisValue: ${entry.dateTime}")
                ds.removeEntryByXValue(entry.dateTime.toEpoch())
            }
            invalidateChart()
        })

        homeViewModel.isStarted.observe(viewLifecycleOwner, Observer { isStarted ->
            Log.i("homeViewModel.isStarted.observe", "isStarted: $isStarted")
            binding.settings.visibility =
                if (isLandscape() && isStarted) View.GONE else View.VISIBLE
            if (isStarted) {
                binding.applyButton.text = getString(R.string.stop)
                binding.portTextField.isEnabled = false
                binding.limitTextField.isEnabled = false
            } else {
                binding.applyButton.text = getString(R.string.start)
                binding.portTextField.isEnabled = true
                binding.limitTextField.isEnabled = true
                binding.lineChart.data = null
                binding.lineChart.hideHighlight()
            }
        })

        homeViewModel.lastDateTime.observe(viewLifecycleOwner){
            //binding.lastDateTimeLabel.text = it
        }

        homeViewModel.addedEntry.observe(viewLifecycleOwner){
            updateLastDateTimeLabel(it)
        }
    }

    private fun updateLastDateTimeLabel(value: CsvDataValue? = null) {
        val lastEntry = binding.lineChart.moveToLast()
        binding.lastDateTimeLabel.text =
            (lastEntry?.data as? CsvDataValue)?.toString() ?: value?.dateTime?.toString() ?: "Last Update"
    }

    private fun plotCsvChart(dataSets: Map<String, List<CsvDataValue>>?, ignoreZeros: Boolean) {
        try {
            val chart = binding.lineChart
            chart.data?.clearValues()
            chart.data = null
            var visibleCount = csvDataMap.count { s -> s.value.voltageVisible }
            val showRelay = visibleCount == 1
            val showCycles = visibleCount == 1
            dataSets?.toList()?.forEachIndexed { idxForColor, (name, data) ->
                val csvData = csvDataMap.getOrPut(name) { CsvData(relayVisible = false, cyclesVisible = false) }
                val isVisible = csvData.voltageVisible

                csvData.clear()
                csvData.source = name
                data.toList().forEach { csvValue ->
                    //csvValue.source = name
                    csvData.addValue(csvValue)
                }

                DetectCycles.analyzeSimple(csvData, ignoreZeros, windowSize = 3, showCycleTraces = false)

                val colorSchema = getColorSchema(idxForColor, isDarkTheme())

                csvData.relayVisible = isVisible && showRelay
                csvData.relayColor = colorSchema.relayColor
                csvData.cyclesVisible = isVisible && showCycles
                csvData.cyclesColor = colorSchema.cyclesColor

                csvData.voltageColor = colorSchema.voltageColor
                csvData.voltageLabel = name

                if (LifeTimeChartBuilder().build(context, chart, csvData, ignoreZeros, isDarkTheme())) {
                    addCheckbox(name, csvData.voltageVisible, csvData.voltageColor, csvData/*,
                        addOtherCheckboxes = isVisible && showRelay*/)
                } else {
                    csvDataMap.remove(name)
                    removeCheckBox(name)
                }
            }
            val visibleData = csvDataMap.filter { s -> s.value.voltageVisible }
            val singleVisibleData = if(visibleData.count() == 1) visibleData.entries.first().value else null
            chart.legend.isEnabled = singleVisibleData?.relayVisible == true || singleVisibleData?.cyclesVisible == true
        } catch (e: Exception) {
            Log.e("HomeFragment", "dataSets?.toList()?.forEachIndexed", e)
            if (IS_DEBUG_BUILD) {
                throw e
            }
        }
    }

    private fun invalidateChart() {
        Log.i("HomeFragment", "invalidateChart")
        binding.lineChart.data?.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    private fun setupSettingsAndApplyButton() {
        //binding.portTextField.setText(UdpListener.DEFAULT_PORT)
        //binding.limitTextField.setText(UdpListener.DEFAULT_DATA_LIMIT)

        binding.applyButton.setOnClickListener {
            val port = binding.portTextField.text.toString().toIntOrNull() ?: UdpListener.port
            val dataLimit = binding.limitTextField.text.toString().toIntOrNull() ?: 0
            UdpListener.initialize(port, dataLimit)
            UdpListener.clear()
            homeViewModel.clear()
            clearCheckBoxes()
            plotCsvChart(homeViewModel.dataSets.value, isIgnoreZeros)
            invalidateChart()

            updateControls()
        }
        updateControls()
    }

    private fun addCheckbox(text: String, isChecked: Boolean, color: Int, csvData: CsvData, addOtherCheckboxes: Boolean = false) {
        fun invalidateChartInternal() {
            binding.lineChart.data = null
            binding.lineChart.hideHighlight()
            plotCsvChart(homeViewModel.dataSets.value, isIgnoreZeros)
            updateLastDateTimeLabel()
            invalidateChart()
        }

        var checkBox = binding.checkBoxContainer.findViewById<CheckBox>(getCheckBoxId(text))
        if (checkBox == null) {
            checkBox = CheckBox(context).apply {
                id = getCheckBoxId(text)
                this.text = text
                this.isChecked = isChecked
                tag = getCheckBoxIdStr(text)
                buttonTintList = ColorStateList.valueOf(color)
            }
            binding.checkBoxContainer.addView(checkBox)
        }
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            csvData.voltageVisible = isChecked
            invalidateChartInternal()
        }

        if(addOtherCheckboxes) {
            val relayCheckBox = CheckBox(context).apply {
                id = getCheckBoxId(csvData.relayLabel, OTHER_CHECK_BOX_ID)
                this.text = csvData.relayLabel
                this.isChecked = csvData.relayVisible
                tag = getCheckBoxIdStr(csvData.relayLabel, OTHER_CHECK_BOX_ID)
                buttonTintList = ColorStateList.valueOf(csvData.relayColor)
                setOnCheckedChangeListener { _, isChecked ->
                    csvData.relayVisible = isChecked
                    invalidateChartInternal()
                }
            }
            binding.checkBoxContainer.addView(relayCheckBox)

            val cyclesCheckBox = CheckBox(context).apply {
                id = getCheckBoxId(csvData.cyclesLabel, OTHER_CHECK_BOX_ID)
                this.text = csvData.cyclesLabel
                this.isChecked = csvData.cyclesVisible
                tag = getCheckBoxIdStr(csvData.cyclesLabel, OTHER_CHECK_BOX_ID)
                buttonTintList = ColorStateList.valueOf(csvData.cyclesColor)
                setOnCheckedChangeListener { _, isChecked ->
                    csvData.cyclesVisible = isChecked
                    invalidateChartInternal()
                }
            }
            binding.checkBoxContainer.addView(cyclesCheckBox)
        }else {
            clearCheckBoxes(OTHER_CHECK_BOX_ID)
        }
    }

    private fun getCheckBoxId(text: String, prefix: String = ""): Int = getCheckBoxIdStr(text, prefix).hashCode()
    private fun getCheckBoxIdStr(text: String, prefix: String = ""): String = "$CHECK_BOX_ID$prefix$text"

    private fun removeCheckBox(text: String) {
        binding.checkBoxContainer.findViewById<CheckBox>(getCheckBoxId(text))?.let {
            binding.checkBoxContainer.removeView(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

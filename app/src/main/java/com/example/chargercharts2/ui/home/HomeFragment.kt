package com.example.chargercharts2.ui.home

import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.chargercharts2.databinding.FragmentHomeBinding
import android.widget.CheckBox
import com.example.chargercharts2.R
import com.example.chargercharts2.utils.*
import android.widget.LinearLayout
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.analytics.DetectCycles
import com.example.chargercharts2.chartbuilders.LifeTimeChartBuilder
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.models.CsvDataValue

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    private val csvDataMap get() = homeViewModel.csvDataMap

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
        plotCsvChart(homeViewModel.dataSets.value)
        setupObservers()
        setupSettingsAndApplyButton()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateControls()
    }

    private fun updateControls() {
        binding.checkBoxContainer.orientation = if (isLandscape()) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        binding.settings.visibility = if(isLandscape() && isStarted()) View.GONE else View.VISIBLE

        if(isLandscape()) {
            updateViewMarginBottom(binding.checkBoxContainer, 8, context)
        }
        else{
            updateViewMarginBottom(binding.checkBoxContainer, 64, context)
        }

        updateSettingsControls()
    }

    private fun clearCheckBoxes() {
        binding.checkBoxContainer.removeAllViews()
    }

    private fun setupObservers() {
        homeViewModel.dataSets.observe(viewLifecycleOwner, Observer { dataSets ->
            plotCsvChart(dataSets)
            invalidateChart()
        })

        homeViewModel.removedEntry.observe(viewLifecycleOwner, Observer { entry ->
            binding.lineChart.data?.dataSets?.forEach{ ds->
                Log.i("removedEntry.observe", "${ds.label}: xAxisValue: ${entry.dateTime}")
                ds.removeEntryByXValue(entry.dateTime.toEpoch()) }
            invalidateChart()
        })
    }

    private fun plotCsvChart(dataSets: Map<String, List<CsvDataValue>>?) {
        val ignoreZeros = false
        try{
            val chart = binding.lineChart
            chart.data?.clearValues()
            dataSets?.toList()?.forEachIndexed { idxForColor, (name, data) ->
                val csvData = csvDataMap.getOrPut(name) { CsvData() }

                csvData.clear()
                data.toList().forEach { csvValue ->
                    csvData.values.add(csvValue)
                }

                DetectCycles.analyzeSimple(csvData, ignoreZeros, showCycleTraces = false)

                csvData.relayVisible = false
                csvData.cyclesVisible = false
                csvData.voltageColor = getColor(idxForColor)
                csvData.voltageLabel = name
                if(LifeTimeChartBuilder().build(context, chart, csvData, ignoreZeros, isDarkTheme())){
                    addCheckbox(name, csvData.voltageVisible, csvData.voltageColor, csvData)
                }else{
                    csvDataMap.remove(name)
                    removeCheckBox(name)
                }
            }
        }catch(e: Exception){
            Log.e("HomeFragment", "dataSets?.toList()?.forEachIndexed", e)
            if (IS_DEBUG_BUILD) { throw e }
        }
    }

    private fun invalidateChart(){
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
            val dataLimit = binding.limitTextField.text.toString().toIntOrNull() ?: UdpListener.dataLimit
            UdpListener.initialize(port, dataLimit)
            UdpListener.clear()
            homeViewModel.clear()
            clearCheckBoxes()
            plotCsvChart(homeViewModel.dataSets.value)
            invalidateChart()

            updateControls()
        }
        updateControls()
    }

    private fun updateSettingsControls() {
        if (isStarted()) {
            binding.applyButton.text = getString(R.string.stop)
            binding.portTextField.isEnabled = false
            binding.limitTextField.isEnabled = false
        } else {
            binding.applyButton.text = getString(R.string.start)
            binding.portTextField.isEnabled = true
            binding.limitTextField.isEnabled = true
        }
    }

    private fun isStarted(): Boolean{
        return UdpListener.isListening
    }

    private fun addCheckbox(text: String, isChecked: Boolean, color: Int, csvData: CsvData) {
        val existing = binding.checkBoxContainer.findViewById<CheckBox>(getCheckBoxId(text))
        if(existing == null) {
            val checkBox = CheckBox(context).apply {
                id = getCheckBoxId(text)
                this.text = text
                this.isChecked = isChecked
                buttonTintList = ColorStateList.valueOf(color)
                setOnCheckedChangeListener { _, isChecked ->
                    csvData.voltageVisible = isChecked
                    binding.lineChart.hideHighlight()
                    plotCsvChart(homeViewModel.dataSets.value)
                    invalidateChart()
                }
            }
            binding.checkBoxContainer.addView(checkBox)
        }
    }

    private fun getCheckBoxId(text: String): Int = "checkBoxId_$text".hashCode()

    private  fun removeCheckBox(text: String){
        binding.checkBoxContainer.findViewById<CheckBox>(getCheckBoxId(text))?.let {
            binding.checkBoxContainer.removeView(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

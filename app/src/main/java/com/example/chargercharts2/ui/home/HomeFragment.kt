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
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.example.chargercharts2.databinding.FragmentHomeBinding
import android.widget.CheckBox
import com.example.chargercharts2.R
import com.example.chargercharts2.utils.*
import android.graphics.Color
import android.widget.LinearLayout
import com.example.chargercharts2.BuildConfig.IS_DEBUG_BUILD
import com.example.chargercharts2.models.CsvData
import com.github.mikephil.charting.charts.LineChart

class HomeFragment : Fragment() {

    private val predefinedColors = listOf(
        Color.RED,
        Color.BLUE,
        Color.GREEN,
        Color.MAGENTA,
        Color.CYAN,
        Color.YELLOW,
        Color.DKGRAY,
        Color.LTGRAY
    )

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    private val dataSetsMap get() = homeViewModel.dataSetsMap

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

        updateCheckBoxContainerOrientation()
        setupChart()
        fillChart(homeViewModel.dataSets.value)
        setupObservers()
        setupSettingsAndApplyButton()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateCheckBoxContainerOrientation()
    }

    private fun updateCheckBoxContainerOrientation() {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        binding.checkBoxContainer.orientation = if (isLandscape) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        binding.settings.visibility = if(isLandscape) View.GONE else View.VISIBLE

        if(isLandscape) {
            updateViewMarginBottom(binding.checkBoxContainer, 8, context)
        }
        else{
            updateViewMarginBottom(binding.checkBoxContainer, 64, context)
        }
    }

    private fun setupChart() {
        binding.checkBoxContainer.removeAllViews()

        binding.lineChart.clear()
        binding.lineChart.apply {
            data = LineData()
            //axisLeft.axisMinimum = 0f
            axisRight.isEnabled = true
            description.isEnabled = false
            xAxis.labelRotationAngle = 45f
            xAxis.granularity = 60F
            xAxis.isGranularityEnabled = true

            setChartSettings(context, this, isDarkTheme(), CsvData.DATE_TIME_UDP_CHART_FORMAT,
                CsvData.DATE_TIME_TOOLTIP_FORMAT)
        }
    }

    private fun getColor(setNumber: Int): Int{
        return predefinedColors[setNumber % predefinedColors.size]
    }

    private fun setupObservers() {
        homeViewModel.dataSets.observe(viewLifecycleOwner, Observer { dataSets ->
            fillChart(dataSets)
            invalidateChart()
        })

        homeViewModel.removedEntry.observe(viewLifecycleOwner, Observer { entry ->
            binding.lineChart.data?.dataSets?.forEach{ ds-> ds.removeEntryByXValue(entry.first) }
            invalidateChart()
        })
    }

    private fun fillChart(dataSets: Map<String, List<Pair<Float, Float>>>?) {
        try{
            dataSets?.toList()?.forEachIndexed { idx, (name, data) ->
                val lineDataSet = dataSetsMap.getOrPut(name) {
                    LineDataSet(mutableListOf(), name)
                }

                lineDataSet.clear()

                lineDataSet.lineWidth = 3f
                lineDataSet.color = getColor(idx)
                lineDataSet.setCircleColor(getColor(idx))
                lineDataSet.valueTextColor = Color.WHITE

                if(lineDataSet.isVisible) {
                    data.toList().forEach { (x, y) -> lineDataSet.addEntry(Entry(x, y)) }
                }

                if(!binding.lineChart.data.isSetExistsByLabel(name, true)){
                    binding.lineChart.data?.addDataSet(lineDataSet)
                    addCheckboxForDataSet(name, binding.lineChart)
                }

                recalculateYAxis(binding.lineChart)
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
            setupChart()
            fillChart(homeViewModel.dataSets.value)
            invalidateChart()

            updateSettings()
        }
        updateSettings()
    }

    private fun updateSettings() {
        if (UdpListener.isListening) {
            binding.applyButton.text = getString(R.string.stop)
            binding.portTextField.isEnabled = false
            binding.limitTextField.isEnabled = false
        } else {
            binding.applyButton.text = getString(R.string.start)
            binding.portTextField.isEnabled = true
            binding.limitTextField.isEnabled = true
        }
    }

    private fun recalculateYAxis(chart: LineChart) {
        val allVisibleEntries = chart.data.dataSets
            .filter { it.isVisible } // Include only visible data sets
            .flatMap  { dataSet ->
                (0 until dataSet.entryCount).map { dataSet.getEntryForIndex(it) }
            } // Collect all entries from visible data sets

        val margin = 0.1f

        if (allVisibleEntries.isNotEmpty()) {
            val minY = allVisibleEntries.minOf { it.y }
            val maxY = allVisibleEntries.maxOf { it.y }

            chart.axisLeft.axisMinimum = chooseValue(minY - margin  == 0f, minY, minY - margin)
            chart.axisLeft.axisMaximum = maxY

            chart.axisRight.axisMinimum = chooseValue(minY - margin  == 0f, minY, minY - margin)
            chart.axisRight.axisMaximum = maxY
        } else {
            // Reset axis if no data is visible
            chart.axisLeft.resetAxisMaximum()
            chart.axisLeft.resetAxisMinimum()

            chart.axisRight.resetAxisMaximum()
            chart.axisRight.resetAxisMinimum()
        }
    }

    private fun addCheckboxForDataSet(setName: String, chart: LineChart) {
        val dataSet = dataSetsMap[setName]
        Log.i("HomeFragment", "addCheckboxForDataSet: $setName isVisible: ${dataSet?.isVisible}")
        if(dataSet != null) {
            val checkBox = CheckBox(context).apply {
                text = setName
                isChecked = dataSet.isVisible
                buttonTintList = ColorStateList.valueOf(dataSet.color)
                setOnCheckedChangeListener { _, isChecked ->
                    dataSet.isVisible = isChecked
                    /*if(!isChecked){
                        dataSet.clear()
                    }*/

                    fillChart(homeViewModel.dataSets.value)

                    invalidateChart()
                }
            }
            binding.checkBoxContainer.addView(checkBox)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.chargercharts2.ui.home

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
import com.example.chargercharts2.models.*
import android.graphics.Color
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val dataSetsMap = mutableMapOf<String, LineDataSet>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i("HomeFragment", "onCreateView")
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupChart()
        fillChart(homeViewModel.dataSets.value)
        setupObservers()
        setupSettings()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("HomeFragment", "onViewCreated")
    }

    private fun setupChart() {
        binding.lineChart.apply {
            data = LineData()
            //axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            xAxis.labelRotationAngle = 45f

            xAxis.valueFormatter = CustomValueFormatter(CsvData.dateTimeChartFormat)
            val markerView = CustomMarkerView(context, R.layout.custom_marker_view, CsvData.dateTimeToolTipFormat)
            marker = markerView
            //markerView.chartView = this // For MPAndroidChart 3.0+
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
                data.toList().forEach { (x, y) -> lineDataSet.addEntry(Entry(x, y)) }

                if(!binding.lineChart.data.isSetExistsByLabel(name, true)){
                    binding.lineChart.data?.addDataSet(lineDataSet)
                    addCheckboxForDataSet(name)
                }
            }
        }catch(e: Exception){
            Log.e("HomeFragment", "dataSets?.toList()?.forEachIndexed", e)
            throw e
        }
    }

    private fun invalidateChart(){
        Log.i("HomeFragment", "invalidateChart")
        binding.lineChart.data?.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    private fun setupSettings() {
        binding.applyButton.setOnClickListener {
            val port = binding.portTextField.text.toString().toIntOrNull() ?: 1985
            val limit = binding.limitTextField.text.toString().toIntOrNull() ?: 50
            UdpListener.initialize(port, limit)
            //setupChart()
            fillChart(homeViewModel.dataSets.value)
            invalidateChart()
        }
    }

    private fun addCheckboxForDataSet(setName: String) {
        Log.i("HomeFragment", "addCheckboxForDataSet: $setName")
        val checkBox = CheckBox(context).apply {
            text = setName
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                dataSetsMap[setName]?.isVisible = isChecked
                binding.lineChart.invalidate()
            }
        }
        binding.checkBoxContainer.addView(checkBox)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

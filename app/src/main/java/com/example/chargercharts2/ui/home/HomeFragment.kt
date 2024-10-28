package com.example.chargercharts2.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.chargercharts2.databinding.FragmentHomeBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import android.widget.CheckBox
import com.example.chargercharts2.ui.home.HomeViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var homeViewModel: HomeViewModel? = null
    //private val homeViewModel: HomeViewModel by viewModels()
    private val lineDataSets = mutableMapOf<String, LineDataSet>() // Store datasets by name

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setupChart()
        observeViewModel()
        return binding.root
    }

    private fun setupChart() {
        binding.lineChart.apply {
            data = LineData()
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            description.isEnabled = false
            xAxis.labelRotationAngle = 45f
        }
    }

    private fun observeViewModel() {
        homeViewModel?.ipAddress?.observe(viewLifecycleOwner, Observer { ip ->
            binding.localIpTextView.text = "Local IP: $ip"
        })

        homeViewModel?.voltageData?.observe(viewLifecycleOwner, Observer { dataSets ->
            dataSets.forEach { (setName, dataList) ->
                val lineDataSet = lineDataSets.getOrPut(setName) {
                    val newDataSet = LineDataSet(mutableListOf(), setName).apply {
                        color = resources.getColor(android.R.color.holo_blue_dark, null)
                        lineWidth = 2f
                    }
                    binding.lineChart.data.addDataSet(newDataSet)
                    addCheckboxForDataSet(setName)
                    newDataSet
                }

                lineDataSet.clear()
                lineDataSet.values = dataList.map { (x, y) -> Entry(x, y) }

                binding.lineChart.data.notifyDataChanged()
                binding.lineChart.notifyDataSetChanged()
                binding.lineChart.invalidate()
            }
        })
    }

    private fun addCheckboxForDataSet(setName: String) {
        val checkBox = CheckBox(context).apply {
            text = setName
            isChecked = true
            setOnCheckedChangeListener { _, isChecked ->
                lineDataSets[setName]?.isVisible = isChecked
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
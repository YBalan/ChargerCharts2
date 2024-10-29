package com.example.chargercharts2.ui.home

import android.os.Bundle
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
import com.example.chargercharts2.utils.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val dataSetsMap = mutableMapOf<String, LineDataSet>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        setupChart()
        setupObservers()
        setupConfiguration()

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

    private fun setupObservers() {
        homeViewModel.dataSets.observe(viewLifecycleOwner, Observer { dataSets ->
            dataSets.forEach { (name, data) ->
                val lineDataSet = dataSetsMap.getOrPut(name) {
                    LineDataSet(mutableListOf(), name).apply {
                        lineWidth = 2f
                    }.also {
                        binding.lineChart.data.addDataSet(it)
                        addCheckboxForDataSet(name)
                    }
                }

                lineDataSet.clear()
                data.forEach { (x, y) -> lineDataSet.addEntry(Entry(x, y)) }
            }

            binding.lineChart.data.notifyDataChanged()
            binding.lineChart.notifyDataSetChanged()
            binding.lineChart.invalidate()
        })
    }

    private fun setupConfiguration() {
        binding.applyButton.setOnClickListener {
            val port = binding.portTextField.text.toString().toIntOrNull() ?: 1985
            val limit = binding.limitTextField.text.toString().toIntOrNull() ?: 50
            UdpListener.initialize(port, limit)
        }
    }

    private fun addCheckboxForDataSet(setName: String) {
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

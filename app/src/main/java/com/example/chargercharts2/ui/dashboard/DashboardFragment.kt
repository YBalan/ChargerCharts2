package com.example.chargercharts2.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.chargercharts2.databinding.FragmentDashboardBinding // Adjust with actual binding class
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.utils.chooseValue
import com.example.chargercharts2.utils.isDarkTheme
import com.example.chargercharts2.utils.updateViewMarginBottom

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateControls(isChartView = false)

        setupBackButton()
        binding.pickFileButton.setOnClickListener {
            openFilePicker()
        }

        viewModel.csvChartData.observe(viewLifecycleOwner) { csvData ->
            updateControls(plotCsvChart(csvData, !binding.ignoreZeroCheckBox.isChecked))
        }

        viewModel.fileName.observe(viewLifecycleOwner){ fileName ->
            binding.fileNameTextView.text = fileName
        }

        binding.ignoreZeroCheckBox.setOnCheckedChangeListener { _, isChecked ->
            plotCsvChart(viewModel.csvChartData.value, !isChecked)
            binding.lineChart.invalidate()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateControls(isChartView = !viewModel.isEmpty())
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Define your back button behavior
                viewModel.clear()
                updateControls(isChartView = false)
            }
        })
    }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    viewModel.parseCsvFile(requireContext(), uri)
                    //binding.fileNameTextView.text = getFileNameFromUri(requireContext(), uri)
                } ?: Toast.makeText(context, "File selection error", Toast.LENGTH_SHORT).show()
            }
        }

    private fun plotCsvChart(csvData: CsvData?, ignoreZeros: Boolean) : Boolean {
        if (csvData == null || csvData.values.isEmpty()) {
            //Toast.makeText(context, "Failed to plot data", Toast.LENGTH_SHORT).show()
            return false
        }

        if(CsvData.plotCsvData(context, binding.lineChart, csvData, ignoreZeros, isDarkTheme())){
            updateControls(isChartView = true)
            setCheckBoxColorFromDataSet(binding.voltageCheckBox, csvData.voltageLabel)
            setCheckBoxColorFromDataSet(binding.relayCheckBox, csvData.relayLabel)
            setCheckBoxColorFromDataSet(binding.cyclesCheckBox, csvData.cyclesLabel)

            binding.voltageCheckBox.isChecked = csvData.voltageVisible
            binding.relayCheckBox.isChecked = csvData.relayVisible
            binding.cyclesCheckBox.isChecked = csvData.cyclesVisible
            binding.voltageCheckBox.text = csvData.voltageLabel
            binding.relayCheckBox.text = csvData.relayLabel
            binding.cyclesCheckBox.text = csvData.cyclesLabel

            // Set up CheckBox listeners to toggle chart visibility
            binding.voltageCheckBox.setOnCheckedChangeListener { _, isChecked ->
                toggleDataSetVisibility(csvData.voltageLabel, isChecked)
                csvData.voltageVisible = isChecked
            }

            binding.relayCheckBox.setOnCheckedChangeListener { _, isChecked ->
                toggleDataSetVisibility(csvData.relayLabel, isChecked)
                csvData.relayVisible = isChecked
            }

            binding.cyclesCheckBox.setOnCheckedChangeListener{_, isChecked ->
                toggleDataSetVisibility(csvData.cyclesLabel, isChecked)
                csvData.cyclesVisible = isChecked
            }

            return true
        }else{
            Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
        }

        return false
    }

    private fun toggleDataSetVisibility(label: String, isVisible: Boolean) {
        // Find the dataset by label and set its visibility
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, true)
        dataSet?.isVisible = isVisible
        binding.lineChart.invalidate() // Refresh chart to apply changes
    }

    private fun setCheckBoxColorFromDataSet(checkBox: CheckBox, label: String){
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, true)
        checkBox.buttonTintList = ColorStateList.valueOf(dataSet?.color ?: checkBox.buttonTintList!!.defaultColor)
    }

    private fun updateControls(isChartView: Boolean) {
        binding.lineChart.visibility = chooseValue(isChartView, View.VISIBLE, View.GONE)
        binding.fileNameTextView.visibility = chooseValue(isChartView, View.VISIBLE, View.GONE)
        binding.checkBoxContainer.visibility = chooseValue(isChartView, View.VISIBLE, View.GONE)
        binding.pickFileButton.visibility = chooseValue(isChartView, View.GONE, View.VISIBLE)

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        if(isLandscape) {
            updateViewMarginBottom(binding.lineChart, 8, context)
        }
        else{
            updateViewMarginBottom(binding.lineChart, 70, context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

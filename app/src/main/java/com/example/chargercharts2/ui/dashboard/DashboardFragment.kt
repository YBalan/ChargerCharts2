package com.example.chargercharts2.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.chargercharts2.chartbuilders.HistoryChartBuilder
import com.example.chargercharts2.databinding.FragmentDashboardBinding // Adjust with actual binding class
import com.example.chargercharts2.models.CsvData
import com.example.chargercharts2.utils.getFileExtensionFromUri
import com.example.chargercharts2.utils.hideHighlight
import com.example.chargercharts2.utils.isDarkTheme
import com.example.chargercharts2.utils.isLandscape
import com.example.chargercharts2.utils.updateViewMarginBottom
import java.io.File

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by activityViewModels()
    val isIgnoreZeros: Boolean get() = !binding.ignoreZeroCheckBox.isChecked

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
            updateControls(plotCsvChart(csvData, isIgnoreZeros))
        }

        viewModel.fileName.observe(viewLifecycleOwner){ fileName ->
            binding.fileNameTextView.text = fileName
        }

        binding.ignoreZeroCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.lineChart.hideHighlight()
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
                    val ext = getFileExtensionFromUri(context, uri)
                    Log.i("filePickerLauncher", "$uri; path: ${uri.path}; ext: $ext")
                    if (ext?.lowercase() != "csv") {
                        Toast.makeText(context, "Selected file is not *.csv", Toast.LENGTH_SHORT).show()
                        return@let
                    }
                    if (!viewModel.parseCsvFile(requireContext(), uri)) {
                        binding.lineChart.data = null
                        binding.lineChart.invalidate()
                        Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "File selection error", Toast.LENGTH_SHORT).show()
            }
        }

    private fun plotCsvChart(csvData: CsvData?, ignoreZeros: Boolean) : Boolean {
        if (csvData == null || csvData.values.isEmpty()) return false

        binding.lineChart.hideHighlight()
        if(HistoryChartBuilder().build(context, binding.lineChart, csvData, ignoreZeros, isDarkTheme())){
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
                binding.lineChart.hideHighlight()
                toggleChartDataSetVisibility(csvData.voltageLabel, isChecked)
                csvData.voltageVisible = isChecked
            }

            binding.relayCheckBox.setOnCheckedChangeListener { _, isChecked ->
                binding.lineChart.hideHighlight()
                toggleChartDataSetVisibility(csvData.relayLabel, isChecked)
                csvData.relayVisible = isChecked
            }

            binding.cyclesCheckBox.setOnCheckedChangeListener{_, isChecked ->
                binding.lineChart.hideHighlight()
                Log.i("binding.cyclesCheckBox.setOnCheckedChangeListener","label: ${csvData.cyclesLabel}; IsChecked: $isChecked")
                toggleChartDataSetVisibility(csvData.cyclesLabel, isChecked)
                csvData.cyclesVisible = isChecked
            }

            return true
        }
        return false
    }

    private fun toggleChartDataSetVisibility(label: String, isVisible: Boolean) {
        // Find the dataset by label and set its visibility
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, true)
        Log.i("toggleChartDataSetVisibility","label: $label; IsVisible: $isVisible; dataSet: ${dataSet != null}")
        dataSet?.isVisible = isVisible
        binding.lineChart.invalidate() // Refresh chart to apply changes
    }

    private fun setCheckBoxColorFromDataSet(checkBox: CheckBox, label: String){
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, true)
        checkBox.buttonTintList = ColorStateList.valueOf(dataSet?.color ?: Color.YELLOW)
    }

    private fun updateControls(isChartView: Boolean) {
        binding.lineChart.visibility = if(isChartView) View.VISIBLE else View.GONE
        binding.fileNameTextView.visibility = if(isChartView) View.VISIBLE else View.GONE
        binding.checkBoxContainer.visibility = if(isChartView) View.VISIBLE else View.GONE
        binding.pickFileButton.visibility = if(isChartView) View.GONE else View.VISIBLE

        if(isLandscape()) {
            updateViewMarginBottom(binding.lineChart, 8, context)
        }
        else{
            updateViewMarginBottom(binding.lineChart, 80, context)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

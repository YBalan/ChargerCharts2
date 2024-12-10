package com.example.chargercharts2.ui.dashboard

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
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
import com.example.chargercharts2.utils.getFileExtension
import com.example.chargercharts2.utils.hideHighlight
import com.example.chargercharts2.utils.isDarkTheme
import com.example.chargercharts2.utils.isLandscape
import com.example.chargercharts2.utils.updateViewMarginBottom

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by activityViewModels()
    private var _isLongPressed = false

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
            _isLongPressed = false
            openFilePicker()
        }

        binding.pickFileButton.setOnLongClickListener {
            _isLongPressed = true
            openFilePicker()
            true
        }

        viewModel.csvChartData.observe(viewLifecycleOwner) { csvData ->
            updateControls(plotCsvChart(csvData, !binding.ignoreZeroCheckBox.isChecked))
        }

        viewModel.addedEntry.observe(viewLifecycleOwner){ csvDataValue ->
            HistoryChartBuilder().addValue(context, binding.lineChart,
                viewModel.csvChartData.value ?: CsvData(), csvDataValue,
                ignoreZeros = !binding.ignoreZeroCheckBox.isChecked, checkValueVisibility = true, addSetsIfNotVisible = true)
            invalidateChart()
        }

        viewModel.fileName.observe(viewLifecycleOwner){ fileName ->
            binding.fileNameTextView.text = fileName
        }

        binding.ignoreZeroCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.lineChart.hideHighlight()
            //if(!_isLongPressed){
                plotCsvChart(viewModel.csvChartData.value, !isChecked)
            //}
            invalidateChart()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        updateControls(isChartView = !viewModel.isEmpty())
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Define your back button behavior
                _isLongPressed = false
                viewModel.clear()
                viewModel.stopTimeLaps()
                binding.lineChart.data = null
                updateControls(isChartView = false)
            }
        })
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher.launch(intent)
    }

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uris: MutableList<Uri> = mutableListOf()
                result.data?.data?.let{ uris.add(it) }

                val clipData = result?.data?.clipData
                if(clipData != null){
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        if(validateUri(uri)){
                            uris.add(uri)
                        }
                    }
                }

                if(uris.isEmpty())
                    Toast.makeText(context, "File selection error", Toast.LENGTH_SHORT).show()
                if (!viewModel.parseCsvFile(requireContext(), uris, _isLongPressed, isDarkTheme())) {
                    binding.lineChart.data = null
                    invalidateChart()
                    Toast.makeText(context, "No data available", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private fun validateUri(uri: Uri, showToast: Boolean = false): Boolean {
        val ext = uri.getFileExtension(context)
        Log.i("filePickerLauncher", "$uri; path: ${uri.path}; ext: $ext")
        if (ext?.lowercase() != "csv") {
            if( showToast) {
                Toast.makeText(context, "${uri.path} - Selected file is not *.csv",  Toast.LENGTH_SHORT).show()
            }
            return false
        }
        return true
    }

    private fun plotCsvChart(csvData: CsvData?, ignoreZeros: Boolean) : Boolean {
        if (csvData == null || csvData.values.isEmpty()) return false

        binding.lineChart.hideHighlight()
        if(HistoryChartBuilder().build(context, binding.lineChart, csvData, ignoreZeros, isDarkTheme(),
            checkValueVisibility = _isLongPressed)){
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

            invalidateChart()
            return true
        }
        return false
    }

    private fun toggleChartDataSetVisibility(label: String, isVisible: Boolean, ignoreCase: Boolean = true) {
        // Find the dataset by label and set its visibility
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, ignoreCase)
        Log.i("toggleChartDataSetVisibility", "label: $label; IsVisible: $isVisible; dataSet: ${dataSet != null}")
        dataSet?.isVisible = isVisible
        binding.lineChart.data.dataSets.filter { ds ->
            ds.label.startsWith(label, ignoreCase)
        }.forEach { ds ->
            ds.isVisible = isVisible
        }
        invalidateChart()
    }

    private fun setCheckBoxColorFromDataSet(checkBox: CheckBox, label: String, ignoreCase: Boolean = true) {
        val dataSet = binding.lineChart.data?.getDataSetByLabel(label, true)
        val color = dataSet?.color ?: binding.lineChart.data?.dataSets?.firstOrNull { ds ->
            ds.label.startsWith(label, ignoreCase)
        }?.color ?: Color.YELLOW
        checkBox.buttonTintList = ColorStateList.valueOf(color)
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

    private fun invalidateChart() {
        Log.i("DashboardFragment", "invalidateChart")
        binding.lineChart.data?.notifyDataChanged()
        binding.lineChart.notifyDataSetChanged()
        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

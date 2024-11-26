package com.example.chargercharts2.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.chargercharts2.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val notificationViewModel: NotificationsViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i("NotificationsFragment", "onCreateView")
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        notificationViewModel.message.observe(viewLifecycleOwner, Observer { message ->
            binding.messagesTextView.append("$message\n")

            binding.messagesTextScrollView.post{
                binding.messagesTextScrollView.fullScroll(View.FOCUS_DOWN)
            }
        })

        binding.messagesTextView.text = ""

        for(message in notificationViewModel.messages.value?.toMutableList() ?: mutableListOf())
            binding.messagesTextView.append("$message\n")

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

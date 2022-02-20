package com.example.spor_tfg

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.spor_tfg.databinding.FragmentFirstBinding
import org.w3c.dom.Text

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find the random_button by its ID and navigate to second fragment on click
        view.findViewById<Button>(R.id.random_button).setOnClickListener {
            val showCountTextView: TextView = view.findViewById(R.id.textview_first)
            val currentCount: Int = showCountTextView.text.toString().toInt()
            val action = FirstFragmentDirections.actionFirstFragmentToSecondFragment(currentCount)
            findNavController().navigate(action)
        }

        // find the toast_button by its ID and set a click listener
        view.findViewById<Button>(R.id.toast_button).setOnClickListener {
            // create a Toast with some text to appear for a short time
            val myToast: Toast = Toast.makeText(context, getString(R.string.toast_text), Toast.LENGTH_SHORT)
            myToast.show()
        }

        // find the textview_first by its ID and increment its value
        view.findViewById<TextView>(R.id.count_button).setOnClickListener {
            countMe(view)
        }
    }

    private fun countMe(view: View) {
       // Get the text view
        val showCountTextView: TextView = view.findViewById(R.id.textview_first)
        // Get the value of the text view
        val countString: String = showCountTextView.text.toString()
        // Convert to int and increment it
        var count = countString.toInt()
        count++
        // Display the value in the textview
        showCountTextView.text = count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
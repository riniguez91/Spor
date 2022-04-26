package com.example.spor_tfg

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.textfield.TextInputLayout

class AddPlayerDialogFragment: DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val inflatedView: LinearLayout = inflater.inflate(R.layout.add_player_signin, null) as LinearLayout

            // Set dropdown menu
            val items = listOf("Right footed", "Left footed", "Ambidextrous")
            val adapter = ArrayAdapter(requireContext().applicationContext, R.layout.list_item, items)
            (inflatedView.findViewById<TextInputLayout>(R.id.player_signin_leading_foot_ilayout).editText as? AutoCompleteTextView)?.setAdapter(adapter)

            builder.setView(inflatedView)
                // Add action buttons
                .setPositiveButton(
                    com.google.android.material.R.string.mtrl_picker_confirm,
                    DialogInterface.OnClickListener { dialog, id ->
                        // Send the positive button event back to the host activity
                        listener.onDialogPositiveClick(this, inflatedView)
                    })
                .setNegativeButton(
                    com.google.android.material.R.string.mtrl_picker_cancel,
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()?.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // Use this instance of the interface to deliver action events
    internal lateinit var listener: NoticeDialogListener

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {
        fun onDialogPositiveClick(dialog: DialogFragment, inflatedView: LinearLayout)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException((context.toString() +
                    " must implement NoticeDialogListener"))
        }
    }
}
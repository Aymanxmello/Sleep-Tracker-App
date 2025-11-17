package com.example.sleeptrackerapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {

    // Interface to communicate logout click back to the hosting Activity (MainActivity)
    interface LogoutListener {
        fun onLogoutClicked()
    }

    private var logoutListener: LogoutListener? = null

    // Ensure the hosting activity implements the listener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LogoutListener) {
            logoutListener = context
        } else {
            // Log this error or handle it as necessary
            // throw RuntimeException("$context must implement LogoutListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)
        val btnLogout: Button = view.findViewById(R.id.btn_logout)

        // Trigger the callback when the button is clicked
        btnLogout.setOnClickListener {
            logoutListener?.onLogoutClicked()
        }

        return view
    }

    override fun onDetach() {
        super.onDetach()
        logoutListener = null
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}
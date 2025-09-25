package com.mtlc.studyplan.settings.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Fragment for resolving setting conflicts during import
 * TODO: Implement conflict resolution UI
 */
class ConflictResolutionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Simple placeholder view
        return TextView(requireContext()).apply {
            text = "Conflict Resolution (Under Development)"
            textSize = 16f
            setPadding(32, 32, 32, 32)
        }
    }

    companion object {
        fun newInstance(): ConflictResolutionFragment {
            return ConflictResolutionFragment()
        }
    }
}
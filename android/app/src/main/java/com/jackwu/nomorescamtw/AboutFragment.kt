package com.jackwu.nomorescamtw

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvAboutVersion = view.findViewById<TextView>(R.id.tvAboutVersion)
        val tvGithub = view.findViewById<TextView>(R.id.tvGithub)
        val btnCloseAbout = view.findViewById<Button>(R.id.btnCloseAbout)

        try {
            val pInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            tvAboutVersion.text = getString(R.string.about_version, pInfo.versionName)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tvGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_github_link)))
            startActivity(intent)
        }

        view.findViewById<TextView>(R.id.tvDataSource).setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.data_source_url)))
            startActivity(intent)
        }

        btnCloseAbout.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}

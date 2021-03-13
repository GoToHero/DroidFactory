package ru.androidacademy.droidfactory.features.memesScreen

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import ru.androidacademy.droidfactory.R
import ru.androidacademy.droidfactory.databinding.MemesScreenFragmentBinding

class MemesScreenFragment : Fragment(R.layout.memes_screen_fragment) {

    private var _binding: MemesScreenFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: MemesScreenFragmentViewModel


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = MemesScreenFragmentBinding.bind(view)

        val viewModelFactory = MemesScreenFragmentViewModelFactory()
        viewModel = ViewModelProvider(this, viewModelFactory)
            .get(MemesScreenFragmentViewModel::class.java)
        viewModel.loadingState.observe(this.viewLifecycleOwner, this::setLoading)
        initErrorHandler()

    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
    }

    private fun initErrorHandler() {
        viewModel.error.observe(viewLifecycleOwner, { errorMessage ->
            Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_SHORT)
                .show()
        })
    }
}
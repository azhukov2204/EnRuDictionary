package ru.androidlearning.presentation.fragments.search.dialog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.terrakok.cicerone.Router
import org.koin.android.ext.android.inject
import org.koin.android.scope.AndroidScopeComponent
import org.koin.androidx.scope.fragmentScope
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.scope.Scope
import ru.androidlearning.core.DataLoadingState
import ru.androidlearning.core.DictionaryPresentationDataModel
import ru.androidlearning.fragments.R
import ru.androidlearning.fragments.databinding.DialogSearchHistoryBinding
import ru.androidlearning.presentation.fragments.search.TranslatedResultsListAdapter
import ru.androidlearning.presentation.navigation.DetailsFragmentScreen

class SearchHistoryDialogFragment : DialogFragment(R.layout.dialog_search_history), AndroidScopeComponent {

    override val scope: Scope by fragmentScope()
    private val viewBinding: DialogSearchHistoryBinding by viewBinding(DialogSearchHistoryBinding::bind)
    private val searchHistoryDialogViewModel: SearchHistoryDialogViewModel by viewModel()
    private val listAdapter: TranslatedResultsListAdapter by lazy { TranslatedResultsListAdapter(::onItemClick) }
    private val router: Router by inject()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            s.toString().takeIf { it.isNotBlank() }?.let { word ->
                searchHistoryDialogViewModel.search(word)
            }
        }

        override fun afterTextChanged(s: Editable?) {}
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        subscribeToLiveData()
    }

    private fun initViews() {
        with(viewBinding) {
            closeButton.setOnClickListener { dismiss() }
            enterWordEditText.addTextChangedListener(textWatcher)
            searchHistoryRecyclerView.apply {
                adapter = listAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
        }
    }

    private fun onItemClick(translatedWord: DictionaryPresentationDataModel.TranslatedWord) {
        router.navigateTo(DetailsFragmentScreen(translatedWord))
        dismiss()
    }

    private fun subscribeToLiveData() {
        searchHistoryDialogViewModel.getLiveData().observe(viewLifecycleOwner) { dataLoadingState -> renderData(dataLoadingState) }
    }

    private fun renderData(dataLoadingState: DataLoadingState<DictionaryPresentationDataModel>) {
        when (dataLoadingState) {
            is DataLoadingState.Error -> showError(dataLoadingState.error)
            is DataLoadingState.Loading -> {}
            is DataLoadingState.Success -> doOnSearchSuccess(dataLoadingState.data)
        }
    }

    private fun doOnSearchSuccess(searchData: DictionaryPresentationDataModel) {
        listAdapter.submitList(searchData.translatedWords)
        if (searchData.translatedWords.isNullOrEmpty()) {
            showNoDataLabel()
        } else {
            hideNoDataLabel()
        }
    }

    private fun showNoDataLabel() {
        viewBinding.noDataTextView.visibility = View.VISIBLE
    }

    private fun hideNoDataLabel() {
        viewBinding.noDataTextView.visibility = View.GONE
    }

    private fun showError(e: Throwable) {
        e.printStackTrace()
        Toast.makeText(requireContext(), getString(R.string.error_message_prefix) + e.message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        viewBinding.enterWordEditText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    companion object {
        fun newInstance() = SearchHistoryDialogFragment()
    }
}

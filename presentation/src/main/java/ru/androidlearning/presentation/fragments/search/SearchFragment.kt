package ru.androidlearning.presentation.fragments.search

import android.transition.ChangeBounds
import android.transition.Fade
import android.transition.TransitionManager
import android.transition.TransitionSet
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import by.kirich1409.viewbindingdelegate.viewBinding
import com.github.terrakok.cicerone.Router
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.stateViewModel
import ru.androidlearning.core.DataLoadingState
import ru.androidlearning.core.DictionaryPresentationDataModel
import ru.androidlearning.core.base_abstract_templates.BaseMVVMFragment
import ru.androidlearning.fragments.R
import ru.androidlearning.fragments.databinding.FragmentSearchBinding
import ru.androidlearning.presentation.fragments.InstantSearchFlow
import ru.androidlearning.presentation.fragments.search.dialog.SearchHistoryDialogFragment
import ru.androidlearning.presentation.navigation.DetailsFragmentScreen
import ru.androidlearning.presentation.navigation.HistoryFragmentScreen
import ru.androidlearning.presentation.navigation.StopwatchFragmentScreen
import ru.androidlearning.utils.network.NetworkState

private const val ANIMATION_DURATION_MS = 1000L

@FlowPreview
class SearchFragment : BaseMVVMFragment(R.layout.fragment_search) {

    private val searchFragmentViewModel: SearchFragmentViewModel by stateViewModel()
    private val viewBinding: FragmentSearchBinding by viewBinding(FragmentSearchBinding::bind)
    private val router: Router by inject()
    private val translatedResultsListAdapter: TranslatedResultsListAdapter by lazy { TranslatedResultsListAdapter(::onItemClick) }

    override fun initViews() {
        with(viewBinding) {
            enterWordTextInputLayout.setEndIconOnClickListener {
                searchFragmentViewModel.search(enterWordEditText.text.toString())
            }
            translationResultsRecyclerView.apply {
                adapter = translatedResultsListAdapter
                addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            }
            InstantSearchFlow.beginObserveEditTextChanges(enterWordEditText)
        }
        initToolbar()
    }

    private fun initToolbar() {
        (context as AppCompatActivity).apply {
            setSupportActionBar(viewBinding.toolbar)
        }
        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        InstantSearchFlow.endObserveEditTextChanges(viewBinding.enterWordEditText)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.open_history_item -> {
                router.navigateTo(HistoryFragmentScreen())
                true
            }
            R.id.search_history_item -> {
                SearchHistoryDialogFragment.newInstance().show(childFragmentManager, null)
                true
            }
            R.id.stopwatch_item -> {
                router.navigateTo(StopwatchFragmentScreen())
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }

    override fun observeToLiveData() {
        with(searchFragmentViewModel) {
            getLiveData().observe(viewLifecycleOwner) { dataLoadingState ->
                renderData(dataLoadingState)
            }
            networkStateLiveData.observe(viewLifecycleOwner) { networkState ->
                displayConnectionStatus(networkState)
            }
        }
    }

    override fun restoreViewState() {
        searchFragmentViewModel.restoreViewState()
    }

    private fun renderData(dataLoadingState: DataLoadingState<DictionaryPresentationDataModel>) {
        when (dataLoadingState) {
            is DataLoadingState.Error -> doOnTranslateError(dataLoadingState.error)
            is DataLoadingState.Loading -> doOnTranslateLoading()
            is DataLoadingState.Success -> doOnTranslateSuccess(dataLoadingState.data)
        }
    }

    private fun displayConnectionStatus(networkState: DataLoadingState<NetworkState>) {
        when (networkState) {
            is DataLoadingState.Error -> doOnGetNetworkStatusError(networkState.error)
            is DataLoadingState.Loading -> {}
            is DataLoadingState.Success -> doOnGetNetworkStatusSuccess(networkState.data)
        }
    }

    private fun doOnTranslateSuccess(dictionaryPresentationDataModel: DictionaryPresentationDataModel) {
        hideProgressBar()
        showTranslatedResult(dictionaryPresentationDataModel)
        if (dictionaryPresentationDataModel.translatedWords.isNullOrEmpty()) {
            showNoDataLabel()
        } else {
            hideNoDataLabel()
        }
    }

    private fun doOnTranslateLoading() {
        showProgressBar()
    }

    private fun doOnTranslateError(e: Throwable) {
        hideProgressBar()
        showError(e.message)
    }

    private fun onItemClick(translatedWord: DictionaryPresentationDataModel.TranslatedWord) {
        router.navigateTo(DetailsFragmentScreen(translatedWord))
    }

    private fun doOnGetNetworkStatusSuccess(networkState: NetworkState) {
        when (networkState) {
            NetworkState.CONNECTED -> doOnPresenceInternet()
            NetworkState.DISCONNECTED -> doOnNoInternet()
        }
    }

    private fun doOnGetNetworkStatusError(e: Throwable) {
        showError(getString(R.string.error_getting_connection_status_message) + e.message)
    }

    private fun showTranslatedResult(dictionaryPresentationDataModel: DictionaryPresentationDataModel) {
        translatedResultsListAdapter.submitList(dictionaryPresentationDataModel.translatedWords)
    }

    private fun showNoDataLabel() {
        viewBinding.noDataTextView.visibility = View.VISIBLE
    }

    private fun hideNoDataLabel() {
        viewBinding.noDataTextView.visibility = View.GONE
    }

    private fun showError(errorMessage: String?) {
        Toast.makeText(requireContext(), getString(R.string.error_message_prefix) + errorMessage, Toast.LENGTH_SHORT).show()
    }

    private fun showProgressBar() {
        viewBinding.includedLoadingSheet.loadingSheet.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        viewBinding.includedLoadingSheet.loadingSheet.visibility = View.GONE
    }

    private fun doOnNoInternet() {
        TransitionSet()
            .addTransition(ChangeBounds())
            .addTransition(Fade())
            .setOrdering(androidx.transition.TransitionSet.ORDERING_SEQUENTIAL)
            .setDuration(ANIMATION_DURATION_MS)
            .let { transitionSet -> TransitionManager.beginDelayedTransition(viewBinding.searchLayout, transitionSet) }
        viewBinding.noInternetBanner.visibility = View.VISIBLE
    }

    private fun doOnPresenceInternet() {
        TransitionSet()
            .addTransition(Fade())
            .addTransition(ChangeBounds())
            .setOrdering(androidx.transition.TransitionSet.ORDERING_SEQUENTIAL)
            .setDuration(ANIMATION_DURATION_MS)
            .let { transitionSet -> TransitionManager.beginDelayedTransition(viewBinding.searchLayout, transitionSet) }

        viewBinding.noInternetBanner.visibility = View.GONE
    }

    companion object {
        fun newInstance() = SearchFragment()
    }
}

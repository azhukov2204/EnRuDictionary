package ru.androidlearning.presentation.navigation

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.github.terrakok.cicerone.androidx.FragmentScreen
import kotlinx.coroutines.FlowPreview
import ru.androidlearning.presentation.fragments.search.SearchFragment

@FlowPreview
class SearchFragmentScreen : FragmentScreen {
    override fun createFragment(factory: FragmentFactory): Fragment =
        SearchFragment.newInstance()
}

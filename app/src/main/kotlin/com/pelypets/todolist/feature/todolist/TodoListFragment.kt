package com.pelypets.todolist.feature.todolist

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.pelypets.todolist.R
import com.pelypets.todolist.core.ui.applySystemBarsMargin
import com.pelypets.todolist.core.ui.applySystemBarsPadding
import com.pelypets.todolist.core.ui.collectWhenStarted
import com.pelypets.todolist.core.ui.emptyRes
import com.pelypets.todolist.core.ui.labelRes
import com.pelypets.todolist.core.ui.messageRes
import com.pelypets.todolist.core.ui.viewBinding
import com.pelypets.todolist.databinding.FragmentTodoListBinding
import com.pelypets.todolist.domain.model.Period
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TodoListFragment : Fragment(R.layout.fragment_todo_list) {

    private val viewModel: TodoListViewModel by viewModels()
    private val binding by viewBinding(FragmentTodoListBinding::bind)
    private val adapter = TodoAdapter { viewModel.onTodoClicked(it.id) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViews()

        collectWhenStarted(viewModel.uiState, ::render)
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is TodoListEvent.OpenTodo ->
                    findNavController().navigate(TodoListFragmentDirections.actionTodoListToTodoDetail(event.id))

                is TodoListEvent.ShowError -> Snackbar.make(binding.root, event.error.messageRes, Snackbar.LENGTH_LONG).show()
            }
        }
        // The view is recreated when the user comes back from the detail screen: the list may have changed.
        viewModel.onScreenShown()
    }

    private fun setUpViews() = with(binding) {
        toolbar.setOnMenuItemClickListener { item ->
            (item.itemId == R.id.action_logout).also { if (it) viewModel.onLogoutClicked() }
        }

        Period.entries.forEach { period -> tabs.addTab(tabs.newTab().setText(period.labelRes).setTag(period)) }
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = viewModel.onPeriodSelected(tab.tag as Period)
            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })

        list.adapter = adapter
        list.addItemDecoration(MaterialDividerItemDecoration(requireContext(), MaterialDividerItemDecoration.VERTICAL))
        list.applySystemBarsPadding(bottom = true)
        placeholder.applySystemBarsPadding(bottom = true)
        addButton.applySystemBarsMargin()
        addButton.setOnClickListener { viewModel.onAddClicked() }
        retryButton.setOnClickListener { viewModel.onRetry() }

        swipeRefresh.setOnRefreshListener { viewModel.onRefresh() }
        // The refresh layout wraps a FrameLayout, so it has to be told which child is actually scrolling.
        swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            val scrolling = if (list.isVisible) list else placeholder
            scrolling.canScrollVertically(-1)
        }
    }

    private fun render(state: TodoListUiState) = with(binding) {
        val selectedTab = tabs.getTabAt(state.period.ordinal)
        if (selectedTab?.isSelected == false) selectedTab.select()

        swipeRefresh.isRefreshing = state.isRefreshing
        progress.isVisible = state.isLoading
        adapter.submitList(state.todos)
        list.isVisible = !state.isLoading && state.error == null && !state.isEmpty

        placeholder.isVisible = !state.isLoading && (state.error != null || state.isEmpty)
        when {
            state.error != null -> {
                placeholderTitle.setText(state.error.messageRes)
                placeholderSubtitle.isVisible = false
                retryButton.isVisible = true
            }

            state.isEmpty -> {
                placeholderTitle.setText(state.period.emptyRes)
                placeholderSubtitle.isVisible = true
                retryButton.isVisible = false
            }
        }
    }
}

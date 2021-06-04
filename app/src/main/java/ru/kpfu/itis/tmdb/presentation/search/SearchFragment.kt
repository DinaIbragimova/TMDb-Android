package ru.kpfu.itis.tmdb.presentation.search

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import ru.kpfu.itis.tmdb.App
import ru.kpfu.itis.tmdb.R
import ru.kpfu.itis.tmdb.databinding.FragmentSearchBinding
import ru.kpfu.itis.tmdb.presentation.rv.search.SearchAdapter
import java.io.IOException
import javax.inject.Inject

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding

    @Inject
    lateinit var viewModel: SearchViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_search, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (activity?.application as App).appComponent.searchComponentFactory()
            .create(this)
            .inject(this)

        binding.myToolbar.inflateMenu(R.menu.nav_menu)
        initSearchView()
        initSubscribes()
    }

    private fun initSearchView(){
        val searchView = binding.myToolbar.menu.findItem(R.id.nav_search).actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{

            private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
            private var searchJob: Job? = null

            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchJob?.cancel()
                searchJob = coroutineScope.launch {
                    delay(500)
                    newText?.let{
                        if (it.isNotEmpty()){
                            viewModel.searchTvOrMovie(it)
                        }
                    }
                }
                return false
            }
        })
    }

    private fun initSubscribes(){
        with(viewModel) {
            progress().observe(viewLifecycleOwner, {
                binding.progressBar.isVisible = it
            })
            multiSearch().observe(viewLifecycleOwner, {results ->
                try {
                    val adapter = SearchAdapter(results.getOrThrow()){
                        navigateToDetailsFragment(it.id, it.type)
                    }
                    binding.searchRv.adapter = adapter
                    adapter.submitList(results.getOrThrow())

                } catch (ex: IOException){
                    Snackbar.make(
                            requireActivity().findViewById(android.R.id.content),
                            "no connection",
                            Snackbar.LENGTH_LONG
                    ).show()
                }
            })
        }
    }

    private fun navigateToDetailsFragment(id: Int, type: String){
        SearchFragmentDirections.actionSearchFragmentToDetailsMultiFragment(id, type).also {
            findNavController().navigate(it.actionId, it.arguments)
        }
    }

}
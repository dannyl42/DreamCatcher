package edu.vt.cs.cs5254.dreamcatcher

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamListBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamBinding
import java.util.UUID

private const val TAG = "Dreams"

class DreamListFragment : Fragment() {

    interface Callbacks {
        fun onDreamSelected(dreamId: UUID)
    }

    private var callbacks: Callbacks? = null
    private lateinit var dreamRecyclerView: RecyclerView
    private var allDreams: List<Dream> = emptyList()

    private var _binding: FragmentDreamListBinding? = null
    private val binding get() = _binding!!

    private val dreamListViewModel: DreamListViewModel by lazy {
        ViewModelProvider(this).get(DreamListViewModel::class.java)
    }

    private var adapter: DreamAdapter? = DreamAdapter(emptyList())

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callbacks = context as Callbacks?
    }

    override fun onCreate (savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add_dream -> {
                val dream = Dream()
                val dreamConceivedEntry = listOf(
                        DreamEntry(
                                text = "CONCEIVED",
                                kind = DreamEntryKind.CONCEIVED,
                                dreamId = dream.id
                        )
                )
                val dreamEntry = DreamWithEntries(dream, dreamConceivedEntry)
                dreamListViewModel.addDreamWithEntries(dreamEntry)
                callbacks?.onDreamSelected(dream.id)
                true
            } R.id.delete_all_dreams -> {
                dreamListViewModel.deleteAllDreamsInDatabase()
                true
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        _binding = FragmentDreamListBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dreamRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.dreamRecyclerView.adapter = adapter
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dreamListViewModel.dreamListLiveData.observe(
                viewLifecycleOwner,
                { dreamsFromViewModel ->
                    dreamsFromViewModel?.let {
                        Log.i(TAG, "Got dreams ${dreamsFromViewModel.size}")
                        allDreams = dreamsFromViewModel
                        updateUI(dreamsFromViewModel)
                    }
                }
        )
    }

    override fun onDetach() {
        super.onDetach()
        callbacks = null
    }

    private fun updateUI(dreams: List<Dream>) {
        adapter = DreamAdapter(dreams)
        binding.dreamRecyclerView.adapter = adapter
    }

     inner class DreamHolder(itemBinding: ListItemDreamBinding)
        :RecyclerView.ViewHolder(itemBinding.root), View.OnClickListener {

        private lateinit var dream: Dream

        private val tittleTextView: TextView = itemBinding.dreamItemTitle
        private val dateTextView: TextView = itemBinding.dreamItemDate
        private val dreamImageView: ImageView = itemBinding.dreamItemImage

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dream: Dream) {
            this.dream = dream
            tittleTextView.text = this.dream.title
            dateTextView.text = this.dream.date.toString()

            val date = dateTextView.text.split(" ")
            dateTextView.text = listOf(date[1],date[2],date[5]).joinToString(separator = " ")

            when {
                dream.isDeferred -> {
                dreamImageView.setImageResource(R.drawable.dream_deferred_icon)
                dreamImageView.tag = R.drawable.dream_deferred_icon
            }
            dream.isFulfilled -> {
                dreamImageView.setImageResource(R.drawable.dream_fulfilled_icon)
                dreamImageView.tag = R.drawable.dream_fulfilled_icon
            }
                else -> {
                    dreamImageView.setImageResource(0)
                    dreamImageView.tag = 0
                }
            }
        }

        override fun onClick(v: View?) {
            callbacks?.onDreamSelected(dream.id)
            }
        }

    private inner class DreamAdapter(var dreams: List<Dream>)
        :RecyclerView.Adapter<DreamHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : DreamHolder {
            val itemBinding = ListItemDreamBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamHolder(itemBinding)
        }

        override fun getItemCount()= dreams.size

        override fun onBindViewHolder(holder: DreamHolder, position: Int) {
            val dream = dreams[position]
            holder.bind(dream)
        }
        }

    companion object {
        fun newInstance(): DreamListFragment {
            return DreamListFragment()
        }
    }
}

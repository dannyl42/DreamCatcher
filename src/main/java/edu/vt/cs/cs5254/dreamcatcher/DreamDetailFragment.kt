package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import android.widget.Button
import android.widget.CheckBox
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.Companion.getScaledBitmap
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil.Companion.revokeCaptureImagePermissions
import java.io.File
import java.util.UUID

private const val TAG = "DreamDetailFragment"
private const val ARG_DREAM_ID = "dream_id"
private const val DIALOG_ADD_REFLECTION = "DialogAddReflection"
private const val REQUEST_ADD_REFLECTION = 0

private const val CONCEIVED_COLOR = "#0D94E5"
private const val REFLECTION_COLOR = "#FDFF43"
private const val FULFILLED_COLOR = "#006600"
private const val DEFERRED_COLOR = "#DC331B"

private const val DATE_FORMAT = "(MMM dd, yyyy)"


class DreamDetailFragment : Fragment(), AddReflectionDialog.Callbacks {

    private lateinit var photoFile: File
    private lateinit var photoUri: Uri
    private lateinit var fulfilledCheckBox: CheckBox
    private lateinit var deferredCheckBox: CheckBox
    private lateinit var dreamEntryRecyclerView: RecyclerView

    private var adapter: DreamEntryAdapter? = DreamEntryAdapter(emptyList())

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding: FragmentDreamDetailBinding get() = _binding!!

    lateinit var dreamWithEntries: DreamWithEntries

    private val detailViewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        Log.d(TAG, "args bundle dream ID: $dreamId")
        detailViewModel.loadDream(dreamId)
        dreamWithEntries = DreamWithEntries(Dream(), emptyList())
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dreamTitleText.setText(dreamWithEntries.dream.title)
        fulfilledCheckBox = binding.dreamFulfilledCheckbox
        deferredCheckBox = binding.dreamDeferredCheckbox

        dreamEntryRecyclerView = binding.dreamEntryRecyclerView
        dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)

        val itemTouchHelper = adapter?.let {
            SwipeToDeleteCallback()
        }?.let {
            ItemTouchHelper(it)
        }

        itemTouchHelper?.attachToRecyclerView(dreamEntryRecyclerView)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        detailViewModel.DreamWithEntriesLiveData.observe(
                viewLifecycleOwner,
                { dreamWithEntriesFromDetailViewModel ->
                    dreamWithEntriesFromDetailViewModel?.let {
                        this.dreamWithEntries = dreamWithEntriesFromDetailViewModel
                        photoFile = detailViewModel.getPhotoFile(dreamWithEntries)
                        photoUri = FileProvider.getUriForFile(
                                requireActivity(),
                                "edu.vt.cs.cs5254.dreamcatcher.fileprovider",
                                photoFile)

                        adapter = DreamEntryAdapter(dreamWithEntriesFromDetailViewModel.dreamEntries)
                        dreamEntryRecyclerView.adapter = adapter
                        updateUI()
                    }
                })
    }
    override fun onStart() {
        super.onStart()

        val titleWatcher = object : TextWatcher {

            override fun beforeTextChanged(
                    sequence: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int,
            ) {
            }

            override fun onTextChanged(
                    sequence: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int,
            ) {
                dreamWithEntries.dream.title = sequence.toString()
            }

            override fun afterTextChanged(sequence: Editable?) {}
        }
        binding.dreamTitleText.addTextChangedListener(titleWatcher)

        fulfilledCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                dreamWithEntries.dream.isFulfilled = isChecked
                updateUI()
                dreamEntryOption(isChecked, DreamEntryKind.FULFILLED)
    }
}
            deferredCheckBox.apply {
                setOnCheckedChangeListener { _, isChecked ->
                    dreamWithEntries.dream.isDeferred = isChecked
                    updateUI()
                    dreamEntryOption(isChecked, DreamEntryKind.DEFERRED)

            }

        }

        binding.addReflectionButton.setOnClickListener {
                AddReflectionDialog().apply {
                    setTargetFragment(this@DreamDetailFragment, REQUEST_ADD_REFLECTION)
                    show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ADD_REFLECTION)
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)

        val cameraAvailable = CameraUtil.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera Available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.share_dream -> {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, getDreamReport())
                        putExtra(Intent.EXTRA_SUBJECT, getString(R.string.dream_report_subject))
                    }.also { intent ->
                        val chooserIntent =
                                Intent.createChooser(intent, getString(R.string.send_report))
                        startActivity(chooserIntent)
                    }
                    true
                }
            R.id.take_dream_photo -> {
                val captureImageIntent = CameraUtil.createCaptureImageIntent(requireActivity(), photoUri)
                startActivity(captureImageIntent)
                true
            } else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getDreamReport(): String {
//        var dreamReport = ">> ${dreamWithEntries.dream.title} <<\n"
//        for (entry in dreamWithEntries.dreamEntries) {
//            val df = DateFormat.getMediumDateFormat(activity)
//            val dreamDate = df.format(entry.date)
//            dreamReport += "-" + entry.text + " (" + dreamDate + ")\n"
//        }

        val fulfilledString = if (dreamWithEntries.dream.isFulfilled) {
            getString(R.string.dream_fulfilled)
        } else {
            getString(R.string.dream_deferred)
        }
        val df = DateFormat.format(DATE_FORMAT, dreamWithEntries.dream.date).toString()
        val dreamReflections = getString(R.string.dream_reflection_1)

        var dreamReport = ">> ${dreamWithEntries.dream.title} <<\n" + getString(R.string.dream_conceived, df) + "\n" +
                "-" + getString(R.string.dream_reflection_1) + "\n" + "-" + getString(R.string.dream_reflection_2) + "\n" + "-" +
                getString(R.string.dream_reflection_3) + "\n" + fulfilledString


        return dreamReport

    }


    override fun onStop() {
        super.onStop()
        detailViewModel.saveDream(dreamWithEntries)
    }

    override fun onDetach() {
        super.onDetach()
        revokeCaptureImagePermissions(requireActivity(), photoUri)
    }


    override fun onReflectionProvided(reflectionText: String) {
        val newDreamEntry = DreamEntry(text = reflectionText, dreamId = dreamWithEntries.dream.id )
        detailViewModel.saveDream(dreamWithEntries)
        detailViewModel.addDreamEntry(newDreamEntry)
    }

    private fun dreamEntryOption(checked: Boolean, kind: DreamEntryKind) {
        detailViewModel.saveDream(dreamWithEntries)

        val text = if (kind == DreamEntryKind.FULFILLED) "FULFILLED" else "DEFERRED"

        if (checked) {
            val dreamEntry = DreamEntry(
                    text = text,
                    kind = kind,
                    dreamId = dreamWithEntries.dream.id
            )

            if (dreamWithEntries.dreamEntries.none { it.kind == kind }) {
                detailViewModel.addDreamEntry(dreamEntry)
            }
        } else {
                detailViewModel.deleteDreamEntry(dreamWithEntries.dream.id, kind)
            }
    }

    private fun updateUI() {
        binding.dreamTitleText.setText(dreamWithEntries.dream.title)

        deferredCheckBox.apply {
            isChecked = dreamWithEntries.dream.isDeferred
            jumpDrawablesToCurrentState()
        }
        fulfilledCheckBox.apply {
            isChecked = dreamWithEntries.dream.isFulfilled
            jumpDrawablesToCurrentState()
        }
        deferredCheckBox.isEnabled = !fulfilledCheckBox.isChecked
        fulfilledCheckBox.isEnabled = !deferredCheckBox.isChecked
        binding.addReflectionButton.isEnabled = !fulfilledCheckBox.isChecked

        updatePhotoView()

    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, requireActivity())
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }

    }

    private fun buttonColor(button: Button, backgroundColor: String, textColor: Int = Color.WHITE) {
        button.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor(backgroundColor))
        button.setTextColor(textColor)
    }

    private fun updateEntryButton(button: Button, entry: DreamEntry) {
        button.isEnabled = false
        button.text = entry.text

        when (entry.kind) {
            DreamEntryKind.CONCEIVED -> {
                buttonColor(button, CONCEIVED_COLOR)
            }

            DreamEntryKind.REFLECTION -> {
                buttonColor(button, REFLECTION_COLOR, Color.BLACK )
                val dF = DateFormat.getMediumDateFormat((activity))
                val dateMade = dF.format(dreamWithEntries.dream.date)
                button.text = "$dateMade: ${entry.text}"
            }

            DreamEntryKind.DEFERRED -> {
                buttonColor(button, DEFERRED_COLOR)
            }

            DreamEntryKind.FULFILLED -> {
                buttonColor(button, FULFILLED_COLOR)
            }
        }

    }

    inner class DreamEntryHolder(view: View) :RecyclerView.ViewHolder(view) {
        private lateinit var dreamEntry: DreamEntry
        private val dreamEntryButton: Button = itemView.findViewById(R.id.dream_entry_button)

        fun bind(dreamEntry: DreamEntry) {
            this.dreamEntry = dreamEntry
            updateEntryButton(dreamEntryButton, dreamEntry)
        }

    }

    inner class DreamEntryAdapter(private var dreamEntries: List<DreamEntry>) :
            RecyclerView.Adapter<DreamEntryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
            val view = layoutInflater.inflate(R.layout.list_item_dream_entry, parent, false)
            return DreamEntryHolder(view)
        }

        override fun getItemCount(): Int = dreamEntries.size

        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }
    }

    private fun deleteItem(position: Int) {
        val dreamEntry = dreamWithEntries.dreamEntries[position]
        if (dreamEntry.kind == DreamEntryKind.REFLECTION) {
            detailViewModel.deleteDreamEntry(dreamEntry.id)
        }
    }

    inner class SwipeToDeleteCallback: ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            deleteItem(position)
        }

        override fun getSwipeDirs(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            val dreamEntry = dreamWithEntries.dreamEntries[viewHolder.adapterPosition]
            return if (dreamEntry.kind == DreamEntryKind.REFLECTION) {
                ItemTouchHelper.LEFT
            } else {
                0
            }
        }
    }

    companion object {
        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args }

        }
    }


}



import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.fyp_duriantrackingsystem.databinding.ItemDurianBinding
import com.example.fyp_duriantrackingsystem.model.Durian

class DurianAdapter : ListAdapter<Durian, DurianAdapter.DurianViewHolder>(DurianDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DurianViewHolder {
        val binding = ItemDurianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DurianViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DurianViewHolder, position: Int) {
        val durian = getItem(position)
        holder.bind(durian)


    }

    inner class DurianViewHolder(private val binding: ItemDurianBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(durian: Durian) {
            binding.batchCode.text = durian.batchCode.toString()
            binding.durianType.text = durian.durianType
            binding.weightInGram.text = durian.weightInGram.toString()
            binding.currentOwner.text = durian.currentOwner
            binding.state.text = durian.state.name
            binding.farmLocation.text = durian.farmLocation
            binding.harvestDate.text = durian.addedTimestamp
            binding.ownedTimestamp.text = durian.ownedTimestamp
            binding.arrivedAtDistributor.text = durian.arrivedAtDistributor
            binding.arrivedAtRetailer.text = durian.arrivedAtRetailer
            binding.distributorName.text = durian.distributorName
            binding.retailerName.text = durian.retailerName

            binding.copyCurrentOwnerImageView.setOnClickListener {
                copyToClipboard(binding.currentOwner.text.toString(), it.context)
            }

            binding.copyBatchCodeImageView.setOnClickListener {
                copyToClipboard(binding.batchCode.text.toString(), it.context)
            }
        }
    }


    // Function to copy text to clipboard
    private fun copyToClipboard(text: String, context: Context) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Text", text)
        clipboardManager.setPrimaryClip(clip)

        // Show a toast message indicating that the text has been copied
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }


    // DiffUtil for efficient updates to the list
    class DurianDiffCallback : DiffUtil.ItemCallback<Durian>() {
        override fun areItemsTheSame(oldItem: Durian, newItem: Durian): Boolean {
            return oldItem.batchCode == newItem.batchCode  // Assuming batchCode is unique
        }

        override fun areContentsTheSame(oldItem: Durian, newItem: Durian): Boolean {
            return oldItem == newItem
        }
    }

}
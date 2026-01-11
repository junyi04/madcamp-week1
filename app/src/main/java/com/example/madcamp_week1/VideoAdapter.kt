package com.example.madcamp_week1

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.madcamp_week1.databinding.ItemVideoBinding
import com.example.madcamp_week1.databinding.ItemVideoGridBinding
import com.example.madcamp_week1.databinding.ItemVideoTopBinding
import com.example.madcamp_week1.databinding.LayoutTop3ContainerBinding

class VideoAdapter(
    private var items: List<VideoData>,
    private val isGridMode: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_TOP_3 = 0
    private val TYPE_ITEM = 1

    // 데이터 업데이트 함수 (이 부분이 누락되어 에러가 발생함)
    fun updateData(newItems: List<VideoData>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        if (isGridMode) return TYPE_ITEM
        return if (position == 0) TYPE_TOP_3 else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TOP_3) {
            Top3ViewHolder(LayoutTop3ContainerBinding.inflate(inflater, parent, false))
        } else {
            if (isGridMode) {
                GridItemViewHolder(ItemVideoGridBinding.inflate(inflater, parent, false))
            } else {
                ListItemViewHolder(ItemVideoBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is Top3ViewHolder -> holder.bind(items.take(3))
            is GridItemViewHolder -> holder.bind(items[position], position + 1)
            is ListItemViewHolder -> {
                val dataIndex = if (!isGridMode) position + 2 else position
                if (dataIndex in items.indices) {
                    holder.bind(items[dataIndex], dataIndex + 1)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        if (isGridMode) return items.size
        return if (items.isEmpty()) 0 else if (items.size <= 3) 1 else 1 + (items.size - 3)
    }

    inner class Top3ViewHolder(private val binding: LayoutTop3ContainerBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(topItems: List<VideoData>) {
            binding.layoutTop3Root.removeAllViews()
            val podiumOrder = listOf(1, 0, 2)

            podiumOrder.forEach { index ->
                if (index >= topItems.size) return@forEach
                val item = topItems[index]
                val rank = index + 1

                val itemBinding = ItemVideoTopBinding.inflate(LayoutInflater.from(binding.root.context), binding.layoutTop3Root, false)

                applyPodiumDesign(itemBinding, rank)
                itemBinding.tvTitle.text = item.title
                itemBinding.tvStats.text = "+${formatCount(item.views)}"
                setImage(itemBinding.ivThumbnail, item.imageFile)

                itemBinding.root.setOnClickListener {
                    it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                }
                binding.layoutTop3Root.addView(itemBinding.root)
            }
        }

        private fun applyPodiumDesign(itemBinding: ItemVideoTopBinding, rank: Int) {
            val lp = itemBinding.root.layoutParams as LinearLayout.LayoutParams
            lp.height = dpToPx(210)
            lp.weight = 1f
            itemBinding.root.layoutParams = lp

            itemBinding.root.translationY = if (rank == 1) -dpToPx(30).toFloat() else 0f

            when (rank) {
                1 -> {
                    itemBinding.ivCrownBadge.setImageResource(R.drawable.ic_crown_gold)

                    itemBinding.cardContainer.strokeColor =
                        ContextCompat.getColor(itemBinding.root.context, R.color.rank_gold)
                }
                2 -> {
                    itemBinding.ivCrownBadge.setImageResource(R.drawable.ic_crown_silver)

                    itemBinding.cardContainer.strokeColor =
                        ContextCompat.getColor(itemBinding.root.context, R.color.rank_silver)
                }
                3 -> {
                    itemBinding.ivCrownBadge.setImageResource(R.drawable.ic_crown_bronze)

                    itemBinding.cardContainer.strokeColor =
                        ContextCompat.getColor(itemBinding.root.context, R.color.rank_bronze)
                }
            }

            itemBinding.tvRank.text = rank.toString()
        }
    }

    inner class GridItemViewHolder(private val binding: ItemVideoGridBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoData, rank: Int) {
            binding.tvRank.text = rank.toString()
            binding.tvTitle.text = item.title
            binding.tvAuthor.text = item.author
            setImage(binding.ivThumbnail, item.imageFile)

            binding.root.setOnClickListener {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            }
        }
    }

    inner class ListItemViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoData, rank: Int) {
            binding.tvRank.text = rank.toString()
            binding.tvTitle.text = item.title
            binding.tvAuthor.text = item.author
            binding.tvStats.text = "조회수 ${formatCount(item.views)}"
            setImage(binding.ivThumbnail, item.imageFile)
            binding.root.setOnClickListener {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            }
        }
    }

    private fun dpToPx(dp: Int): Int = (dp * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
    private fun formatCount(count: Long): String = if (count >= 10000) "${count / 10000}만" else count.toString()
    private fun setImage(imageView: ImageView, url: String) {
        Glide.with(imageView.context).load(url).centerCrop().into(imageView)
    }
}
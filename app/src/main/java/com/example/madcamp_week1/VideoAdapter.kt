package com.example.madcamp_week1

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Glide 임포트 추가
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

    override fun getItemViewType(position: Int): Int {
        if (isGridMode) return TYPE_ITEM
        return if (position == 0) TYPE_TOP_3 else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_TOP_3) {
            val binding = LayoutTop3ContainerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            Top3ViewHolder(binding)
        } else {
            if (isGridMode) {
                val binding = ItemVideoGridBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                GridItemViewHolder(binding)
            } else {
                val binding = ItemVideoBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ListItemViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is Top3ViewHolder -> {
                holder.bind(items.take(3))
            }

            is GridItemViewHolder -> {
                holder.bind(items[position])
            }

            is ListItemViewHolder -> {
                // top3 이후 시작 인덱스 (기존 로직 유지)
                val rank = position + 3
                val itemIndex = position + 2
                if (itemIndex in items.indices) {
                    holder.bind(items[itemIndex], rank)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        if (isGridMode) return items.size
        if (items.isEmpty()) return 0
        if (items.size <= 3) return 1
        return 1 + (items.size - 3)
    }

    fun updateData(newItems: List<VideoData>) {
        items = newItems
        notifyDataSetChanged()
    }

    // =========================
    // Top3 (메인 상단 가로 스크롤)
    // =========================
    inner class Top3ViewHolder(
        private val binding: LayoutTop3ContainerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(topItems: List<VideoData>) {
            binding.layoutTop3Root.removeAllViews()

            topItems.forEachIndexed { index, item ->
                val itemBinding = ItemVideoTopBinding.inflate(
                    LayoutInflater.from(binding.root.context),
                    binding.layoutTop3Root,
                    false
                )

                setRankBadge(itemBinding, index + 1)
                itemBinding.tvTitle.text = item.title
                itemBinding.tvStats.text = "${item.views} 조회"

                setImage(itemBinding.ivThumbnail, item.imageFile)

                binding.layoutTop3Root.addView(itemBinding.root)
            }
        }
    }

    // =========================
    // Grid (카테고리)
    // =========================
    inner class GridItemViewHolder(
        private val binding: ItemVideoGridBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoData) {
            binding.tvTitle.text = item.title
            binding.tvAuthor.text = item.author
            setImage(binding.ivThumbnail, item.imageFile)
        }
    }

    // =========================
    // List (Top3 아래 세로 리스트)
    // =========================
    inner class ListItemViewHolder(
        private val binding: ItemVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: VideoData, rank: Int) {
            binding.tvRank.text = rank.toString()
            binding.tvTitle.text = item.title
            binding.tvAuthor.text = item.author
            binding.tvStats.text = "조회수 ${item.views} • 좋아요 ${item.likes}"
            setImage(binding.ivThumbnail, item.imageFile)
        }
    }

    // =========================
    // Utils
    // =========================
    private fun setRankBadge(binding: ItemVideoTopBinding, rank: Int) {
        binding.tvRank.text = rank.toString()
        val badgeColor = when (rank) {
            1 -> "#F2B807" // 금색
            2 -> "#B4B4B4" // 은색
            3 -> "#CD7F32" // 동색
            else -> "#80000000"
        }
        binding.tvRank.background.setTint(Color.parseColor(badgeColor))
    }

    // 실시간 이미지 로딩
    private fun setImage(imageView: ImageView, imageUrl: String) {
        Glide.with(imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.notfound404) // 로딩 스피너
            .error(R.drawable.notfound404)
            .centerCrop()
            .into(imageView)
    }
}
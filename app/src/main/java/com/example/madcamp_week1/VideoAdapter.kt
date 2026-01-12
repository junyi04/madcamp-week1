package com.example.madcamp_week1

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.madcamp_week1.databinding.ItemVideoBinding
import com.example.madcamp_week1.databinding.LayoutCategoryContainerBinding
import com.example.madcamp_week1.databinding.LayoutTop10ContainerBinding

class VideoAdapter(
    private var items: List<VideoData>,
    private val isCategoryMode: Boolean = false,
    private var currentCategoryName: String = "Dance"
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_TOP_CONTAINER = 0
    private val TYPE_ITEM = 1

    fun updateCategoryData(newItems: List<VideoData>, categoryName: String) {
        this.items = newItems
        this.currentCategoryName = categoryName
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_TOP_CONTAINER else TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_TOP_CONTAINER) {
            if (isCategoryMode) {
                CategoryContainerViewHolder(
                    LayoutCategoryContainerBinding.inflate(inflater, parent, false)
                )
            } else {
                TopContainerViewHolder(
                    LayoutTop10ContainerBinding.inflate(inflater, parent, false)
                )
            }
        } else {
            ListItemViewHolder(ItemVideoBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryContainerViewHolder -> {
                // 상단 타이틀 텍스트를 현재 카테고리로 변경
                holder.binding.tvCategoryTitle.text = "$currentCategoryName"
            }
            is ListItemViewHolder -> {
                val dataIndex = position - 1
                if (dataIndex in items.indices) {
                    holder.bind(items[dataIndex], dataIndex + 1)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return if (items.isEmpty()) 0 else items.size + 1
    }


    inner class TopContainerViewHolder(binding: LayoutTop10ContainerBinding) : RecyclerView.ViewHolder(binding.root)

    inner class CategoryContainerViewHolder(val binding: LayoutCategoryContainerBinding)
        : RecyclerView.ViewHolder(binding.root)

    inner class ListItemViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: VideoData, rank: Int) {
            binding.tvRank.text = rank.toString()
            binding.tvTitle.text = item.title
            binding.tvStats.text = formatCount(item.views)
            binding.tvTag.text = item.category ?: "밈"

            setImage(binding.ivThumbnail, item.imageFile)

            binding.btnView.setOnClickListener {
                it.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            }

            binding.btnLike.setOnClickListener {
                Toast.makeText(it.context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatCount(count: Long): String {
        return if (count >= 1000) "${count / 1000}K" else count.toString()
    }

    private fun setImage(imageView: ImageView, url: String) {
        Glide.with(imageView.context).load(url).centerCrop().into(imageView)
    }
}
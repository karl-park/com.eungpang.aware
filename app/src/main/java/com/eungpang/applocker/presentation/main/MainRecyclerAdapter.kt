package com.eungpang.applocker.presentation.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eungpang.snstimechecker.databinding.ItemMainSnsBinding
import com.eungpang.applocker.domain.item.Item

class MainRecyclerAdapter(
        items: List<Item>,
        private val itemHandler: ItemHandler
) : RecyclerView.Adapter<MainRecyclerAdapter.MainRecyclerViewHolder>() {
    private val _items: MutableList<Item> = items.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemMainSnsBinding.inflate(layoutInflater, parent, false)
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemId(position: Int): Long {
        return _items[position].packageName.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        holder.bind(_items[position], itemHandler)
    }

    override fun getItemCount(): Int = _items.size

    class MainRecyclerViewHolder(
            private val binding: ItemMainSnsBinding,
    ): RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Item, itemHandler: ItemHandler) {
            binding.apply {
                this.item = item
                this.handler = itemHandler

                executePendingBindings()
            }
        }
    }
}

package com.shaan.androiduicomponents

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.shaan.androiduicomponents.adapters.NotificationAdapter
import com.shaan.androiduicomponents.databinding.ActivityNotificationListBinding
import com.shaan.androiduicomponents.managers.NotificationManager as CustomNotificationManager
import com.shaan.androiduicomponents.models.Notification

class NotificationListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationListBinding
    private lateinit var adapter: NotificationAdapter
    private val itemTouchHelper by lazy { createItemTouchHelper() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupChips()
        loadNotifications()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        adapter = NotificationAdapter(
            onItemClick = { notification -> 
                startActivity(Intent(this, UniversityDetailsActivity::class.java).apply {
                    putExtra("universityName", notification.universityName)
                })
            },
            onItemDismiss = { notification ->
                TODO()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationListActivity)
            adapter = this@NotificationListActivity.adapter
            
            // Add divider
            addItemDecoration(
                DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            )
            
            // Add swipe and drag support
            itemTouchHelper.attachToRecyclerView(this)
        }
    }

    private fun createItemTouchHelper(): ItemTouchHelper {
        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                adapter.moveItem(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                TODO()
//                val position = viewHolder.bindingAdapterPosition
//                val notification = adapter.getItem(position)
//
//                adapter.removeItem(position)
//                showUndoSnackbar(notification, position)
            }
        })
    }

    private fun showUndoSnackbar(notification: Notification, position: Int) {
        Snackbar.make(
            binding.root,
            "Notification dismissed",
            Snackbar.LENGTH_LONG
        ).apply {
            setAction("UNDO") {
                adapter.addItem(position, notification)
            }
            setActionTextColor(ContextCompat.getColor(this@NotificationListActivity, R.color.primary))
            show()
        }
    }

    private fun setupChips() {
        binding.filterChipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.allChip -> filterNotifications(null)
                R.id.deadlinesChip -> filterNotifications("deadline")
                R.id.updatesChip -> filterNotifications("update")
            }
        }
    }

    private fun filterNotifications(type: String?) {
        val filtered = if (type == null) {
            CustomNotificationManager.getNotifications(this)
        } else {
            CustomNotificationManager.getNotifications(this).filter { it.type == type }
        }
        
        adapter.submitList(filtered)
        updateEmptyState(filtered.isEmpty())
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun loadNotifications() {
        val notifications = CustomNotificationManager.getNotifications(this)
        adapter.submitList(notifications)
        updateEmptyState(notifications.isEmpty())
    }
}
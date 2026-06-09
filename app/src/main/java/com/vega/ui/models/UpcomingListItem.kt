package com.vega.ui.models

import com.vega.data.database.Task

sealed class UpcomingListItem {
    data class Header(val id: String, val title: String) : UpcomingListItem()
    data class TaskItem(val task: Task) : UpcomingListItem()
}

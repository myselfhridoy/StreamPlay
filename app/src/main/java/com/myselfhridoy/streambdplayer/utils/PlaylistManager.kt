package com.myselfhridoy.streambdplayer.utils

import com.myselfhridoy.streambdplayer.data.models.Channel

object PlaylistManager {
    var currentPlaylist: List<Channel> = emptyList()
    var currentIndex: Int = -1

    fun playNext(): Channel? {
        if (currentIndex < currentPlaylist.size - 1) {
            currentIndex++
            return currentPlaylist[currentIndex]
        }
        return null
    }

    fun playPrevious(): Channel? {
        if (currentIndex > 0) {
            currentIndex--
            return currentPlaylist[currentIndex]
        }
        return null
    }
}

import os
import re

app_dir = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer"
player_file = os.path.join(app_dir, "ui/screens/player/PlayerScreen.kt")
with open(player_file, "r", encoding="utf-8") as f:
    player_code = f.read()

# Fix 1: Make sure zIndex is imported correctly
if "import androidx.compose.ui.zIndex.zIndex" not in player_code:
    player_code = player_code.replace("import androidx.compose.ui.Alignment", "import androidx.compose.ui.Alignment\nimport androidx.compose.ui.zIndex.zIndex")

# Fix 3: ExoPlayer setMediaSource instead of setMediaSourceFactory
old_exo_set = """            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(finalHeaders)
            }
            exoPlayer.setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory))

            val mediaItemBuilder = MediaItem.Builder().setUri(finalUri)"""

new_exo_set = """            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setDefaultRequestProperties(finalHeaders)
            }
            val mediaSourceFactory = DefaultMediaSourceFactory(context).setDataSourceFactory(httpDataSourceFactory)
            val mediaItemBuilder = MediaItem.Builder().setUri(finalUri)"""
player_code = player_code.replace(old_exo_set, new_exo_set)

old_set_item = """            exoPlayer.setMediaItem(mediaItemBuilder.build())
            exoPlayer.prepare()"""

new_set_item = """            val mediaSource = mediaSourceFactory.createMediaSource(mediaItemBuilder.build())
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()"""
player_code = player_code.replace(old_set_item, new_set_item)


# Check where headers is declared.
# In my previous script, I replaced:
# eff_old = """    LaunchedEffect(mediaUrl...
# So let's just make sure finalHeaders is correct.
# Wait, if headers is declared below LaunchedEffect, we need to move it up.
# Let's inspect the code.
with open(player_file, "w", encoding="utf-8") as f:
    f.write(player_code)

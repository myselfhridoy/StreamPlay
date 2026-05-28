package com.myselfhridoy.streambdplayer.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.tv.material3.DrawerValue
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.NavigationDrawer
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.Text
import com.myselfhridoy.streambdplayer.ui.theme.BackgroundDark
import com.myselfhridoy.streambdplayer.ui.theme.SurfaceDark

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvHomeScreen(navController: NavController) {
    var selectedItem by remember { mutableIntStateOf(0) }

    NavigationDrawer(
        drawerContent = { drawerValue ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(if (drawerValue == DrawerValue.Open) SurfaceDark else Color.Transparent)
                    .padding(16.dp)
            ) {
                androidx.tv.foundation.lazy.list.TvLazyColumn {
                    items(navItems.size) { index ->
                        val item = navItems[index]
                        NavigationDrawerItem(
                            selected = selectedItem == index,
                            onClick = { selectedItem = index },
                            leadingContent = {
                                Icon(item.icon, contentDescription = item.title)
                            }
                        ) {
                            Text(item.title)
                        }
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(start = 80.dp) // Leave space for closed drawer
        ) {
            Text(text = "TV View: \${navItems[selectedItem].title}", color = Color.White)
        }
    }
}

import re

# 1. Update ChannelBrowserScreen.kt
file_path_1 = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/browser/ChannelBrowserScreen.kt"
with open(file_path_1, "r", encoding="utf-8") as f:
    content_1 = f.read()

if "import androidx.compose.ui.graphics.Brush" not in content_1:
    content_1 = content_1.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush")

old_chip = """@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) Color.White else SurfaceDark,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.Black else Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}"""

new_chip = """@Composable
fun CategoryChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundModifier = if (isSelected) {
        Modifier.background(
            Brush.horizontalGradient(
                colors = listOf(Color(0xFFE50914), Color(0xFF9E060E))
            ),
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        Modifier.background(SurfaceDark, shape = RoundedCornerShape(20.dp))
    }

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .then(backgroundModifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}"""
content_1 = content_1.replace(old_chip, new_chip)
with open(file_path_1, "w", encoding="utf-8") as f:
    f.write(content_1)

# 2. Update TmdbHomeScreen.kt
file_path_2 = "d:/Projects/StreamPlay/app/src/main/java/com/myselfhridoy/streambdplayer/ui/screens/home/TmdbHomeScreen.kt"
with open(file_path_2, "r", encoding="utf-8") as f:
    content_2 = f.read()

if "import androidx.compose.ui.graphics.Brush" not in content_2:
    content_2 = content_2.replace("import androidx.compose.ui.graphics.Color", "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.Brush")

old_card = """@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = "${TmdbApi.IMAGE_BASE_URL}${item.posterPath}",
            contentDescription = item.title ?: item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Rating Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format("%.1f", item.voteAverage),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}"""

new_card = """@Composable
fun MediaCard(item: MediaItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .height(195.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = "${TmdbApi.IMAGE_BASE_URL}${item.posterPath}",
            contentDescription = item.title ?: item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        // Gradient overlay for title
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent, Color(0xDD000000))
                    )
                )
        )
        
        // Title text at bottom
        Text(
            text = item.title ?: item.name ?: "",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        )
        
        // Rating Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .background(Color(0x99000000), RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = String.format("%.1f", item.voteAverage),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}"""
content_2 = content_2.replace(old_card, new_card)
with open(file_path_2, "w", encoding="utf-8") as f:
    f.write(content_2)

print("Updated cards.")

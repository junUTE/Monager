package com.monager.app.ui.categories

import com.monager.app.ui.components.tactile
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.repository.CategoryNode
import com.monager.app.ui.CategoriesViewModel
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.Tone
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc

private val Tabs = listOf("Expense", "Income")
private const val NONE = -1L
private const val NEW = 0L

@Composable
fun CategoriesScreen(vm: CategoriesViewModel, onBack: () -> Unit) {
    val c = mc
    val tree by vm.tree.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableLongStateOf(NONE) }
    var editId by rememberSaveable { mutableLongStateOf(NONE) }      // NONE closed, NEW creating, else editing that id
    var newParent by rememberSaveable { mutableLongStateOf(NONE) }   // parent for a new subcategory

    val all = tree.flatMap { listOf(it.parent) + it.children }
    val editing: CategoryEntity? = all.firstOrNull { it.id == editId }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "h") {
                ScreenHeader("Categories", "Arrange and customize", onBack = onBack, right = {
                    AppIconButton(MIcon.Plus, "Add category", { newParent = NONE; editId = NEW }, c.primary)
                })
            }
            item(key = "tabs") {
                SegmentedTabs(Tabs, tab, { tab = it; expanded = NONE; vm.setType(if (it == 0) TxType.EXPENSE else TxType.INCOME) })
            }
            item(key = "hint") {
                Text("Open a category to add sub-categories. Tap ✎ to edit, reorder or delete.", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp))
            }
            items(tree, key = { it.parent.id }, contentType = { "cat" }) { node ->
                CategoryGroup(
                    node, open = node.parent.id == expanded,
                    onToggle = { expanded = if (expanded == node.parent.id) NONE else node.parent.id },
                    onEdit = { editId = it },
                    onAddChild = { newParent = node.parent.id; editId = NEW },
                )
            }
            item(key = "add") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp)
                        .border(1.dp, c.primary, RoundedCornerShape(13.dp))
                        .tactile(RoundedCornerShape(13.dp), scale = .98f) { newParent = NONE; editId = NEW },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    MIconView(MIcon.Plus, c.primary, size = 18.dp)
                    Text("  Add category", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (editId == NEW || editing != null) {
            val parentName = all.firstOrNull { it.id == newParent }?.name
            CategoryEditSheet(
                existing = editing,
                parentName = if (editing == null) parentName else null,
                onSave = { name, icon, tone ->
                    vm.save(editing?.id ?: NEW, if (editing == null && newParent != NONE) newParent else null, name, icon, tone) { editId = NONE }
                },
                onDelete = if (editing == null) null else { onResult -> vm.delete(editing.id) { ok -> if (ok) editId = NONE; onResult(ok) } },
                onMove = if (editing == null) null else { delta -> vm.move(editing.id, delta) },
                onClose = { editId = NONE },
            )
        }
    }
}

@Composable
private fun CategoryGroup(node: CategoryNode, open: Boolean, onToggle: () -> Unit, onEdit: (Long) -> Unit, onAddChild: () -> Unit) {
    val c = mc
    val row = node.parent
    // Clipped so the header / "add sub-category" ripples follow the card corners.
    Column(Modifier.fillMaxWidth().raised(radius = 14.dp, depth = 3.dp).clip(RoundedCornerShape(14.dp))) {
        Row(
            Modifier.fillMaxWidth().height(57.dp).clickable(onClick = onToggle).padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            CategoryIcon(MIcon.fromKey(row.iconKey), Tone.fromKey(row.toneKey), size = 36.dp)
            Text(row.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            MIconView(MIcon.ChevronRight, c.muted, Modifier.graphicsLayer { rotationZ = if (open) 90f else 0f }, 17.dp)
            AppIconButton(MIcon.Edit, "Edit ${row.name}", { onEdit(row.id) }, c.muted)
        }
        AnimatedVisibility(open) {
            Column {
                node.children.forEach { sub ->
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).padding(start = 33.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        CategoryIcon(MIcon.fromKey(sub.iconKey), Tone.fromKey(sub.toneKey), size = 28.dp)
                        Text(sub.name, color = c.text, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        AppIconButton(MIcon.Edit, "Edit ${sub.name}", { onEdit(sub.id) }, c.muted)
                    }
                }
                Row(
                    Modifier.fillMaxWidth().height(40.dp).clickable(onClick = onAddChild).padding(start = 33.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MIconView(MIcon.Plus, c.primary, size = 16.dp)
                    Text("Add sub-category", color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

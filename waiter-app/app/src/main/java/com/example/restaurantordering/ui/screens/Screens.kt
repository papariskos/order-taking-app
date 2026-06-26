package com.example.restaurantordering.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import com.example.restaurantordering.data.model.*
import com.example.restaurantordering.ui.viewmodels.AuthState
import com.example.restaurantordering.ui.viewmodels.AuthViewModel
import com.example.restaurantordering.ui.viewmodels.OrderViewModel

// Premium color palette for Dark Theme
val PrimaryDark = Color(0xFF030712)
val SecondaryDark = Color(0xFF0B0F19)
val AccentCyan = Color(0xFF06B6D4)
val AccentIndigo = Color(0xFF6366F1)
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TableGreen = Color(0xFF10B981)
val TableRed = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    var username by remember { mutableStateOf("waiter1") }
    var password by remember { mutableStateOf("waiterpassword") }
    var serverIp by remember { mutableStateOf("order-taking-app-production.up.railway.app") }
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
        } else if (authState is AuthState.Error) {
            Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1B4B), PrimaryDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(360.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryDark),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock",
                    tint = AccentCyan,
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF083344), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Σύνδεση Σερβιτόρου",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Restaurant Ordering App",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Όνομα Χρήστη", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Κωδικός Πρόσβασης", color = TextSecondary) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    label = { Text("IP Server / Port", color = TextSecondary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { authViewModel.login(username, password, serverIp) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Είσοδος", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaSelectionScreen(
    orderViewModel: OrderViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onTableSelected: () -> Unit,
    onEditOrder: (Order) -> Unit
) {
    val activeOrders by orderViewModel.activeOrders.collectAsState()
    val zones = listOf("Σάλα", "Μπαλκόνι", "1ος Όροφος", "Πισίνα")
    var selectedZoneIdx by remember { mutableStateOf(0) }
    var selectedTabIdx by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RestoWaiter", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark),
                actions = {
                    IconButton(onClick = { 
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = TableRed)
                    }
                }
            )
        },
        containerColor = PrimaryDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Main tabs: "Τραπέζια" and "Ενεργές Παραγγελίες"
            TabRow(
                selectedTabIndex = selectedTabIdx,
                containerColor = SecondaryDark,
                contentColor = AccentCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIdx == 0,
                    onClick = { selectedTabIdx = 0 },
                    text = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Τραπέζια", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                )
                Tab(
                    selected = selectedTabIdx == 1,
                    onClick = { selectedTabIdx = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ενεργές (${activeOrders.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedTabIdx == 0) {
                // Table View
                TabRow(
                    selectedTabIndex = selectedZoneIdx,
                    containerColor = PrimaryDark,
                    contentColor = AccentCyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    zones.forEachIndexed { idx, zone ->
                        Tab(
                            selected = selectedZoneIdx == idx,
                            onClick = { selectedZoneIdx = idx },
                            text = { Text(zone, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val currentZone = zones[selectedZoneIdx]
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(15) { index ->
                        val tableId = "${currentZone[0]}${index + 1}" // e.g. Σ1, Μ5, 1, Π8
                        val tableOrder = activeOrders.find { it.tableId == tableId && it.zone == currentZone }
                        val hasActiveOrder = tableOrder != null
                        val tableColor = if (hasActiveOrder) TableRed else TableGreen

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SecondaryDark)
                                .border(
                                    width = if (hasActiveOrder) 2.dp else 1.dp,
                                    color = tableColor,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable {
                                    orderViewModel.selectTable(currentZone, tableId)
                                    onTableSelected()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "Τραπέζι",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = tableId,
                                    color = TextPrimary,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (hasActiveOrder && tableOrder != null) {
                                    Text(
                                        text = "${String.format("%.2f", tableOrder.totalPrice)} €",
                                        color = TableRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                } else {
                                    Text(
                                        text = "Ελεύθερο",
                                        color = TableGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Active Orders List View
                if (activeOrders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Δεν υπάρχουν ενεργές παραγγελίες", color = TextSecondary, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(activeOrders) { order ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Τραπέζι ${order.tableId} (${order.zone})",
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "Σερβιτόρος: ${order.waiterName ?: "User"}",
                                                color = TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = "${String.format("%.2f", order.totalPrice)} €",
                                            color = AccentCyan,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 20.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color(0xFF1E293B), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Προϊόντα:",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    order.items.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${item.productName ?: "Προϊόν"} x${item.quantity}",
                                                color = TextPrimary,
                                                fontSize = 13.sp
                                            )
                                            if (!item.notes.isNullOrEmpty()) {
                                                Text(
                                                    text = "(${item.notes})",
                                                    color = AccentCyan,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(start = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (!order.notes.isNullOrEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Σημείωση: ${order.notes}",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { onEditOrder(order) },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Επεξεργασία", color = Color.White, fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                orderViewModel.selectTable(order.zone, order.tableId)
                                                onTableSelected()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Πληρωμή", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    orderViewModel: OrderViewModel,
    onBack: () -> Unit,
    onGoToCart: () -> Unit
) {
    val products by orderViewModel.products.collectAsState()
    val categories by orderViewModel.categories.collectAsState()
    
    var selectedCatId by remember { mutableStateOf<Int?>(null) }
    val cartItems = orderViewModel.cartItems
    val totalItems = cartItems.sumOf { it.quantity }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCatId == null) {
            selectedCatId = categories[0].id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Τραπέζι: ${orderViewModel.selectedTable.value} (${orderViewModel.selectedZone.value})",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (orderViewModel.editingOrderId.value != null) {
                        Text(
                            text = "Επεξεργασία",
                            color = AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark)
            )
        },
        floatingActionButton = {
            BadgedBox(
                badge = {
                    if (totalItems > 0) {
                        Badge(
                            containerColor = TableRed,
                            contentColor = Color.White
                        ) {
                            Text(totalItems.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = onGoToCart,
                    containerColor = AccentCyan,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = "Cart",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        containerColor = PrimaryDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name, color = TextPrimary, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentCyan,
                            containerColor = SecondaryDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (selectedCatId == cat.id) AccentCyan else Color(0xFF334155)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val filteredProducts = products.filter { it.categoryId == selectedCatId && it.available }
            
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Δεν υπάρχουν διαθέσιμα προϊόντα σε αυτή την κατηγορία.", color = TextSecondary)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts.size) { index ->
                        val prod = filteredProducts[index]
                        val qtyInCart = cartItems.find { it.product.id == prod.id }?.quantity ?: 0

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { orderViewModel.addToCart(prod) },
                            colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                width = if (qtyInCart > 0) 2.dp else 1.dp,
                                color = if (qtyInCart > 0) AccentCyan else Color(0xFF1E293B)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = prod.name,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${String.format("%.2f", prod.price)} €",
                                        color = AccentCyan,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    if (qtyInCart > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(AccentCyan, RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = qtyInCart.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    orderViewModel: OrderViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit,
    onSubmitSuccess: () -> Unit
) {
    val cartItems = orderViewModel.cartItems
    val context = LocalContext.current
    val isEditing = orderViewModel.editingOrderId.value != null

    var noteItemToEdit by remember { mutableStateOf<CartItem?>(null) }
    var itemNoteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Καλάθι: Τραπέζι ${orderViewModel.selectedTable.value}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark)
            )
        },
        containerColor = PrimaryDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Προϊόντα στο Καλάθι",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Το καλάθι είναι άδειο", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.product.name,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "${String.format("%.2f", item.product.price)} € ανά τμχ.",
                                            color = TextSecondary,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = { orderViewModel.decrementCartItem(item) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        ) {
                                            Text("—", color = TableRed, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = item.quantity.toString(),
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { orderViewModel.incrementCartItem(item) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Inc", tint = TableGreen, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            noteItemToEdit = item
                                            itemNoteText = item.notes
                                        }
                                        .padding(top = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Note",
                                        tint = if (item.notes.isEmpty()) TextSecondary else AccentCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (item.notes.isEmpty()) "Προσθήκη σημείωσης..." else item.notes,
                                        color = if (item.notes.isEmpty()) TextSecondary else AccentCyan,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = orderViewModel.cartNotes.value,
                onValueChange = { orderViewModel.cartNotes.value = it },
                label = { Text("Γενικές Σημειώσεις Παραγγελίας", color = TextSecondary) },
                singleLine = false,
                maxLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = AccentCyan,
                    unfocusedBorderColor = Color(0xFF1E293B)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            val cartTotal = cartItems.sumOf { it.product.price * it.quantity }
            
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color(0xFF1E293B), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Συνολικό Ποσό:", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${String.format("%.2f", cartTotal)} €",
                    color = AccentCyan,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        orderViewModel.submitCurrentOrder(
                            onSuccess = {
                                Toast.makeText(context, "Η παραγγελία υποβλήθηκε επιτυχώς!", Toast.LENGTH_SHORT).show()
                                onSubmitSuccess()
                            },
                            onError = {
                                Toast.makeText(context, "Σφάλμα: $it", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(12.dp),
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isEditing) "Ενημέρωση Παραγγελίας" else "Υποβολή Παραγγελίας",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                if (isEditing) {
                    Button(
                        onClick = onCheckout,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = "Checkout", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Πληρωμή / Ακύρωση",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (noteItemToEdit != null) {
        Dialog(onDismissRequest = { noteItemToEdit = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.5f)),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Σημείωση για: ${noteItemToEdit!!.product.name}",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = itemNoteText,
                        onValueChange = { itemNoteText = it },
                        placeholder = { Text("π.χ. χωρίς κρεμμύδι, παγάκια...", color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { noteItemToEdit = null }) {
                            Text("Ακύρωση", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                orderViewModel.updateItemNotes(noteItemToEdit!!, itemNoteText)
                                noteItemToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Αποθήκευση", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    orderViewModel: OrderViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val activeOrders by orderViewModel.activeOrders.collectAsState()
    val tableId = orderViewModel.selectedTable.value
    val zone = orderViewModel.selectedZone.value
    val context = LocalContext.current

    val currentOrder = activeOrders.find { it.tableId == tableId && it.zone == zone }
    var cancelDialogOpen by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Έκδοση Λογαριασμού: $tableId", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark)
            )
        },
        containerColor = PrimaryDark
    ) { paddingValues ->
        if (currentOrder == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Δεν βρέθηκε ενεργή παραγγελία για το τραπέζι.", color = TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .weight(1.5f)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "--- ΑΠΟΔΕΙΞΗ ΤΡΑΠΕΖΙΟΥ ---",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Τραπέζι: ${currentOrder.tableId} (${currentOrder.zone})",
                        color = Color.Black,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Σερβιτόρος: ${currentOrder.waiterName ?: "User"}",
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Ημερομηνία: ${currentOrder.createdAt.replace("T", " ").substring(0, 16)}",
                        color = Color.Black,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(color = Color.DarkGray, thickness = 1.dp)

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(orderViewModel.cartItems) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.product.name} x${item.quantity}",
                                    color = Color.Black,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${String.format("%.2f", item.product.price * item.quantity)} €",
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ΣΥΝΟΛΟ:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("${String.format("%.2f", currentOrder.totalPrice)} €", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Επιλέξτε Τρόπο Πληρωμής:", color = TextSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                orderViewModel.issueBillCurrentOrder(
                                    paymentMethod = "cash",
                                    onSuccess = {
                                        Toast.makeText(context, "Εισπράχθηκε Μετρητά & Εκτυπώθηκε!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    },
                                    onError = { Toast.makeText(context, "Σφάλμα: $it", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TableGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Cash")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Μετρητά")
                        }

                        Button(
                            onClick = {
                                orderViewModel.issueBillCurrentOrder(
                                    paymentMethod = "card",
                                    onSuccess = {
                                        Toast.makeText(context, "Εισπράχθηκε Κάρτα & Εκτυπώθηκε!", Toast.LENGTH_SHORT).show()
                                        onSuccess()
                                    },
                                    onError = { Toast.makeText(context, "Σφάλμα: $it", Toast.LENGTH_SHORT).show() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Card")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Κάρτα")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { cancelDialogOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TableRed),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ακύρωση Παραγγελίας")
                    }
                }
            }
        }
    }

    if (cancelDialogOpen) {
        Dialog(onDismissRequest = { cancelDialogOpen = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Λόγος Ακύρωσης", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        placeholder = { Text("π.χ. Λάθος καταχώρηση") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = TableRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { cancelDialogOpen = false }) {
                            Text("Πίσω", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                orderViewModel.cancelCurrentOrder(
                                    reason = cancelReason,
                                    onSuccess = {
                                        Toast.makeText(context, "Η παραγγελία ακυρώθηκε.", Toast.LENGTH_SHORT).show()
                                        cancelDialogOpen = false
                                        onSuccess()
                                    },
                                    onError = {
                                        Toast.makeText(context, "Σφάλμα: $it", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TableRed)
                        ) {
                            Text("Ακύρωση", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

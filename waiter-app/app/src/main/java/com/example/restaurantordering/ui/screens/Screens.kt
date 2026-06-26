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
import com.example.restaurantordering.data.model.CartItem
import com.example.restaurantordering.data.model.Product
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
    onTableSelected: () -> Unit
) {
    val activeOrders by orderViewModel.activeOrders.collectAsState()
    val zones = listOf("Σάλα", "Μπαλκόνι", "1ος Όροφος", "Πισίνα")
    var selectedZoneIdx by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RestoWaiter - Επιλογή Τραπεζιού", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark),
                actions = {
                    IconButton(onClick = { 
                        authViewModel.logout()
                        onLogout()
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.Red)
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
            // Horizontal scrollable Tabs for Zones
            TabRow(
                selectedTabIndex = selectedZoneIdx,
                containerColor = SecondaryDark,
                contentColor = AccentCyan,
                modifier = Modifier.fillMaxWidth()
            ) {
                zones.forEachIndexed { idx, zone ->
                    Tab(
                        selected = selectedZoneIdx == idx,
                        onClick = { selectedZoneIdx = idx },
                        text = { Text(zone, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of tables for selected zone (1 to 15)
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
                    val hasActiveOrder = activeOrders.any { it.tableId == tableId && it.zone == currentZone }
                    val tableColor = if (hasActiveOrder) TableRed else TableGreen

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SecondaryDark)
                            .border(2.dp, tableColor, RoundedCornerShape(16.dp))
                            .clickable {
                                orderViewModel.selectTable(currentZone, tableId)
                                onTableSelected()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Τραπέζι",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = tableId,
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (hasActiveOrder) "Κατειλημμένο" else "Ελεύθερο",
                                color = tableColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
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
    onCheckout: () -> Unit
) {
    val products by orderViewModel.products.collectAsState()
    val categories by orderViewModel.categories.collectAsState()
    
    var selectedCatId by remember { mutableStateOf<Int?>(null) }
    val cartItems = orderViewModel.cartItems
    val context = LocalContext.current

    // Set first category selected by default when loaded
    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedCatId == null) {
            selectedCatId = categories[0].id
        }
    }

    // Modal dialog state for adding item note
    var noteItemToEdit by remember { mutableStateOf<CartItem?>(null) }
    var itemNoteText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Παραγγελία: ${orderViewModel.selectedTable.value} (${orderViewModel.selectedZone.value})",
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
                        Button(
                            onClick = onCheckout,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentIndigo),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Πληρωμή / Ακύρωση", color = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryDark)
            )
        },
        containerColor = PrimaryDark
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Left Side: Menu Selection
            Column(
                modifier = Modifier
                    .weight(1.3f)
                    .fillMaxHeight()
                    .padding(12.dp)
            ) {
                // Category Tabs (Horizontal Scroll)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCatId == cat.id,
                            onClick = { selectedCatId = cat.id },
                            label = { Text(cat.name, color = TextPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentCyan,
                                containerColor = SecondaryDark
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Products list
                val filteredProducts = products.filter { it.categoryId == selectedCatId && it.available }
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts.size) { index ->
                        val prod = filteredProducts[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { orderViewModel.addToCart(prod) },
                            colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(prod.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${prod.price} €",
                                    color = AccentCyan,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Right Side: Cart Summary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SecondaryDark)
                    .padding(12.dp)
            ) {
                Text(
                    text = "Καλάθι Παραγγελίας",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(cartItems) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryDark),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.product.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            text = "${item.product.price * item.quantity} €",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { orderViewModel.decrementCartItem(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("—", color = Color.Red, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(item.quantity.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        IconButton(
                                            onClick = { orderViewModel.incrementCartItem(item) },
                                            modifier = Modifier.size(24.dp)
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
                                        .padding(top = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Note",
                                        tint = if (item.notes.isEmpty()) TextSecondary else AccentCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (item.notes.isEmpty()) "Προσθήκη σημείωσης..." else item.notes,
                                        color = if (item.notes.isEmpty()) TextSecondary else AccentCyan,
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // General Notes Field
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = orderViewModel.cartNotes.value,
                    onValueChange = { orderViewModel.cartNotes.value = it },
                    label = { Text("Γενικές Σημειώσεις", color = TextSecondary, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentCyan,
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Total & Submit
                val cartTotal = cartItems.sumOf { it.product.price * it.quantity }
                
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Σύνολο:", color = TextSecondary, fontWeight = FontWeight.Bold)
                    Text("${cartTotal} €", color = AccentCyan, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        orderViewModel.submitCurrentOrder(
                            onSuccess = {
                                Toast.makeText(context, "Η παραγγελία εστάλη!", Toast.LENGTH_SHORT).show()
                                onBack()
                            },
                            onError = {
                                Toast.makeText(context, "Σφάλμα: $it", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCyan),
                    shape = RoundedCornerShape(10.dp),
                    enabled = cartItems.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (orderViewModel.editingOrderId.value != null) "Ενημέρωση Παραγγελίας" else "Αποστολή Παραγγελίας",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Dialogue for editing item-specific notes
    if (noteItemToEdit != null) {
        Dialog(onDismissRequest = { noteItemToEdit = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SecondaryDark),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Σημείωση για: ${noteItemToEdit!!.product.name}", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = itemNoteText,
                        onValueChange = { itemNoteText = it },
                        placeholder = { Text("π.χ. χωρίς κρεμμύδι, παγάκια...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { noteItemToEdit = null }) {
                            Text("Ακύρωση", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                orderViewModel.updateItemNotes(noteItemToEdit!!, itemNoteText)
                                noteItemToEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                        ) {
                            Text("Αποθήκευση", color = Color.White)
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
    val products by orderViewModel.products.collectAsState()
    val tableId = orderViewModel.selectedTable.value
    val zone = orderViewModel.selectedZone.value
    val context = LocalContext.current

    val currentOrder = activeOrders.find { it.tableId == tableId && it.zone == zone }

    // Dialog state for cancellation reason
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
            // Simulated Thermal Bill Receipt Card
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

                    // Render items
                    // In real app, items are part of order details loaded into viewmodel.
                    // For the UI display, we retrieve quantities from orderViewModel.cartItems
                    // or simulated items.
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
                                    text = "${item.product.price * item.quantity} €",
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
                        Text("${currentOrder.totalPrice} €", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }

            // Checkout Buttons Panel
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

    // Cancellation Reason Dialog
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

package com.example.taskgo.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSelectorScreen(
    initialLocation: LatLng = LatLng(3.1718, 101.7145), // Default UTMKL
    onLocationSelected: (LatLng, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Permission handling
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        locationPermissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 17f)
    }

    var currentAddress by remember { mutableStateOf("Fetching address...") }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    // Constants for Malaysia bounds
    val malaysiaBounds = com.google.android.gms.maps.model.LatLngBounds(
        LatLng(0.8, 99.6), // Southwest
        LatLng(7.4, 119.3)  // Northeast
    )
    
    // Skill: Reverse Geocoding (LatLng -> Address)
    fun updateAddress(latLng: LatLng) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                withContext(Dispatchers.Main) {
                    currentAddress = if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0) ?: "Unknown Location"
                    } else {
                        "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { currentAddress = "Error fetching address" }
            }
        }
    }

    // Update address when map stops moving
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            updateAddress(cameraPositionState.position.target)
        }
    }

    // Live Search Effect
    LaunchedEffect(searchQuery) {
        if (searchQuery.length > 2) {
            isSearching = true
            scope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    // Search specifically in Malaysia
                    val results = geocoder.getFromLocationName(
                        "$searchQuery, Malaysia", 
                        5,
                        malaysiaBounds.southwest.latitude,
                        malaysiaBounds.southwest.longitude,
                        malaysiaBounds.northeast.latitude,
                        malaysiaBounds.northeast.longitude
                    )
                    withContext(Dispatchers.Main) {
                        searchResults = results ?: emptyList()
                        isSearching = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { isSearching = false }
                }
            }
        } else {
            searchResults = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth().padding(end = 16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search in Malaysia...", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Clear, null) }
                                } else {
                                    Icon(Icons.Default.Search, null)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onPOIClick = { poi ->
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(poi.latLng, 18f))
                        currentAddress = poi.name
                    }
                },
                properties = MapProperties(
                    isMyLocationEnabled = locationPermissionGranted,
                    latLngBoundsForCameraTarget = malaysiaBounds
                ),
                uiSettings = MapUiSettings(myLocationButtonEnabled = locationPermissionGranted)
            )

            // Search Results Overlay
            if (searchResults.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp)
                        .heightIn(max = 300.dp)
                        .align(Alignment.TopCenter),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    LazyColumn {
                        items(searchResults) { addressItem ->
                            ListItem(
                                headlineContent = { Text(addressItem.getAddressLine(0) ?: "Unknown", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = { Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
                                modifier = Modifier.clickable {
                                    val target = LatLng(addressItem.latitude, addressItem.longitude)
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, 17f))
                                    }
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            // THE CENTRAL PIN (Fixed in middle)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).offset(y = (-20).dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Box(modifier = Modifier.size(8.dp).background(Color.Black.copy(0.3f), CircleShape))
                }
            }
            
            // Bottom Address Card & Confirmation
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PinDrop, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Selected Address", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = currentAddress,
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Button(
                        onClick = { onLocationSelected(cameraPositionState.position.target, currentAddress) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Location")
                    }
                }
            }
        }
    }
}

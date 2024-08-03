@file:OptIn(ExperimentalMaterialApi::class, ExperimentalMaterialApi::class,
    ExperimentalMaterialApi::class
)

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import dev.jordond.compass.geolocation.Geolocator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import dev.jordond.compass.Coordinates
import dev.jordond.compass.Place
import dev.jordond.compass.Priority
import dev.jordond.compass.autocomplete.Autocomplete
import dev.jordond.compass.autocomplete.mobile
import dev.jordond.compass.geocoder.MobileGeocoder
import dev.jordond.compass.geocoder.placeOrNull
import dev.jordond.compass.geolocation.GeolocatorResult
import dev.jordond.compass.geolocation.LocationRequest
import dev.jordond.compass.geolocation.TrackingStatus
import dev.jordond.compass.geolocation.mobile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        val geoLocation = remember {
            Geolocator.mobile()
        }
        LaunchedEffect(Unit) {
            when (val result = geoLocation.current()) {

                is GeolocatorResult.Error -> when (result) {
                    is GeolocatorResult.NotSupported -> println("LOCATION ERROR: ${result.message}")
                    is GeolocatorResult.NotFound -> println("LOCATION ERROR: ${result.message}")
                    is GeolocatorResult.PermissionError -> println("LOCATION ERROR: ${result.message}")
                    is GeolocatorResult.GeolocationFailed -> println("LOCATION ERROR: ${result.message}")
                    else -> println("LOCATION ERROR: ${result.message}")
                }

                is GeolocatorResult.Success -> {
                    println("CURRENT LOCATION: ${result.data.coordinates}")
                    println(
                        "CURRENT LOCATION NAME: ${
                            MobileGeocoder()
                                .placeOrNull(result.data.coordinates)?.locality
                        }"
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LocationTracking()
            Spacer(
                modifier = Modifier
                    .height(16.dp)
            )
            PlacesAutocomplete()
        }
    }
}

@Composable
fun LocationTracking() {
    val scope = rememberCoroutineScope()
    val geoLocator = remember {
        Geolocator.mobile()
    }
    val trackingStatus by geoLocator.trackingStatus
        .collectAsState(initial = null)
    var currentLocation: Coordinates? by remember {
        mutableStateOf(null)
    }
    var currentCity: String? by remember {
        mutableStateOf(null)
    }

    LaunchedEffect(key1 = Unit) {
        geoLocator.locationUpdates.collectLatest {
            currentLocation = it.coordinates
            currentCity = MobileGeocoder().placeOrNull(it.coordinates)?.locality
        }
    }

    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = currentCity ?: "Waiting...",
    )
    Spacer(
        modifier = Modifier
            .height(16.dp)
    )
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = "LAT: ${currentLocation?.latitude}\nLNG: ${currentLocation?.longitude}",
    )
    Spacer(
        modifier = Modifier
            .height(16.dp)
    )
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Button(
            enabled = trackingStatus == TrackingStatus.Idle,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    geoLocator.startTracking(
                        LocationRequest(
                            Priority.HighAccuracy
                        )
                    )
                }
            }
        ) {
            Text(
                text = "Start"
            )
        }
        Spacer(
            modifier = Modifier
                .width(16.dp)
        )
        Button(
            enabled = trackingStatus != TrackingStatus.Idle,
            onClick = {
                scope.launch(Dispatchers.IO) {
                    geoLocator.stopTracking()
                    currentLocation = null
                    currentCity = null
                }
            }
        ) {
            Text(
                text = "Stop"
            )
        }
    }
}

@Composable
fun PlacesAutocomplete() {
    val scope = rememberCoroutineScope()
    val autoComplete = remember {
        Autocomplete.mobile()
    }
    var exppanded by remember {
        mutableStateOf(false)
    }
    var searchQuery by remember { mutableStateOf("") }
    val places = remember {
        mutableStateListOf<Place?>()
    }
    var selectedPlace: Place? by remember {
        mutableStateOf(null)
    }
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        text = if (selectedPlace != null)
            "${selectedPlace?.locality} (${selectedPlace?.country})\n" +
                    "LAT: ${selectedPlace?.coordinates?.latitude}\n" +
                    "LNG: ${selectedPlace?.coordinates?.longitude}"
        else "No place selected",
    )
    Spacer(
        modifier = Modifier
            .height(16.dp)
    )
    ExposedDropdownMenuBox(
        expanded = exppanded,
        onExpandedChange = {

        }
    ) {
        TextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    scope.launch {
                        autoComplete.search(searchQuery).getOrNull().let {
                            println("mysearchQuery")
                            places.clear()
                            places.addAll(it?.toList() ?: emptyList())
                            if (places.isNotEmpty())
                                println(places.toList())
                            else
                                println("List unknown")
                        }
                    }
                    exppanded = !exppanded
                }
            )
        )
        ExposedDropdownMenu(
            expanded = exppanded,
            onDismissRequest = {
                exppanded = false
            }
        ) {
            if (places.isNotEmpty()) {
                places.forEach { selectedOption ->
                    DropdownMenuItem(
                        onClick = {
                            selectedPlace = selectedOption
                            exppanded = false
                        }
                    ) {
                        Text(
                            text = selectedOption?.locality ?: "Unknown place"
                        )
                    }
                }
            }
        }
    }
}
package com.example.busnego_nathalia_examen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.busnego_nathalia_examen.db.AppDataBase
import com.example.busnego_nathalia_examen.db.Travel
import com.example.busnego_nathalia_examen.ws.Fabrica
import com.example.busnego_nathalia_examen.ws.Indicator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.time.LocalDateTime


class MissingPermissionsException(message:String):Exception(message)

enum class Screen{
    FORM,
    CAMERA,
    MAP,
    MAIN,
    INFO
}

class AppVm: ViewModel(){
    val latitud = mutableStateOf(0.0)
    val longitud = mutableStateOf(0.0)
    val currentScreen = mutableStateOf(Screen.FORM)
    var selectedTravel = mutableStateOf<Travel?>(null)
    val picture = mutableStateOf<Uri?>(null)
    var PermissionCameraOk:() -> Unit ={
    }
    var LocationPermission:() -> Unit = {}
}

class MainActivity : ComponentActivity() {
    val appVm: AppVm by viewModels()
    lateinit var cameraController: LifecycleCameraController

    val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (it[android.Manifest.permission.CAMERA] ?: false) {
            appVm.PermissionCameraOk()
        }
    }

    val permissionLauncherM = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (
            (it[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false) or
            (it[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false)
        ) {
            appVm.LocationPermission()
        } else {
            Log.v("permissionLauncher callback", "Access Denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        setContent {
            AppUi(permissionLauncher, cameraController, appVm)
        }
    }

    @Composable
    fun AppUi(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        cameraController: LifecycleCameraController, appVm: AppVm
    ) {
        val appVm: AppVm = viewModel()

        when (appVm.currentScreen.value) {
            Screen.FORM -> {
                FormUi(appVm)
            }

            Screen.CAMERA -> {
                CameraUi(permissionLauncher, cameraController)
            }

            Screen.MAP -> {
                MapUi(appVm, permissionLauncherM)
            }

            Screen.MAIN -> {
                MainScreen(appVm)
            }

            Screen.INFO -> {
                InfoUi(appVm, permissionLauncher)
            }

        }
    }


    @Composable
    fun MainScreen(appVm: AppVm) {
        val context = LocalContext.current
        val daoTravel = AppDataBase.getInstance(context).daoTravel()
        val scope = rememberCoroutineScope()
        val items = remember { mutableStateListOf<Travel>() }
        val (dolar, setDolar) = remember { mutableStateOf(emptyArray<Indicator>()) }
        var valorMoneda = 1.0

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val service = Fabrica.getDailyIndicator()
                setDolar(service.getMonthlyValues().serie)
            }
        }
        if (dolar.isNotEmpty()) {
            valorMoneda = dolar[0].valor.toDouble()
        }

        LaunchedEffect(Unit) {
            scope.launch {
                val viajes = withContext(Dispatchers.IO) {
                    daoTravel.select()
                }
                items.clear()
                items.addAll(viajes)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .align(CenterHorizontally)
            ) {
                Text(
                    text = "Travel Organizer",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.padding(5.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
            ) {
                itemsIndexed(items) { index, travel ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .background(Color.White)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = travel.imagen),
                                    contentDescription = "Imagen del Destino",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Nombre del Destino: ",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("${travel.nombre}")
                                    Text(
                                        "Costo por noche $: ", fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${travel.alojamiento}" +
                                                "- ${Math.round(travel.alojamiento / valorMoneda)}" + "USD"
                                    )
                                    Text(
                                        "Costo traslado: ", fontWeight = FontWeight.Bold
                                    )
                                    Text(" ${travel.traslado}")
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        appVm.latitud.value = travel.latitud.toDouble()
                                        appVm.longitud.value = travel.latitud.toDouble()
                                        appVm.currentScreen.value = Screen.MAP
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = "Ubicación"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        appVm.selectedTravel.value = travel
                                        appVm.currentScreen.value = Screen.INFO
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Abrir Viaje"
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            daoTravel.delete(travel)
                                            items.remove(travel)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar",
                                        tint = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = { appVm.currentScreen.value = Screen.FORM },
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    Text("Add Another Travel")
                }
            }
        }
    }

    @Composable
    fun CameraUi(
        permissionLauncher: ActivityResultLauncher<Array<String>>,
        cameraController: LifecycleCameraController
    ) {
        val context = LocalContext.current
        val appVm: AppVm = viewModel()

        permissionLauncher.launch(arrayOf(android.Manifest.permission.CAMERA))

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    controller = cameraController
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    takePicture(
                        cameraController,
                        createPrivateImageFile(context),
                        context
                    ) {
                        appVm.picture.value = it
                        appVm.currentScreen.value = Screen.INFO
                    }
                }
            ) {
                Text("Take Picture")
            }
        }
    }

    fun createNameByDate(): String = LocalDateTime
        .now().toString().replace(Regex("[T:.-]"), "").substring(0, 14)

    fun createPrivateImageFile(context: Context): File = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DCIM),
        "${createNameByDate()}.jpg"
    )

    fun takePicture(
        cameraController: LifecycleCameraController,
        file: File,
        context: Context,
        onPictureTaken: (uri: Uri) -> Unit
    ) {
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        cameraController.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    outputFileResults.savedUri?.let {
                        onPictureTaken(it)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(
                        "takePicture::OnImageSavedCallback::onError",
                        exception.message ?: "Error"
                    )
                }

            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun FormUi(appVm: AppVm) {
        val context = LocalContext.current
        var lugar by remember { mutableStateOf("") }
        var imagenRef by remember { mutableStateOf("") }
        var latLong by remember { mutableStateOf("") }
        var orden by remember { mutableStateOf("") }
        var alojamiento by remember { mutableStateOf("") }
        var traslado by remember { mutableStateOf("") }
        var comentarios by remember { mutableStateOf("") }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Text(
                    text = "Place Name",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = lugar,
                    onValueChange = { lugar = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Place Name") }
                )
            }

            item {
                Text(
                    text = "Imagen Ref",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = imagenRef,
                    onValueChange = { imagenRef = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Imagen Ref") }
                )
            }

            item {
                Text(
                    text = "Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = latLong,
                    onValueChange = { latLong = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Location") }
                )
            }

            item {
                Text(
                    text = "Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = orden,
                    onValueChange = { orden = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Order") }
                )
            }

            item {
                Text(
                    text = "Housing Cost",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = alojamiento,
                    onValueChange = { alojamiento = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Housing Cost") }
                )
            }

            item {
                Text(
                    text = "Transfer Cost",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = traslado,
                    onValueChange = { traslado = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Transfer Cost") }
                )
            }

            item {
                Text(
                    text = "Commentary",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    textAlign = TextAlign.Center
                )
                TextField(
                    value = comentarios,
                    onValueChange = { comentarios = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    label = { Text("Commentary") }
                )
            }
            item {
                Button(
                    onClick = {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val daoTravel = AppDataBase.getInstance(context).daoTravel()
                            val travel = Travel(
                                id = 0,
                                nombre = lugar,
                                imagen = imagenRef,
                                latitud = 0,
                                longitud = 0,
                                orden = orden.toIntOrNull() ?: 0,
                                alojamiento = alojamiento.toIntOrNull() ?: 0,
                                traslado = traslado.toIntOrNull() ?: 0,
                                comentario = comentarios
                            )
                            daoTravel.insert(travel)
                            appVm.currentScreen.value = Screen.MAIN
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Guardar")
                }
            }
        }
    }


    @Composable
    fun MapUi(appVm: AppVm, permissionLauncherM: ActivityResultLauncher<Array<String>>) {
        val context = LocalContext.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    getLocation(context) {
                        appVm.latitud.value = it.latitude
                        appVm.longitud.value = it.longitude
                    }
                }
                appVm.LocationPermission = {
                    getLocation(context) {
                        appVm.latitud.value = it.latitude
                        appVm.longitud.value = it.longitude
                    }
                }
                permissionLauncherM.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            Spacer(Modifier.height(100.dp))
            AndroidView(
                factory = {
                    MapView(it).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        Configuration.getInstance().userAgentValue = context.packageName
                        controller.setZoom(15.0)
                    }
                }, update = {
                    it.overlays.removeIf { true }
                    it.invalidate()

                    val geoPoint = GeoPoint(appVm.latitud.value, appVm.longitud.value)
                    it.controller.animateTo(geoPoint)

                    val mark = Marker(it)
                    mark.position = geoPoint
                    mark.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    it.overlays.add(mark)
                }
            )
        }

        Button(
            onClick = {
                appVm.currentScreen.value = Screen.MAIN
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Back")
        }
    }

    fun getLocation(context: Context, onSuccess: (location: Location) -> Unit) {

        try {
            val service = LocationServices.getFusedLocationProviderClient(context)
            val work = service.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                null
            )
            work.addOnSuccessListener {
                onSuccess(it)
            }
        } catch (se: SecurityException) {
            throw MissingPermissionsException("Permission denied")
        }
    }

    @Composable
    fun InfoUi(appVm: AppVm, permissionLauncher: ActivityResultLauncher<Array<String>>) {
        val context = LocalContext.current
        var capturedImageUri by remember { mutableStateOf<Uri?>(null) }
        var bitmap: ImageBitmap? by remember { mutableStateOf(null) }

        LaunchedEffect(capturedImageUri) {
            if (capturedImageUri != null) {
                bitmap = uriABitmap(capturedImageUri!!, context)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            appVm.selectedTravel.value?.let {
                Text(
                    text = it.nombre,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            Image(
                painter = rememberAsyncImagePainter(model = appVm.selectedTravel.value?.imagen),
                contentDescription = "Imagen del Destino",
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .padding(bottom = 20.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Housing Cost:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "$ ${appVm.selectedTravel.value?.alojamiento}",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                Column {
                    Text(
                        text = "Transfer Cost:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "$ ${appVm.selectedTravel.value?.traslado}",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
            Column {
                Text(
                    text = "Commentary:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "${appVm.selectedTravel.value?.comentario}",
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            IconButton(
                onClick = {
                    appVm.currentScreen.value = Screen.CAMERA
                    takePicture(
                        cameraController,
                        createPrivateImageFile(context),
                        context
                    ) { uri ->
                        appVm.picture.value = uri
                        capturedImageUri = uri
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = "Abrir cámara",
                    tint = Color.Black
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            )
            {Text(text ="Add A Selfie",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 2.dp))}

            appVm.picture.value?.let { uri ->
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = "Picture taken from camera",
                    modifier = Modifier.size(200.dp)
                )

            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { appVm.currentScreen.value = Screen.MAIN },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(CenterHorizontally)
            ) {
                Text("Back to main")
            }
        }
    }


    suspend fun uriABitmap(uri: Uri, context: Context): ImageBitmap {
        return withContext(Dispatchers.IO) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            bitmap.asImageBitmap()
        }
    }
}



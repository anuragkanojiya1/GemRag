import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract.Colors
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminiapicompetition.BakingViewModel
import com.example.geminiapicompetition.R
import com.example.geminiapicompetition.UiState
import java.io.InputStream

@Composable
fun BakingScreen(
  bakingViewModel: BakingViewModel = viewModel()
) {
  val context = LocalContext.current
  var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  val placeholderResult = stringResource(R.string.results_placeholder)
  var prompt by rememberSaveable { mutableStateOf("") }
  var result by rememberSaveable { mutableStateOf(placeholderResult) }
  val uiState by bakingViewModel.uiState.collectAsState()

  val gradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF2F5F7),Color(0xFF7FE2F0))
  )

  // Define the image picker launcher
  val getImageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      selectedImageUri = it
      val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
      bitmap = BitmapFactory.decodeStream(inputStream)
    }
  }

  // Define the permission launcher
  val requestPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted: Boolean ->
    if (isGranted) {
      // Permission is granted, proceed with accessing gallery
      openGallery(context, getImageLauncher)
    } else {
      // Permission is denied, show an error message or dialog
      Toast.makeText(context, "Permission denied", Toast.LENGTH_SHORT).show()
    }
  }

  // Check for permissions and request if necessary
  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      when {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PermissionChecker.PERMISSION_GRANTED -> {
          // Permission is already granted
        }
        else -> {
          // Request permission
          requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      }
    } else {
      // Permissions are automatically granted on Android versions below M
      openGallery(context, getImageLauncher)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize()
      .background(brush = gradient)
  ) {
    Text(
      text = "GemRag",
      style = MaterialTheme.typography.titleLarge,
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally),
      fontSize = 32.sp,
      fontStyle = FontStyle.Normal
    )

    Button(
      onClick = {
        if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
          ) == PermissionChecker.PERMISSION_GRANTED) {
          // Permission is granted, open gallery
          openGallery(context, getImageLauncher)
        } else {
          // Request permission
          requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
      },
      modifier = Modifier
        .padding(16.dp)
        .align(Alignment.CenterHorizontally)
    ) {
      Text(text = "Select an Image")
    }

    selectedImageUri?.let {
      bitmap?.asImageBitmap()?.let { it1 ->
        Image(
          bitmap = it1,
          contentDescription = null,
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        )
      }
    }

    Row(
      modifier = Modifier
        .padding(all = 16.dp)
        .align(Alignment.CenterHorizontally)
    ) {
      OutlinedTextField(
        value = prompt,
        label = { Text(stringResource(R.string.label_prompt)) },
        onValueChange = { prompt = it },
        modifier = Modifier
          .weight(0.8f)
          .padding(end = 16.dp)
          .align(Alignment.CenterVertically)
      )

      ElevatedButton(
        onClick = {
          bitmap?.let { bmp ->
            bakingViewModel.sendPrompt(bmp, prompt)
          }
        },
        enabled = prompt.isNotEmpty() && bitmap != null,
        modifier = Modifier.align(Alignment.CenterVertically).width(76.dp),
        colors = ButtonColors(
          disabledContainerColor = Color.Red,
          containerColor = Color.Blue,
          contentColor = Color.White,
          disabledContentColor = Color.White
        ),
        border = BorderStroke(2.dp, Color.White)
      ) {
        Text(text = stringResource(R.string.action_go))
      }
    }

    if (uiState is UiState.Loading) {
      CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
    } else {
      var textColor = MaterialTheme.colorScheme.onSurface
      if (uiState is UiState.Error) {
        textColor = MaterialTheme.colorScheme.error
        result = (uiState as UiState.Error).errorMessage
      } else if (uiState is UiState.Success) {
        textColor = MaterialTheme.colorScheme.onSurface
        result = (uiState as UiState.Success).outputText
      }
      val scrollState = rememberScrollState()
      Text(
        text = result,
        textAlign = TextAlign.Center,
        color = textColor,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(16.dp)
          .fillMaxSize()
          .verticalScroll(scrollState)
      )
    }
  }
}

// Helper function to open the gallery
private fun openGallery(context: Context, getImageLauncher: ActivityResultLauncher<String>) {
  getImageLauncher.launch("image/*")
}















//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.lifecycle.viewmodel.compose.viewModel
//
//val images = arrayOf(
//  // Image generated using Gemini from the prompt "cupcake image"
//  R.drawable.baked_goods_1,
//  // Image generated using Gemini from the prompt "cookies images"
//  R.drawable.baked_goods_2,
//  // Image generated using Gemini from the prompt "cake images"
//  R.drawable.baked_goods_3,
//)
//val imageDescriptions = arrayOf(
//  R.string.image1_description,
//  R.string.image2_description,
//  R.string.image3_description,
//)
//
//@Composable
//fun BakingScreen(
//  bakingViewModel: BakingViewModel = viewModel()
//) {
//  val selectedImage = remember { mutableIntStateOf(0) }
//  val placeholderPrompt = stringResource(R.string.prompt_placeholder)
//  val placeholderResult = stringResource(R.string.results_placeholder)
//  var prompt by rememberSaveable { mutableStateOf(placeholderPrompt) }
//  var result by rememberSaveable { mutableStateOf(placeholderResult) }
//  val uiState by bakingViewModel.uiState.collectAsState()
//  val context = LocalContext.current
//
//  Column(
//    modifier = Modifier.fillMaxSize()
//  ) {
//    Text(
//      text = stringResource(R.string.baking_title),
//      style = MaterialTheme.typography.titleLarge,
//      modifier = Modifier.padding(16.dp)
//    )
//
//    LazyRow(
//      modifier = Modifier.fillMaxWidth()
//    ) {
//      itemsIndexed(images) { index, image ->
//        var imageModifier = Modifier
//          .padding(start = 8.dp, end = 8.dp)
//          .requiredSize(200.dp)
//          .clickable {
//            selectedImage.intValue = index
//          }
//        if (index == selectedImage.intValue) {
//          imageModifier =
//            imageModifier.border(BorderStroke(4.dp, MaterialTheme.colorScheme.primary))
//        }
//        Image(
//          painter = painterResource(image),
//          contentDescription = stringResource(imageDescriptions[index]),
//          modifier = imageModifier
//        )
//      }
//    }
//
//    Row(
//      modifier = Modifier.padding(all = 16.dp)
//    ) {
//      TextField(
//        value = prompt,
//        label = { Text(stringResource(R.string.label_prompt)) },
//        onValueChange = { prompt = it },
//        modifier = Modifier
//          .weight(0.8f)
//          .padding(end = 16.dp)
//          .align(Alignment.CenterVertically)
//      )
//
//      Button(
//        onClick = {
//          val bitmap = BitmapFactory.decodeResource(
//            context.resources,
//            images[selectedImage.intValue]
//          )
//          bakingViewModel.sendPrompt(bitmap, prompt)
//        },
//        enabled = prompt.isNotEmpty(),
//        modifier = Modifier
//          .align(Alignment.CenterVertically)
//      ) {
//        Text(text = stringResource(R.string.action_go))
//      }
//    }
//
//    if (uiState is UiState.Loading) {
//      CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
//    } else {
//      var textColor = MaterialTheme.colorScheme.onSurface
//      if (uiState is UiState.Error) {
//        textColor = MaterialTheme.colorScheme.error
//        result = (uiState as UiState.Error).errorMessage
//      } else if (uiState is UiState.Success) {
//        textColor = MaterialTheme.colorScheme.onSurface
//        result = (uiState as UiState.Success).outputText
//      }
//      val scrollState = rememberScrollState()
//      Text(
//        text = result,
//        textAlign = TextAlign.Start,
//        color = textColor,
//        modifier = Modifier
//          .align(Alignment.CenterHorizontally)
//          .padding(16.dp)
//          .fillMaxSize()
//          .verticalScroll(scrollState)
//      )
//    }
//  }
//}
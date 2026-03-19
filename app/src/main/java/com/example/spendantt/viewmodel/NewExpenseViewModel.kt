package com.example.spendantt.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.spendantt.data.local.AppDatabase
import com.example.spendantt.data.local.entity.ExpenseEntity
import com.example.spendantt.data.local.entity.ExpenseSource
import com.example.spendantt.data.local.entity.LabelEntity
import com.example.spendantt.data.ocr.OcrProcessor
import com.example.spendantt.data.repository.ExpenseRepository
import com.example.spendantt.data.repository.LabelRepository
import com.example.spendantt.data.service.AutoCategorizationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

data class NewExpenseUiState(
    val name: String = "",
    val amount: String = "",
    val date: String = "",
    val time: String = "",
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val receiptImageUri: Uri? = null,
    val receiptStorageUrl: String? = null,
    val source: ExpenseSource = ExpenseSource.MANUAL,
    val isUploadingImage: Boolean = false,
    val isProcessingOcr: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val selectedLabelIds: Set<Int> = emptySet(),
    val selectedLabels: List<LabelEntity> = emptyList(),
    val allLabels: List<LabelEntity> = emptyList(),
    val labelsGroupedByCategory: Map<String, List<LabelEntity>> = emptyMap(),
    val showLabelPrompt: Boolean = false,
    val pendingExpenseId: Int? = null
)

class NewExpenseViewModel(
    private val context: Context,
    private val userId: Int
) : ViewModel() {

    private val db = AppDatabase.getInstance(context)
    private val repository = ExpenseRepository(db.expenseDao(), db.labelDao())
    private val labelRepository = LabelRepository(db.labelDao())
    private val ocrProcessor = OcrProcessor(context)
    private val autoCategorizationService = AutoCategorizationService(context, userId)

    private var firebaseUid: String? = null

    private val cloudinaryCloudName = "dpvrhtjka"
    private val cloudinaryUploadPreset = "SpendAnt"
    private val cloudinaryUploadUrl =
        "https://api.cloudinary.com/v1_1/$cloudinaryCloudName/image/upload"

    private val _uiState = MutableStateFlow(NewExpenseUiState())
    val uiState: StateFlow<NewExpenseUiState> = _uiState

    init {
        val now = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("hh:mma", Locale.getDefault())
        _uiState.value = _uiState.value.copy(
            date = dateFormat.format(now.time),
            time = timeFormat.format(now.time)
        )
        loadLabels()
        loadFirebaseUid()
    }

    private fun loadFirebaseUid() {
        viewModelScope.launch {
            firebaseUid = db.userDao().getUserById(userId)?.firebaseUid
        }
    }

    private fun loadLabels() {
        viewModelScope.launch {
            labelRepository.getLabelsByUser(userId).collect { labels ->
                if (labels.isEmpty()) {
                    labelRepository.insertDefaultLabels(userId)
                } else {
                    val grouped = labels.groupBy { it.category ?: "Other" }
                    _uiState.value = _uiState.value.copy(
                        allLabels = labels,
                        labelsGroupedByCategory = grouped
                    )
                }
            }
        }
    }

    fun toggleLabel(label: LabelEntity) {
        val currentIds = _uiState.value.selectedLabelIds.toMutableSet()
        if (currentIds.contains(label.id)) currentIds.remove(label.id)
        else currentIds.add(label.id)
        val selectedLabels = _uiState.value.allLabels.filter { currentIds.contains(it.id) }
        _uiState.value = _uiState.value.copy(
            selectedLabelIds = currentIds,
            selectedLabels = selectedLabels
        )
    }

    fun clearSelectedLabels() {
        _uiState.value = _uiState.value.copy(
            selectedLabelIds = emptySet(),
            selectedLabels = emptyList()
        )
    }

    fun onNameChange(value: String) { _uiState.value = _uiState.value.copy(name = value) }
    fun onAmountChange(value: String) { _uiState.value = _uiState.value.copy(amount = value) }

    fun onReceiptSelected(uri: Uri) {
        _uiState.value = _uiState.value.copy(receiptImageUri = uri, source = ExpenseSource.OCR)
        uploadReceiptToCloudinary(uri)
        processOcr(uri)
    }

    private fun processOcr(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingOcr = true)
            try {
                val result = ocrProcessor.processReceipt(uri)
                autoFillFromReceipt(
                    name = result.name, amount = result.amount,
                    date = result.date, time = result.time, locationName = result.locationName
                )
            } catch (e: Exception) {
            } finally {
                _uiState.value = _uiState.value.copy(isProcessingOcr = false)
            }
        }
    }

    private fun uploadReceiptToCloudinary(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingImage = true)
            try {
                val url = withContext(Dispatchers.IO) { uploadToCloudinary(uri) }
                _uiState.value = _uiState.value.copy(receiptStorageUrl = url, isUploadingImage = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isUploadingImage = false)
            }
        }
    }

    private fun uploadToCloudinary(uri: Uri): String {
        val boundary = "spendant_${System.currentTimeMillis()}"
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("No se pudo leer el archivo")
        val fileBytes = inputStream.readBytes()
        inputStream.close()

        val connection = URL(cloudinaryUploadUrl).openConnection() as HttpURLConnection
        connection.apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }

        DataOutputStream(connection.outputStream).use { out ->
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n")
            out.writeBytes("$cloudinaryUploadPreset\r\n")
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"folder\"\r\n\r\n")
            out.writeBytes("receipts/$userId\r\n")
            out.writeBytes("--$boundary\r\n")
            out.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"receipt.jpg\"\r\n")
            out.writeBytes("Content-Type: image/jpeg\r\n\r\n")
            out.write(fileBytes)
            out.writeBytes("\r\n--$boundary--\r\n")
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) throw Exception("Error: $responseCode")
        return JSONObject(connection.inputStream.bufferedReader().readText()).getString("secure_url")
    }

    fun autoFillFromReceipt(
        name: String? = null, amount: String? = null, date: String? = null,
        time: String? = null, locationName: String? = null,
        latitude: Double? = null, longitude: Double? = null
    ) {
        _uiState.value = _uiState.value.copy(
            name = name ?: _uiState.value.name,
            amount = amount ?: _uiState.value.amount,
            date = date ?: _uiState.value.date,
            time = time ?: _uiState.value.time,
            locationName = locationName ?: _uiState.value.locationName,
            latitude = latitude ?: _uiState.value.latitude,
            longitude = longitude ?: _uiState.value.longitude,
            source = ExpenseSource.OCR
        )
    }

    fun saveExpense() {
        val state = _uiState.value
        val name = state.name.trim()
        val amount = state.amount.replace("$", "").replace(",", "").trim().toDoubleOrNull()

        if (name.isEmpty() || amount == null) {
            _uiState.value = state.copy(error = "Name and amount are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, error = null)
            try {
                val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                val parsedDate = dateFormat.parse(state.date)?.time ?: System.currentTimeMillis()

                val expense = ExpenseEntity(
                    userId = userId,
                    name = name,
                    amount = amount,
                    date = parsedDate,
                    time = state.time,
                    latitude = state.latitude,
                    longitude = state.longitude,
                    locationName = state.locationName.ifEmpty { null },
                    source = state.source,
                    receiptImagePath = state.receiptStorageUrl ?: state.receiptImageUri?.toString(),
                    isPendingCategory = state.selectedLabelIds.isEmpty()
                )

                val expenseId = repository.insertExpense(expense, state.selectedLabelIds.toList(), firebaseUid)
                val id = expenseId.getOrNull()?.toInt()

                if (state.selectedLabelIds.isEmpty() && id != null) {
                    val categorized = autoCategorizationService.categorizeExpense(id, name, firebaseUid)
                    if (!categorized) {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            showLabelPrompt = true,
                            pendingExpenseId = id
                        )
                        return@launch
                    }
                }

                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = e.message)
            }
        }
    }

    fun savePendingLabels() {
        val state = _uiState.value
        val expenseId = state.pendingExpenseId ?: return

        viewModelScope.launch {
            state.selectedLabelIds.forEach { labelId ->
                autoCategorizationService.assignLabelManually(
                    expenseId = expenseId,
                    labelId = labelId,
                    expenseName = state.name,
                    firebaseUid = firebaseUid
                )
            }
            _uiState.value = _uiState.value.copy(isSaved = true, showLabelPrompt = false)
        }
    }

    fun dismissLabelPrompt() {
        _uiState.value = _uiState.value.copy(isSaved = true, showLabelPrompt = false)
    }
}
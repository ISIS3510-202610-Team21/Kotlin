package com.example.spendantt.ui.screens.report

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.spendantt.viewmodel.ReportViewModel

/**
 * Single-route container that toggles between the setup and the report view
 * depending on whether a report has been opened.
 */
@Composable
fun ReportFlowScreen(
    viewModel: ReportViewModel,
    onExit: () -> Unit,
) {
    val report by viewModel.currentReport.collectAsState()

    BackHandler(enabled = report != null) {
        viewModel.closeReport()
    }

    if (report == null) {
        ReportSetupScreen(
            viewModel = viewModel,
            onClose = onExit,
        )
    } else {
        ReportViewScreen(
            viewModel = viewModel,
            onClose = { viewModel.closeReport() },
        )
    }
}

package com.jolabs.looplog.habit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jolabs.looplog.habit.R

@Composable
fun TimePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    content: @Composable () -> Unit
) {
   Dialog(
       onDismissRequest = onDismiss,
       properties = DialogProperties(usePlatformDefaultWidth = false)
   ) {
       ElevatedCard(
           modifier = Modifier
               .wrapContentWidth()
               .background(
                   color = MaterialTheme.colorScheme.surface,
                   shape = MaterialTheme.shapes.extraLarge
               ),
           elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
           shape = MaterialTheme.shapes.extraLarge
       ){
           Column(modifier = Modifier
               .padding(16.dp)
               .verticalScroll(rememberScrollState())) {
               Text(
                   stringResource(R.string.choose_time),
                   style = MaterialTheme.typography.titleLarge)

               Spacer(modifier = Modifier.padding(8.dp))
               content()
               Row(modifier = Modifier.align(Alignment.End)){
                   TextButton(
                       onClick = onDismiss

                   ) {
                       Text(stringResource(R.string.cancel))
                   }
                   TextButton(onClick = onConfirm) {
                       Text(stringResource(R.string.set_time))
                   }

               }
           }

       }
   }
}
package com.ecoalertum.android.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.EmergencyCategory
import com.ecoalertum.android.core.data.model.EmergencyContact

/**
 * FR6.2: official emergency contacts. Tapping a number opens the phone dialler with that number.
 * Nothing is called until the student presses the call button there.
 */
@Composable
fun EmergencyContactsScreen(
    modifier: Modifier = Modifier,
    viewModel: EmergencyContactsViewModel = viewModel(factory = EmergencyContactsViewModel.Factory),
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.emergency_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (viewModel.contacts.isEmpty()) {
            item { Text(stringResource(R.string.emergency_empty), style = MaterialTheme.typography.bodyLarge) }
        }
        items(viewModel.contacts, key = { it.id }) { contact ->
            ContactCard(
                contact = contact,
                name = contact.name.get(viewModel.language),
                onCall = { number -> openDialer(context, number) },
            )
        }
    }
}

@Composable
private fun ContactCard(
    contact: EmergencyContact,
    name: String,
    onCall: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(contact.category.labelRes()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            val availability = if (contact.available24h) {
                stringResource(R.string.emergency_open_24h)
            } else {
                contact.hours?.let { stringResource(R.string.emergency_hours, it) }
            }
            if (availability != null) {
                Text(
                    text = availability,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            contact.location?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CallButton(name = name, number = contact.phone, onCall = onCall, primary = true)
                contact.altPhone?.let { CallButton(name = name, number = it, onCall = onCall, primary = false) }
            }
        }
    }
}

@Composable
private fun CallButton(name: String, number: String, onCall: (String) -> Unit, primary: Boolean) {
    val description = stringResource(R.string.emergency_call_description, name, number)
    val label = stringResource(R.string.emergency_call, number)
    val modifier = Modifier.semantics { contentDescription = description }
    if (primary) {
        Button(onClick = { onCall(number) }, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = { onCall(number) }, modifier = modifier) { Text(label) }
    }
}

private fun EmergencyCategory.labelRes(): Int = when (this) {
    EmergencyCategory.CAMPUS_SECURITY -> R.string.emergency_category_campus_security
    EmergencyCategory.MEDICAL -> R.string.emergency_category_medical
    EmergencyCategory.FIRE_RESCUE -> R.string.emergency_category_fire_rescue
    EmergencyCategory.POLICE -> R.string.emergency_category_police
    EmergencyCategory.CIVIL_DEFENCE -> R.string.emergency_category_civil_defence
    EmergencyCategory.FACILITIES -> R.string.emergency_category_facilities
}

/** Opens the dialler with the number filled in. It does not place the call. */
private fun openDialer(context: Context, number: String) {
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, R.string.emergency_no_dialer, Toast.LENGTH_LONG).show()
    }
}

package com.instantroom

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.instantroom.data.Settings
import com.instantroom.ui.*
import org.junit.*
import org.junit.Assert.*

class SettingsUiTest {
 @get:Rule val compose=createComposeRule()
 @Test fun manualDevelopmentIsAnExplicitPreference(){
  var settings=Settings()
  compose.setContent{InstantTheme{SettingsScreen(settings){settings=it}}}
  compose.onNodeWithText("Enable manual development").performScrollTo().assertIsDisplayed()
  assertFalse(settings.accessible)
 }
 @Test fun filmPrintDescribesUndevelopedState(){
  compose.setContent{InstantTheme{FilmPrint(null)}}
  compose.onNodeWithContentDescription("Undeveloped Classic photograph; shake to reveal").assertExists()
 }
}

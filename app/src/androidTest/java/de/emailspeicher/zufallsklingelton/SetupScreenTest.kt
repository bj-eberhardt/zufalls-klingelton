package de.emailspeicher.zufallsklingelton

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.emailspeicher.zufallsklingelton.ui.theme.ZufallsKlingeltonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun missingPermissions_showSetupAndDisableStart() {
        composeRule.setContent {
            ZufallsKlingeltonTheme {
                SetupScreen(
                    status = PermissionStatus(false, false, false),
                    onRequestPermissions = {},
                    onStart = {},
                )
            }
        }

        composeRule.onNodeWithText(targetContext.getString(R.string.setup_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("start_button").assertIsNotEnabled()
    }

    @Test
    fun allPermissions_enableStart() {
        composeRule.setContent {
            ZufallsKlingeltonTheme {
                SetupScreen(
                    status = PermissionStatus(true, true, true),
                    onRequestPermissions = {},
                    onStart = {},
                )
            }
        }

        composeRule.onNodeWithTag("start_button").assertIsDisplayed()
        composeRule.onNodeWithTag("start_button").assertIsEnabled()
        composeRule.onNodeWithTag("request_permissions_button").assertIsNotEnabled()
    }
}

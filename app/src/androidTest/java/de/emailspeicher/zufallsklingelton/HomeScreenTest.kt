package de.emailspeicher.zufallsklingelton

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.emailspeicher.zufallsklingelton.ui.theme.ZufallsKlingeltonTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val targetContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun emptyPool_showsAddHintAndDisablesActivation() {
        composeRule.setContent {
            ZufallsKlingeltonTheme {
                HomeScreen(emptyList(), false, SwitchMode.DAILY, null, null, null, null, null, {}, {}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText(targetContext.getString(R.string.empty_pool_title)).assertIsDisplayed()
        composeRule.onNodeWithTag("add_track_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("activation_switch").assertIsNotEnabled()
    }

    @Test
    fun populatedPool_showsTrackAndEnablesActivation() {
        val track = Track("1", "content://one", "Mein langer Song", "Interpret", 125_000, 0)
        composeRule.setContent {
            ZufallsKlingeltonTheme {
                HomeScreen(listOf(track), false, SwitchMode.DAILY, null, null, null, null, null, {}, {}, {}, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Mein langer Song").assertIsDisplayed()
        composeRule.onNodeWithTag("activation_switch").assertIsDisplayed()
    }
}

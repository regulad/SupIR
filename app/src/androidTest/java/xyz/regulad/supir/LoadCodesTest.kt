package xyz.regulad.supir

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import xyz.regulad.supir.ir.loadAllBrands
import kotlin.time.Duration
import kotlin.time.measureTimedValue

@RunWith(AndroidJUnit4::class)
class LoadCodesTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testLoadCodes() = runTest(
        timeout = Duration.INFINITE
    ) {
        val flow = loadAllBrands(appContext)

        val oneLoadTime = measureTimedValue {
            flow.toList()
        }

        Log.d("LoadCodesTest", "oneLoadTime: ${oneLoadTime.duration}, size: ${oneLoadTime.value.size}")

        val twoLoadTime = measureTimedValue {
            flow.toList()
        }

        Log.d("LoadCodesTest", "twoLoadTime: ${twoLoadTime.duration}, size: ${twoLoadTime.value.size}")
    }
}

package eu.basicairdata.graziano.tracker.export

import org.junit.Assert.assertEquals
import org.junit.Test

class MapsLinkBuilderTest {

    // Only webUrl() is a pure Kotlin function (no android.* classes), so it's the one
    // testable in a plain JVM unit test. geoUri()/shareIntent() use android.net.Uri /
    // android.content.Intent and need an instrumented (androidTest) or Robolectric run.
    @Test
    fun webUrl_formatsLatLonWithEightDecimals() {
        val url = MapsLinkBuilder.webUrl(45.5, 9.25)
        assertEquals("https://www.google.com/maps?q=45.50000000,9.25000000", url)
    }
}

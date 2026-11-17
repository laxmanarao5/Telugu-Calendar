package com.telugucalendar.app

import com.telugucalendar.app.data.update.UpdateInfo
import com.telugucalendar.app.data.update.UpdateLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTest {
    private val json = """{"versionCode":3,"versionName":"1.2.0","apk":"telugu-calendar.apk","sha256":"ABC123","size":1000,"notes":"New 2027 data\nBug fixes"}"""

    @Test fun parsesVersionJson() {
        val info = UpdateLogic.parse(json)
        assertNotNull(info)
        assertEquals(3, info!!.versionCode)
        assertEquals("1.2.0", info.versionName)
        assertEquals("New 2027 data\nBug fixes", info.notes)
    }

    @Test fun rejectsGarbageAndEmptyFiles() {
        assertNull(UpdateLogic.parse("not json"))
        assertNull(UpdateLogic.parse("{}"))
        assertNull(UpdateLogic.parse("""{"versionCode":0,"versionName":"0","apk":"x.apk"}"""))
        assertNull(UpdateLogic.parse("""{"versionCode":2,"versionName":"1","apk":""}"""))
    }

    @Test fun offersOnlyNewerVersions() {
        val info = UpdateInfo(versionCode = 3, versionName = "1.2.0")
        assertTrue(UpdateLogic.isNewer(info, 2))
        assertFalse(UpdateLogic.isNewer(info, 3))
        assertFalse(UpdateLogic.isNewer(info, 4))   // never a downgrade
    }

    @Test fun checksumIsCaseInsensitiveAndBlankMeansNotProvided() {
        assertTrue(UpdateLogic.checksumOk("ABC123", "abc123"))
        assertTrue(UpdateLogic.checksumOk("", "anything"))
        assertFalse(UpdateLogic.checksumOk("abc123", "def456"))
    }

    @Test fun hexIsLowerCaseTwoDigitsPerByte() {
        assertEquals("00ff10", UpdateLogic.hex(byteArrayOf(0, -1, 16)))
    }
}

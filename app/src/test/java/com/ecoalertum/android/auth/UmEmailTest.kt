package com.ecoalertum.android.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UmEmailTest {

    @Test
    fun acceptsStudentEmail() {
        assertTrue(UmEmail.isValid("aisyah.rahman@siswa.um.edu.my"))
    }

    @Test
    fun ignoresCaseAndSurroundingSpaces() {
        assertTrue(UmEmail.isValid("  Aisyah@Siswa.UM.edu.my "))
        assertEquals("aisyah@siswa.um.edu.my", UmEmail.normalize("  Aisyah@Siswa.UM.edu.my "))
    }

    @Test
    fun rejectsOtherDomains() {
        assertFalse(UmEmail.isValid("someone@gmail.com"))
        assertFalse(UmEmail.isValid("staff@um.edu.my"))
        assertFalse(UmEmail.isValid("someone@siswa.um.edu.my.evil.com"))
        assertFalse(UmEmail.isValid("someone@notsiswa.um.edu.my"))
    }

    @Test
    fun rejectsMissingOrBrokenName() {
        assertFalse(UmEmail.isValid(""))
        assertFalse(UmEmail.isValid("@siswa.um.edu.my"))
        assertFalse(UmEmail.isValid("a@b@siswa.um.edu.my"))
        assertFalse(UmEmail.isValid("a b@siswa.um.edu.my"))
    }
}

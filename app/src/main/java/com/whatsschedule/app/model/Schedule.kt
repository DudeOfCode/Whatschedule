package com.whatsschedule.app.model

/**
 * A single scheduled WhatsApp autoreply entry.
 *
 * @param id     stable unique id (also used as the AlarmManager request code)
 * @param name   recipient display name / label
 * @param phone  phone number with country code (digits may include +, spaces)
 * @param time   epoch millis of the scheduled send time
 * @param message the text body to pre-fill into WhatsApp
 * @param active  true = armed/pending, false = paused or already fired
 * @param fired   true once the schedule has been triggered
 * @param repeatDaily if true, re-arms for the same time the next day after firing
 */
data class Schedule(
    val id: Long,
    var name: String,
    var phone: String,
    var time: Long,
    var message: String,
    var active: Boolean = true,
    var fired: Boolean = false,
    var repeatDaily: Boolean = false
) {
    /** Phone reduced to digits only, as required by the wa.me deep link. */
    fun cleanPhone(): String = phone.filter { it.isDigit() }
}

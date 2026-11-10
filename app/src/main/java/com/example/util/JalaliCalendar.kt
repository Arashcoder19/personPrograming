package com.example.util

import java.util.Calendar

object JalaliCalendar {
    private val MONTH_NAMES = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )

    private val WEEK_DAYS = listOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    private val FA_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun toFa(input: Any?): String {
        val str = input?.toString() ?: ""
        val sb = java.lang.StringBuilder()
        for (ch in str) {
            if (ch in '0'..'9') {
                sb.append(FA_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun parseFaDigits(input: String?): String {
        if (input == null) return ""
        val sb = java.lang.StringBuilder()
        for (ch in input) {
            val idx = FA_DIGITS.indexOf(ch)
            if (idx != -1) {
                sb.append(idx)
            } else if (ch in '٠'..'٩') {
                sb.append(ch - '٠')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatToman(amount: Double): String {
        val rounded = Math.round(amount)
        val formatted = String.format("%,d", rounded)
        return toFa(formatted) + " ت"
    }

    fun formatTomanWithoutUnit(amount: Double): String {
        val rounded = Math.round(amount)
        val formatted = String.format("%,d", rounded)
        return toFa(formatted)
    }

    // Convert Gregorian (gy, gm 1..12, gd) to Jalali "yyyy/MM/dd"
    fun g2j(gy: Int, gm: Int, gd: Int): String {
        val g = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var d = 355666 + 365 * gy + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + g[gm - 1]
        var jy = -1595 + 33 * (d / 12053)
        d %= 12053
        jy += 4 * (d / 1461)
        d %= 1461
        if (d > 365) {
            jy += (d - 1) / 365
            d = (d - 1) % 365
        }
        val jm = if (d < 186) 1 + (d / 31) else 7 + ((d - 186) / 30)
        val jd = 1 + (if (d < 186) d % 31 else (d - 186) % 30)
        return String.format("%04d/%02d/%02d", jy, jm, jd)
    }

    fun today(): String {
        val cal = Calendar.getInstance()
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return g2j(gy, gm, gd)
    }

    fun addDays(n: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, n)
        val gy = cal.get(Calendar.YEAR)
        val gm = cal.get(Calendar.MONTH) + 1
        val gd = cal.get(Calendar.DAY_OF_MONTH)
        return g2j(gy, gm, gd)
    }

    fun lastDays(n: Int): List<String> {
        return (0 until n).map { addDays(-(n - 1 - it)) }
    }

    fun currentWeekday(): String {
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday is 1
        return WEEK_DAYS.getOrElse(dayOfWeek - 1) { "" }
    }

    fun normalizeDate(s: String?): String? {
        if (s.isNullOrBlank()) return ""
        val clean = parseFaDigits(s).trim()
        val parts = clean.split('/', '-', ' ')
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m < 1 || m > 12) return null
        val maxDays = if (m <= 6) 31 else 30
        if (d < 1 || d > maxDays) return null
        return String.format("%04d/%02d/%02d", y, m, d)
    }

    fun formatJalaliReadable(dateStr: String?): String {
        val norm = normalizeDate(dateStr) ?: return dateStr ?: ""
        if (norm.isEmpty()) return ""
        val parts = norm.split('/')
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        val d = parts[2].toInt()
        val monthName = MONTH_NAMES.getOrElse(m - 1) { "" }
        return "${toFa(d)} $monthName ${toFa(y)}"
    }

    fun getMonthLabel(ym: Pair<Int, Int>): String {
        val mName = MONTH_NAMES.getOrElse(ym.second - 1) { "" }
        return "$mName ${toFa(ym.first)}"
    }

    fun currentYearMonth(): Pair<Int, Int> {
        val t = today()
        val parts = t.split('/')
        return Pair(parts[0].toInt(), parts[1].toInt())
    }

    fun ymString(ym: Pair<Int, Int>): String {
        return String.format("%04d/%02d", ym.first, ym.second)
    }

    fun addMonths(ym: Pair<Int, Int>, months: Int): Pair<Int, Int> {
        val total = ym.first * 12 + (ym.second - 1) + months
        val y = total / 12
        val m = (total % 12) + 1
        return Pair(y, m)
    }
}

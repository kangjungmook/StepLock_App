package com.steplock.app.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.steplock.app.ui.theme.PlexSansKr

/**
 * "6시간 12분", "2 / 3회" 처럼 숫자와 한글 단위가 섞인 값을 그립니다.
 *
 * 숫자체(Barlow Condensed)에는 한글이 없어서, 그대로 두면 한글만 기기 기본 글꼴로
 * 바뀌어 크기·굵기가 어긋납니다. 숫자·기호는 스타일 그대로(숫자체) 두고, 한글
 * 단위만 본문 글꼴로 [unitSize] 크기로 줄여 표지판처럼 "큰 숫자 + 작은 단위"로 씁니다.
 */
fun numeralText(text: String, unitSize: TextUnit): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val hangul = text[i].isHangul()
        var j = i
        while (j < text.length && text[j].isHangul() == hangul) j++
        val part = text.substring(i, j)
        if (hangul) {
            withStyle(SpanStyle(fontFamily = PlexSansKr, fontWeight = FontWeight.Bold, fontSize = unitSize)) {
                append(part)
            }
        } else {
            append(part)
        }
        i = j
    }
}

private fun Char.isHangul(): Boolean = this in '가'..'힣' || this in 'ㄱ'..'ㆎ'

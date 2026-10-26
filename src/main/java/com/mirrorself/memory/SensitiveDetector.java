package com.mirrorself.memory;

import java.util.regex.Pattern;

/**
 * Only a conservative safety guard, not an accurate PII/medical classifier.
 * All extracted text remains owner-only, and no cloud egress exists in V0.1.
 */
public final class SensitiveDetector {
    private static final Pattern FLAG = Pattern.compile(
            "(?i)(密码|密钥|token|api.?key|身份证|护照号|银行卡|诊断|医院|抑郁|宗教|政治|" +
                    "性生活|性取向|家人|伴侣|家庭住址|住址|电话号码)");
    private SensitiveDetector() { }
    public static Sensitivity classify(String text) {
        return FLAG.matcher(text).find() ? Sensitivity.SENSITIVE : Sensitivity.NORMAL;
    }
}

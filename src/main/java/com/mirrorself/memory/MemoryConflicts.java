package com.mirrorself.memory;

import java.util.regex.Pattern;

/** Deliberately narrow deterministic conflict hints, never automatic replacement. */
public final class MemoryConflicts {
    private static final Pattern RESIDENCE=Pattern.compile("^我(?:目前|现在|曾经|以前)?(?:住在|居住在)(.+)$");
    private static final Pattern PREFERENCE=Pattern.compile("^我(不再喜欢|不喜欢|喜欢)(.+)$");
    private MemoryConflicts() { }
    public static boolean contradicts(String left,String right) {
        String a=MemoryNormalization.canonical(left), b=MemoryNormalization.canonical(right);
        var ra=RESIDENCE.matcher(a); var rb=RESIDENCE.matcher(b);
        if(ra.matches()&&rb.matches()) return !ra.group(1).equals(rb.group(1));
        var pa=PREFERENCE.matcher(a); var pb=PREFERENCE.matcher(b);
        return pa.matches()&&pb.matches()&&pa.group(2).equals(pb.group(2))
                &&pa.group(1).equals("喜欢")!=pb.group(1).equals("喜欢");
    }
}

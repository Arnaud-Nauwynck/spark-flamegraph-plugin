package fr.an.spark.plugin.flamegraph.shared.utils;

public class ThreadNameUtils {

    public static String templatizeThreadName(String name) {
        String res = name;
        int nameLen = name.length();
        int lastSep = res.lastIndexOf('-');
        if (lastSep != -1 && lastSep < nameLen) {
            // String suffix = name.substring(lastSep+1);
            boolean isSuffixNumber = true;
            for(int i = lastSep+1; i < nameLen; i++) {
                char ch = name.charAt(i);
                if (! (Character.isDigit(ch) || ch == '-' || ch == '_' || Character.isWhitespace(ch))) {
                    isSuffixNumber = false;
                    break;
                }
            }
            if (isSuffixNumber) {
                String prefix = name.substring(0, lastSep);
                res = templatizeThreadName(prefix) + "-*";
            }
        }
        return res;
    }

}

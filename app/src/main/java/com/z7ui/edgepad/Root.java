package com.z7ui.edgepad;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * root 执行。KSU 对未授权的应用会直接 EACCES（连弹窗都没有），
 * 所以只能真跑一次 su 看结果，不能靠「su 文件在不在」判断。
 */
public class Root {

    public static class Result {
        public final boolean ok;
        public final String out;

        Result(boolean ok, String out) {
            this.ok = ok;
            this.out = out;
        }
    }

    public static boolean available(Context c) {
        Result r = run(c, "id");
        return r.ok && r.out.contains("uid=0");
    }

    public static Result run(Context c, String cmd) {
        Process p = null;
        try {
            p = new ProcessBuilder("su", "-c", cmd).redirectErrorStream(true).start();
            if (!p.waitFor(15, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return new Result(false, "timeout");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                    if (sb.length() > 4000) break;
                }
            }
            return new Result(p.exitValue() == 0, sb.toString().trim());
        } catch (IOException e) {
            // KSU 未授权时这里报 EACCES（error=13）
            return new Result(false, String.valueOf(e));
        } catch (InterruptedException e) {
            if (p != null) p.destroyForcibly();
            return new Result(false, "interrupted");
        } finally {
            if (p != null) {
                try {
                    p.getInputStream().close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

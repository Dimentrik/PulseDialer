package ru.pulsedialer.app;
import android.content.*;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.telecom.*;
import android.widget.Toast;
import java.io.File;
import java.util.*;

public class Engine {
public static class Rec {
public String n, p, f, s, note; public int d;
public Rec(String n, String p, String f, int d, String s, String note) { this.n=n; this.p=p; this.f=f; this.d=d; this.s=s; this.note=note; }
}
public static class Con {
public String n, p, r;
public Con(String n, String p, String r) { this.n=n; this.p=p; this.r=r; }
}
public static int st=0, cSec=0, rSec=0, tab=0, sim=0;
public static String ph="+7 (916) 450-12-89", nm="Алексей Морозов", q="", dial="+7 (916) 450-12-89";
public static boolean rec=false, auto=false, spk=false;
public static Call sysCall; public static InCallService srv; public static MediaRecorder mr;
public static File rf; public static long rStart; public static MediaPlayer mp; public static MainActivity inst;

public static final List<Rec> recs = new ArrayList<>(Arrays.asList(
new Rec("Алексей Морозов", "+7 (916) 450-12-89", "", 148, "SIM 1", "Согласование сметы (Ответить с записью)"),
new Rec("Алексей Морозов", "+7 (916) 450-12-89", "", 64, "SIM 1", "Уточнение адреса склада"),
new Rec("Екатерина Власова", "+7 (925) 781-34-10", "", 215, "SIM 2", "Обсуждение договора аренды")
));
public static final List<Con> cons = new ArrayList<>(Arrays.asList(
new Con("Алексей Морозов", "+7 (916) 450-12-89", "Техдиректор • СтройИнвест"),
new Con("Екатерина Власова", "+7 (925) 781-34-10", "Юридический отдел"),
new Con("Дмитрий Соколов", "+7 (903) 512-90-44", "Руководитель логистики")
));

public static final Handler h = new Handler(Looper.getMainLooper());
public static final Runnable tick = new Runnable() {
@Override public void run() { if (st == 2) { cSec++; if (rec) rSec++; ui(); h.postDelayed(this, 1000L); } }
};
public static final Call.Callback cb = new Call.Callback() {
@Override public void onStateChanged(Call c, int s) { sync(c, s); }
};

public static void ui() { if (inst != null) h.post(inst::render); }
public static String norm(String s) { String d = s == null ? "" : s.replaceAll("\\D+", ""); return d.length() > 10 ? d.substring(d.length() - 10) : d; }
public static String fmt(int s) { return String.format(Locale.US, "%02d:%02d", s / 60, s % 60); }
public static GradientDrawable box(int bg, int sc, int r) { GradientDrawable g = new GradientDrawable(); g.setColor(bg); g.setCornerRadius(r); if (sc != 0) g.setStroke(3, sc); return g; }

public static void attach(InCallService s, Call c) { srv = s; if (sysCall != null) sysCall.unregisterCallback(cb); sysCall = c; c.registerCallback(cb); sync(c, c.getState()); }
public static void detach(Call c) { if (sysCall == c) { c.unregisterCallback(cb); sysCall = null; endCall(); } }
public static void sync(Call c, int s) {
ph = (c.getDetails() != null && c.getDetails().getHandle() != null) ? c.getDetails().getHandle().getSchemeSpecificPart() : "+7 (900) 000-00-00";
nm = ph; for (Con x : cons) if (norm(x.p).equals(norm(ph))) nm = x.n;
if (s == Call.STATE_RINGING) st = 1;
else if (s == Call.STATE_DISCONNECTED || s == Call.STATE_DISCONNECTING) { endCall(); return; }
else { st = 2; h.removeCallbacks(tick); h.postDelayed(tick, 1000L); if (auto && !rec) startRec(); }
ui();
}
public static void ans(boolean withRec) {
auto = withRec; if (sysCall != null) sysCall.answer(0); st = 2; cSec = 0;
h.removeCallbacks(tick); h.postDelayed(tick, 1000L); if (withRec) startRec(); ui();
}
public static void endCall() {
if (sysCall != null) { try { if (sysCall.getState() == Call.STATE_RINGING) sysCall.reject(false, null); else sysCall.disconnect(); } catch (Exception e) {} }
h.removeCallbacks(tick); if (rec) stopRec(); st = 0; ui();
}
public static void startRec() {
if (inst == null) return;
try { if (mr != null) { mr.stop(); mr.release(); } } catch (Exception e) {} mr = null;
rf = new File(inst.getFilesDir(), "R_" + System.currentTimeMillis() + ".m4a");
for (int s : new int[]{6, 1, 4}) {
try { MediaRecorder r = new MediaRecorder(); r.setAudioSource(s); r.setOutputFormat(2); r.setAudioEncoder(3); r.setOutputFile(rf.getAbsolutePath()); r.prepare(); r.start(); mr = r; break; } catch (Exception e) { mr = null; }
}
rStart = System.currentTimeMillis(); rec = true; rSec = 0; ui();
}
public static void stopRec() {
if (!rec) return;
int d = Math.max(1, (int)((System.currentTimeMillis() - rStart) / 1000L));
try { if (mr != null) { mr.stop(); mr.release(); } } catch (Exception e) {} mr = null;
recs.add(0, new Rec(nm, ph, rf != null ? rf.getAbsolutePath() : "", d, "SIM " + (sim + 1), auto ? "Автозапись (Ответить с записью)" : "Запись во время звонка"));
rec = false; rSec = 0; ui();
}
public static void play(Rec r) {
try { if (mp != null) { mp.stop(); mp.release(); } } catch (Exception e) {} mp = null;
if (!r.f.isEmpty() && new File(r.f).exists()) { try { mp = new MediaPlayer(); mp.setDataSource(r.f); mp.prepare(); mp.start(); } catch (Exception e) {} }
if (inst != null) Toast.makeText(inst, "Воспроизведение: " + r.note + " (" + fmt(r.d) + ")", Toast.LENGTH_SHORT).show();
}
}

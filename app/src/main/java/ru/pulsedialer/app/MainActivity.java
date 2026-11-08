package ru.pulsedialer.app;
import android.Manifest;
import android.app.*;
import android.app.role.RoleManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.ContactsContract;
import android.telecom.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
@Override protected void onCreate(Bundle b) {
super.onCreate(b); Engine.inst = this;
if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
requestPermissions(new String[]{Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALL_LOG, Manifest.permission.RECORD_AUDIO, Manifest.permission.ANSWER_PHONE_CALLS}, 1);
try (Cursor c = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, new String[]{"display_name", "data1"}, null, null, null)) {
if (c != null) while (c.moveToNext() && Engine.cons.size() < 60) Engine.cons.add(new Engine.Con(c.getString(0), c.getString(1), "Контакт телефона"));
} catch (Exception e) {}
render();
}
@SuppressWarnings("MissingPermission")
void callOut(String p, String n, boolean wRec) {
Engine.ph = p; Engine.nm = n.isEmpty() ? p : n;
for (Engine.Con x : Engine.cons) if (Engine.norm(x.p).equals(Engine.norm(p))) Engine.nm = x.n;
Engine.auto = wRec; Engine.st = 2; Engine.cSec = 0;
try {
TelecomManager tm = (TelecomManager) getSystemService(TELECOM_SERVICE); Bundle b = new Bundle();
List<PhoneAccountHandle> a = tm.getCallCapablePhoneAccounts();
if (a != null && !a.isEmpty()) b.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, a.get(Math.min(Engine.sim, a.size() - 1)));
tm.placeCall(Uri.fromParts("tel", p, null), b);
} catch (Exception e) {}
Engine.h.removeCallbacks(Engine.tick); Engine.h.postDelayed(Engine.tick, 1000L);
if (wRec) Engine.startRec(); render();
}
public void render() {
LinearLayout r = new LinearLayout(this); r.setOrientation(1); r.setBackgroundColor(0xFF070A12); r.setPadding(40, 48, 40, 32);
if (Engine.st > 0) {
r.setGravity(1);
tv(r, "SIM " + (Engine.sim + 1) + " • Вызов", 13, 0xFF00F0FF); tv(r, Engine.nm, 26, 0xFFFFFFFF); tv(r, Engine.ph, 18, 0xFF00F0FF);
tv(r, Engine.st == 1 ? "Входящий вызов..." : "Разговор • " + Engine.fmt(Engine.cSec), 15, 0xFF94A3B8);
if (Engine.rec) tv(r, "● ИДЕТ ЗАПИСЬ РАЗГОВОРА • " + Engine.fmt(Engine.rSec), 15, 0xFFF43F5E);
r.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
if (Engine.st == 1) {
btn(r, "● ОТВЕТИТЬ С ЗАПИСЬЮ (Автозапись)", 0xFF152238, 0xFF00F0FF, v -> Engine.ans(true));
btn(r, "📞 ОТВЕТИТЬ", 0xFF10B981, 0, v -> Engine.ans(false));
btn(r, "✕ ОТКЛОНИТЬ", 0xFFF43F5E, 0, v -> Engine.endCall());
} else {
btn(r, Engine.rec ? "■ ОСТАНОВИТЬ ЗАПИСЬ (" + Engine.fmt(Engine.rSec) + ")" : "● НАЧАТЬ ЗАПИСЬ РАЗГОВОРА", Engine.rec ? 0xFFF43F5E : 0xFF17223B, 0xFF00F0FF, v -> { if (Engine.rec) Engine.stopRec(); else Engine.startRec(); });
btn(r, Engine.spk ? "Динамик: ВКЛ" : "Включить динамик", 0xFF17223B, 0, v -> { Engine.spk = !Engine.spk; if (Engine.srv != null) Engine.srv.setAudioRoute(Engine.spk ? 8 : 1); render(); });
btn(r, "ЗАВЕРШИТЬ ВЫЗОВ", 0xFFF43F5E, 0, v -> Engine.endCall());
}
setContentView(r); return;
}
btn(r, "PULSE ЗВОНИЛКА • Тест входящего звонка (3 кнопки)", 0xFF17223B, 0xFF00F0FF, v -> { Engine.ph = "+7 (916) 450-12-89"; Engine.nm = "Алексей Морозов"; Engine.st = 1; Engine.rec = false; render(); });
btn(r, "Сделать звонилкой по умолчанию (ROLE_DIALER)", 0xFF111827, 0, v -> { if (Build.VERSION.SDK_INT >= 29) { RoleManager rm = getSystemService(RoleManager.class); if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER), 2); } });
ScrollView sc = new ScrollView(this); LinearLayout bd = new LinearLayout(this); bd.setOrientation(1); sc.addView(bd); r.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1f));
if (Engine.tab == 0) {
btn(bd, Engine.sim == 0 ? "Линия: SIM 1 (Нажмите для SIM 2)" : "Линия: SIM 2 (Нажмите для SIM 1)", 0xFF17223B, 0xFF00F0FF, v -> { Engine.sim = 1 - Engine.sim; render(); });
EditText et = new EditText(this); et.setText(Engine.dial); et.setTextColor(0xFFFFFFFF); et.setTextSize(22f); et.setGravity(17); bd.addView(et);
for (String[] row : new String[][]{{"1","2","3"},{"4","5","6"},{"7","8","9"},{"*","0","⌫"}}) {
LinearLayout lr = new LinearLayout(this);
for (String k : row) {
Button b = new Button(this); b.setText(k); b.setTextColor(0xFFFFFFFF); b.setBackground(Engine.box(0xFF17223B, 0, 28));
b.setOnClickListener(v -> { Engine.dial = "⌫".equals(k) ? (Engine.dial.isEmpty() ? "" : Engine.dial.substring(0, Engine.dial.length() - 1)) : Engine.dial + k; render(); });
lr.addView(b, new LinearLayout.LayoutParams(0, 118, 1f));
}
bd.addView(lr);
}
btn(bd, "📞 Позвонить (SIM " + (Engine.sim + 1) + ")", 0xFF10B981, 0, v -> callOut(et.getText().toString(), "", false));
btn(bd, "● Вызов + Запись разговора", 0xFFF43F5E, 0, v -> callOut(et.getText().toString(), "", true));
} else if (Engine.tab == 1) {
searchBox(bd, "Поиск контакта...");
for (Engine.Con c : Engine.cons) {
if (!Engine.q.isEmpty() && !c.n.toLowerCase().contains(Engine.q.toLowerCase()) && !c.p.contains(Engine.q)) continue;
int rc = 0; for (Engine.Rec x : Engine.recs) if (Engine.norm(x.p).equals(Engine.norm(c.p))) rc++;
btn(bd, c.n + " (" + c.p + ") • Записей: " + rc + " шт.", 0xFF17223B, 0xFF00F0FF, v -> openContact(c));
}
} else {
searchBox(bd, "Поиск записей по имени, номеру, заметке...");
for (Engine.Rec x : Engine.recs) {
if (!Engine.q.isEmpty() && !x.n.toLowerCase().contains(Engine.q.toLowerCase()) && !x.p.contains(Engine.q) && !x.note.toLowerCase().contains(Engine.q.toLowerCase())) continue;
btn(bd, "▶ " + x.n + " (" + x.p + ") • " + x.note + " [" + Engine.fmt(x.d) + "]", 0xFF17223B, 0xFF00F0FF, v -> Engine.play(x));
}
}
LinearLayout nav = new LinearLayout(this); String[] lbs = {"Набор", "Контакты", "Записи (" + Engine.recs.size() + ")"};
for (int i = 0; i < 3; i++) {
final int ti = i; Button b = new Button(this); b.setText(lbs[i]); b.setAllCaps(false);
b.setTextColor(Engine.tab == i ? 0xFF041019 : 0xFFFFFFFF); b.setBackground(Engine.box(Engine.tab == i ? 0xFF00F0FF : 0xFF17223B, 0, 30));
b.setOnClickListener(v -> { Engine.tab = ti; render(); }); nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1f));
}
r.addView(nav); setContentView(r);
}
void openContact(Engine.Con c) {
LinearLayout bx = new LinearLayout(this); bx.setOrientation(1); bx.setPadding(32, 24, 32, 24);
btn(bx, "📞 Позвонить (" + c.p + ")", 0xFF10B981, 0, v -> callOut(c.p, c.n, false));
btn(bx, "● Вызов с автозаписью", 0xFFF43F5E, 0, v -> callOut(c.p, c.n, true));
for (Engine.Rec x : Engine.recs) if (Engine.norm(x.p).equals(Engine.norm(c.p))) btn(bx, "▶ Слушать: " + x.note + " (" + Engine.fmt(x.d) + ")", 0xFF17223B, 0xFF00F0FF, v -> Engine.play(x));
new AlertDialog.Builder(this).setTitle(c.n).setView(bx).setPositiveButton("Закрыть", null).show();
}
void searchBox(LinearLayout p, String hnt) {
EditText et = new EditText(this); et.setHint(hnt); et.setText(Engine.q); et.setTextColor(0xFFFFFFFF); p.addView(et);
btn(p, "Найти / Фильтровать", 0xFF17223B, 0, v -> { Engine.q = et.getText().toString(); render(); });
}
void tv(LinearLayout p, String t, float s, int c) { TextView v = new TextView(this); v.setText(t); v.setTextSize(s); v.setTextColor(c); v.setGravity(17); v.setPadding(0, 12, 0, 12); p.addView(v); }
void btn(LinearLayout p, String t, int bg, int sc, View.OnClickListener l) { Button b = new Button(this); b.setText(t); b.setAllCaps(false); b.setTextColor(0xFFFFFFFF); b.setBackground(Engine.box(bg, sc, 34)); b.setOnClickListener(l); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = 14; p.addView(b, lp); }
}

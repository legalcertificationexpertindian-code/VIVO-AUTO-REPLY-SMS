package com.example.vivoautoreply

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.telephony.SubscriptionManager
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

class CallReceiver : BroadcastReceiver() {
 override fun onReceive(context:Context, intent:Intent) {
  if(intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
  val state=intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
  val p=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
  when(state) {
   TelephonyManager.EXTRA_STATE_RINGING -> p.edit().putLong("ring_started",System.currentTimeMillis()).putBoolean("ringing",true).apply()
   TelephonyManager.EXTRA_STATE_OFFHOOK -> p.edit().putBoolean("ringing",false).apply()
   TelephonyManager.EXTRA_STATE_IDLE -> {
    if(!p.getBoolean("ringing",false)) return
    p.edit().putBoolean("ringing",false).apply()
    if(!p.getBoolean("enabled",false)) return
    val pending=goAsync()
    // The call log may appear after the phone-state IDLE broadcast.
    // Keep this async receiver short (< 10 seconds) and retry only while unconfirmed.
    val handler = Handler(Looper.getMainLooper())
    val delays = longArrayOf(700L, 1700L, 3200L, 5200L)
    fun attempt(index: Int) {
     handler.postDelayed({
      val done = try { sendIfMissed(context) } catch (e: Exception) { android.util.Log.e("VivoAutoReply", "Call verification failed", e); false }
      if (done || index == delays.lastIndex) pending.finish()
      else attempt(index + 1)
     }, if (index == 0) delays[index] else delays[index] - delays[index-1])
    }
    attempt(0)
   }
  }
 }
 private fun sendIfMissed(c:Context): Boolean {
  val p=c.getSharedPreferences("settings",Context.MODE_PRIVATE)
  if(!p.getBoolean("enabled",false)) return true
  if(c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)!=PackageManager.PERMISSION_GRANTED || c.checkSelfPermission(Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED) return true
  val now=LocalDateTime.now()
  val dayIndex=now.dayOfWeek.value-1
  val today=now.toLocalDate().toString()
  val msg=when {
   p.getBoolean("holiday_on",false) && p.getString("holiday_date","")==today -> p.getString("holiday_msg","")
   now.dayOfWeek==DayOfWeek.FRIDAY && p.getBoolean("friday",true) && isLunch(now.toLocalTime(),p.getString("start","13:00")!!,p.getString("end","14:30")!!) -> p.getString("friday_msg","")
   p.getBoolean("day_${dayIndex}_on",dayIndex==6) && isLunch(now.toLocalTime(),p.getString("day_${dayIndex}_start","00:00")!!,p.getString("day_${dayIndex}_end","23:59")!!) -> p.getString("day_${dayIndex}_msg","")
   else -> null
  }?.trim()
  if(msg.isNullOrEmpty()) return true
  val since=p.getLong("ring_started",0L)
  if(since==0L || System.currentTimeMillis()-since>120_000L) return true
  val projection=arrayOf(CallLog.Calls._ID,CallLog.Calls.NUMBER,CallLog.Calls.TYPE,CallLog.Calls.DATE)
  try {
   c.contentResolver.query(CallLog.Calls.CONTENT_URI,projection,"${CallLog.Calls.DATE} >= ?",arrayOf((since-5000L).toString()),"${CallLog.Calls.DATE} DESC")?.use { cursor ->
    val idCol=cursor.getColumnIndexOrThrow(CallLog.Calls._ID)
    val numCol=cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
    val typeCol=cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
    val dateCol=cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
    while(cursor.moveToNext()) {
     val date=cursor.getLong(dateCol)
     if(date>System.currentTimeMillis()+5000L) continue
     if(cursor.getInt(typeCol)!=CallLog.Calls.MISSED_TYPE) continue
     val id=cursor.getLong(idCol)
     val number=cursor.getString(numCol) ?: return true
     if(number.isBlank() || number.equals("unknown",true) || p.getLong("last_call_id",-1L)==id) return true
     val lastNumber=p.getString("last_number","")
     val lastTime=p.getLong("last_sms_time",0L)
     if(lastNumber==number && System.currentTimeMillis()-lastTime<4*60*60*1000L) return true
     val subId=p.getInt("sim",-1)
     val manager=if(subId>=0) SmsManager.getSmsManagerForSubscriptionId(subId) else SmsManager.getDefault()
     val parts=manager.divideMessage(msg)
     // Deduplicate before submission; a carrier delivery receipt is not implied.
     p.edit().putLong("last_call_id",id).putString("last_number",number)
      .putLong("last_sms_time",System.currentTimeMillis()).putString("last_status","Submitting SMS to " + number).apply()
     try {
      if(parts.size==1) manager.sendTextMessage(number,null,msg,null,null)
      else manager.sendMultipartTextMessage(number,null,parts,null,null)
      p.edit().putString("last_status","SMS submitted to Android for " + number).apply()
     } catch (e:Exception) {
      p.edit().putString("last_status","SMS submission failed: " + (e.message ?: "unknown error")).apply()
      android.util.Log.e("VivoAutoReply","SMS submission failed",e)
     }
     return true
    }
   }
  } catch (e:Exception) { android.util.Log.e("VivoAutoReply","Could not inspect call log",e) }
  return false
 }
 private fun isLunch(t:LocalTime,s:String,e:String):Boolean = try { !t.isBefore(LocalTime.parse(s)) && (t.isBefore(LocalTime.parse(e)) || (e=="23:59" && t.hour==23 && t.minute==59)) } catch (_:Exception) { false }
}

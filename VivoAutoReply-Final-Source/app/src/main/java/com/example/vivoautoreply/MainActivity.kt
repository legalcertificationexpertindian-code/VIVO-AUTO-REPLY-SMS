package com.example.vivoautoreply

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {
 private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
 private val permissions = arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CALL_LOG, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_NUMBERS)
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,32,32,32) }
  val scroll=ScrollView(this); scroll.addView(layout); setContentView(scroll)
  fun label(s:String) { layout.addView(TextView(this).apply { text=s; textSize=17f; setPadding(0,18,0,8) }) }
  label("Vivo Auto SMS Reply")
  val enabled=Switch(this).apply { text="Enable automatic replies"; isChecked=prefs.getBoolean("enabled",false) }; layout.addView(enabled)
  label("WEEKLY AUTO-REPLY SCHEDULES")
  val days=listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
  val daySwitches=mutableListOf<Switch>()
  val dayStarts=mutableListOf<EditText>()
  val dayEnds=mutableListOf<EditText>()
  val dayMessages=mutableListOf<EditText>()
  days.forEachIndexed { i, day ->
   val on=Switch(this).apply { text="$day automatic reply"; isChecked=prefs.getBoolean("day_${i}_on",i==6) }; layout.addView(on); daySwitches.add(on)
   label("$day start time (HH:mm)")
   val start=EditText(this).apply { setText(prefs.getString("day_${i}_start","00:00")); inputType=InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME }; layout.addView(start); dayStarts.add(start)
   label("$day end time (HH:mm; 00:00 to 23:59 for full day)")
   val end=EditText(this).apply { setText(prefs.getString("day_${i}_end","23:59")); inputType=InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME }; layout.addView(end); dayEnds.add(end)
   label("$day SMS message")
   val default=if(i==6) "Dear Customer, our office is closed on Sunday. Please contact us Monday to Saturday during office hours. Thank you." else "Dear Customer, we are currently unavailable. Please call again during office hours, or we will call you back shortly. Thank you."
   val msg=EditText(this).apply { setText(prefs.getString("day_${i}_msg",default)); minLines=2 }; layout.addView(msg); dayMessages.add(msg)
  }
  label("FRIDAY LUNCH BREAK (takes priority over Friday daily schedule)")
  val friday=Switch(this).apply { text="Enable Friday lunch reply"; isChecked=prefs.getBoolean("friday",true) }; layout.addView(friday)
  label("Friday lunch start (HH:mm)")
  val start=EditText(this).apply { setText(prefs.getString("start","13:00")); inputType=InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME }; layout.addView(start)
  label("Friday lunch end (HH:mm)")
  val end=EditText(this).apply { setText(prefs.getString("end","14:30")); inputType=InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_TIME }; layout.addView(end)
  label("Friday lunch message")
  val fridayText=EditText(this).apply { setText(prefs.getString("friday_msg","Dear Customer, we are currently on our Friday lunch break. Please call after 2:30 PM, or we will call you back shortly. Thank you.")); minLines=3 }; layout.addView(fridayText)
  label("SPECIAL HOLIDAY / FESTIVAL (takes highest priority)")
  val holiday=Switch(this).apply { text="Enable special holiday reply"; isChecked=prefs.getBoolean("holiday_on",false) }; layout.addView(holiday)
  label("Holiday date (YYYY-MM-DD)")
  val holidayDate=EditText(this).apply { setText(prefs.getString("holiday_date","")); hint="2026-11-08" }; layout.addView(holidayDate)
  label("Holiday message")
  val holidayText=EditText(this).apply { setText(prefs.getString("holiday_msg","Dear Customer, our office is closed today due to a festival. We will be available on the next working day. Thank you.")); minLines=3 }; layout.addView(holidayText)
  label("SMS SIM subscription ID (-1 = default SMS SIM)")
  val sim=EditText(this).apply { setText(prefs.getInt("sim",-1).toString()); inputType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED }; layout.addView(sim)
  val sims=TextView(this); layout.addView(sims)
  val subs= getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
  if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)==PackageManager.PERMISSION_GRANTED) {
   try { sims.text="Available SIMs: " + (subs.activeSubscriptionInfoList?.joinToString { "SIM ${it.simSlotIndex+1}: subscription ID ${it.subscriptionId}" } ?: "None") } catch (_:SecurityException) {} }
  label("LAST SMS ATTEMPT")
  val status=TextView(this).apply { text=prefs.getString("last_status","No SMS attempts recorded yet") }; layout.addView(status)
  val refresh=Button(this).apply { text="Refresh SMS status" }; layout.addView(refresh)
  refresh.setOnClickListener { status.text=prefs.getString("last_status","No SMS attempts recorded yet") }
  val save=Button(this).apply { text="Save settings" }; layout.addView(save)
  save.setOnClickListener {
   val timeRegex=Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
   if(!timeRegex.matches(start.text.toString()) || !timeRegex.matches(end.text.toString()) || start.text.toString()>=end.text.toString()) { Toast.makeText(this,"Enter valid start/end times",Toast.LENGTH_LONG).show(); return@setOnClickListener }
   if(dayStarts.indices.any { !timeRegex.matches(dayStarts[it].text.toString()) || !timeRegex.matches(dayEnds[it].text.toString()) || dayStarts[it].text.toString() >= dayEnds[it].text.toString() }) { Toast.makeText(this,"Check daily HH:mm times",Toast.LENGTH_LONG).show(); return@setOnClickListener }
   if(holiday.isChecked && runCatching { java.time.LocalDate.parse(holidayDate.text.toString()) }.isFailure) { Toast.makeText(this,"Enter valid holiday date YYYY-MM-DD",Toast.LENGTH_LONG).show(); return@setOnClickListener }
   val editor=prefs.edit().putBoolean("enabled",enabled.isChecked).putBoolean("friday",friday.isChecked).putString("friday_msg",fridayText.text.toString()).putString("start",start.text.toString()).putString("end",end.text.toString()).putBoolean("holiday_on",holiday.isChecked).putString("holiday_date",holidayDate.text.toString()).putString("holiday_msg",holidayText.text.toString()).putInt("sim",sim.text.toString().toIntOrNull() ?: -1)
   days.indices.forEach { i -> editor.putBoolean("day_${i}_on",daySwitches[i].isChecked).putString("day_${i}_start",dayStarts[i].text.toString()).putString("day_${i}_end",dayEnds[i].text.toString()).putString("day_${i}_msg",dayMessages[i].text.toString()) }
   editor.apply()
   Toast.makeText(this,"Settings saved",Toast.LENGTH_SHORT).show()
  }
  val grant=Button(this).apply { text="Grant required permissions" }; layout.addView(grant)
  grant.setOnClickListener { requestPermissions(permissions.filter { checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED }.toTypedArray(),1) }
  label("Requires Call Log, Phone State and SMS permissions. Messages are sent from your phone's SIM. Carrier charges may apply. Test on your Vivo device. Disable battery restrictions/autostart blocking for this app if your device offers those settings.")
 }
}

package com.example.vivoautoreply

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.BitmapFactory
import android.os.Bundle
import android.telephony.SubscriptionManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import java.time.LocalDate
import java.util.Calendar

class MainActivity : Activity() {
 private val p by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
 private val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
 private val navy=Color.rgb(19,35,64); private val blue=Color.rgb(5,103,245); private val muted=Color.rgb(103,115,137); private val bg=Color.rgb(246,249,253)
 private lateinit var root: LinearLayout
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun shape(color:Int, radius:Int=18)=GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat() }
 private fun text(s:String,size:Float=15f,bold:Boolean=false,color:Int=navy)=TextView(this).apply { text=s; textSize=size; setTextColor(color); if(bold) setTypeface(null,Typeface.BOLD) }
 private fun space(n:Int)=Space(this).apply { minimumHeight=dp(n) }
 private fun spaceHorizontal(n:Int)=Space(this).apply { minimumWidth=dp(n) }
 private fun card():LinearLayout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; background=shape(Color.WHITE,22); elevation=dp(1).toFloat(); setPadding(dp(18),dp(15),dp(18),dp(15)); layoutParams=LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(12) } }
 private fun row():LinearLayout=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
 private fun button(label:String, action:()->Unit)=Button(this).apply { text=label; isAllCaps=false; setTextColor(Color.WHITE); background=shape(blue,13); setPadding(dp(10),dp(6),dp(10),dp(6)); setOnClickListener{action()} }
 private fun addCard(c:LinearLayout){root.addView(c)}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); render()}
 private fun render(){
  window.statusBarColor=navy; window.navigationBarColor=bg; window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
  val scroll=ScrollView(this).apply { fillViewport=true; setBackgroundColor(bg) }
  root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(17),dp(20),dp(17),dp(28)) }; scroll.addView(root); setContentView(scroll)
  val brand=row(); val logo=ImageView(this).apply { setImageResource(com.example.vivoautoreply.R.drawable.auto_sms_logo); scaleType=ImageView.ScaleType.FIT_CENTER };brand.addView(logo,LinearLayout.LayoutParams(dp(54),dp(54)));val brandTitle=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,0,0)};brandTitle.addView(text("AUTO SMS",25f,true));brandTitle.addView(text("Smart missed-call replies",12f,false,muted));brand.addView(brandTitle);root.addView(brand);root.addView(space(18))
  val hero=card().apply { background=shape(Color.rgb(232,242,255),22) };hero.addView(text("YOUR SMART RECEPTIONIST",11f,true,blue));hero.addView(space(7));hero.addView(text("Never leave a missed call unanswered.",20f,true));hero.addView(space(5));hero.addView(text("Automatic SMS from your selected SIM, following your saved schedules.",13f,false,muted));addCard(hero)
  val master=card(); val top=row(); val heading=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; heading.addView(text("Automatic replies",18f,true)); heading.addView(text(if(p.getBoolean("enabled",false))"Active · schedule rules apply" else "Paused · no automatic SMS",12f,false,muted)); top.addView(heading,LinearLayout.LayoutParams(0,-2,1f)); val toggle=Switch(this).apply { isChecked=p.getBoolean("enabled",false); setOnCheckedChangeListener{_,v->p.edit().putBoolean("enabled",v).apply();render()} }; top.addView(toggle); master.addView(top); master.addView(space(11)); master.addView(text("Only unanswered missed calls are eligible. Saved and unsaved numbers are treated equally.",13f,false,muted)); addCard(master)
  val quick=row();val q1=button("Holiday settings") { editSpecial(true) };val q2=button("SMS SIM") { chooseSim() };quick.addView(q1,LinearLayout.LayoutParams(0,dp(48),1f));quick.addView(spaceHorizontal(10));quick.addView(q2,LinearLayout.LayoutParams(0,dp(48),1f));root.addView(quick);root.addView(space(20))
  root.addView(text("Weekly schedule",19f,true)); root.addView(text("Tap a day to edit its message and time",12f,false,muted)); root.addView(space(12))
  days.forEachIndexed { i,d ->
   val c=card(); val r=row(); val left=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }; val on=p.getBoolean("day_${i}_on",i==6)
   left.addView(text(d,16f,true)); left.addView(text(if(on)"${p.getString("day_${i}_start","00:00")} – ${p.getString("day_${i}_end","23:59")}" else "Off",12f,false,if(on)blue else muted)); r.addView(left,LinearLayout.LayoutParams(0,-2,1f))
   val sw=Switch(this).apply { isChecked=on; setOnCheckedChangeListener{_,v->p.edit().putBoolean("day_${i}_on",v).apply();render()} }; r.addView(sw); c.addView(r)
   val edit=TextView(this).apply { text="Edit schedule & message   ›"; textSize=13f; setTextColor(blue); setPadding(0,dp(13),0,0); setOnClickListener{editDay(i)} }; c.addView(edit); addCard(c)
  }
  root.addView(space(7)); root.addView(text("Special schedules",19f,true)); root.addView(space(9))
  special("Friday lunch break",if(p.getBoolean("friday",true))"${p.getString("start","13:00")} – ${p.getString("end","14:30")}" else "Off", "friday",true){editSpecial(false)}
  special("Festival / office holiday",if(p.getBoolean("holiday_on",false))"${p.getString("holiday_date","")}" else "Off", "holiday_on",false){editSpecial(true)}
  val tools=card(); tools.addView(text("Device & SMS settings",17f,true)); tools.addView(space(10)); tools.addView(button("Choose SMS SIM") { chooseSim() }); tools.addView(space(9)); tools.addView(button("Check / grant permissions") { grant() }); tools.addView(space(12)); tools.addView(text("LAST SMS ATTEMPT",11f,true,muted)); tools.addView(space(4)); tools.addView(text(p.getString("last_status","No SMS attempts recorded") ?: "",13f)); addCard(tools)
  root.addView(text("Priority: Holiday → Friday lunch → Weekly schedule. SMS charges may apply. Android restrictions can delay or prevent sending.",12f,false,muted))
 }
 private fun special(name:String,detail:String,key:String,default:Boolean,edit:()->Unit){ val c=card(); val r=row(); val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};l.addView(text(name,16f,true));l.addView(text(detail,12f,false,muted));r.addView(l,LinearLayout.LayoutParams(0,-2,1f));r.addView(Switch(this).apply{isChecked=p.getBoolean(key,default);setOnCheckedChangeListener{_,v->p.edit().putBoolean(key,v).apply();render()}});c.addView(r);c.addView(TextView(this).apply{text="Edit message & timing   ›";textSize=13f;setTextColor(blue);setPadding(0,dp(13),0,0);setOnClickListener{edit()}});addCard(c) }
 private fun field(container:LinearLayout,title:String,value:String,lines:Int=1):EditText{ container.addView(space(12));container.addView(text(title,13f,true,muted)); val e=EditText(this).apply { setText(value);textSize=15f;minLines=lines;maxLines=if(lines>1)6 else 1;setSelectAllOnFocus(false); if(lines>1)inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE };container.addView(e);return e }
 private fun dialog(title:String,content:LinearLayout,save:()->Unit){AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply{addView(content)}).setNegativeButton("Cancel",null).setPositiveButton("Save"){_,_->save()}.show()}
 private fun valid(a:String,b:String)=Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(a)&&Regex("(?:[01]\\d|2[0-3]):[0-5]\\d").matches(b)&&a<b
 private fun error(){Toast.makeText(this,"Use valid HH:mm times; start must be before end",Toast.LENGTH_LONG).show()}
 private fun editDay(i:Int){val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,dp(20),dp(10))};val a=field(c,"Start (HH:mm)",p.getString("day_${i}_start","00:00")!!);val b=field(c,"End (HH:mm)",p.getString("day_${i}_end","23:59")!!);val def=if(i==6)"Dear Customer, our office is closed on Sunday. Please contact us Monday to Saturday during office hours. Thank you." else "Dear Customer, we are currently unavailable. Please call again during office hours, or we will call you back shortly. Thank you.";val m=field(c,"SMS message",p.getString("day_${i}_msg",def)!!,3);dialog("${days[i]} reply",c){if(!valid(a.text.toString(),b.text.toString()))error() else {p.edit().putString("day_${i}_start",a.text.toString()).putString("day_${i}_end",b.text.toString()).putString("day_${i}_msg",m.text.toString()).apply();render()}} }
 private fun editSpecial(holiday:Boolean){val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),0,dp(20),dp(10))};if(holiday){val date=field(c,"Holiday date (YYYY-MM-DD)",p.getString("holiday_date",LocalDate.now().toString())!!);val msg=field(c,"Holiday SMS",p.getString("holiday_msg","Dear Customer, our office is closed today for a holiday. Please contact us on the next working day. Thank you.")!!,3);dialog("Special holiday",c){if(runCatching{LocalDate.parse(date.text.toString())}.isFailure)Toast.makeText(this,"Enter a valid YYYY-MM-DD date",Toast.LENGTH_LONG).show() else {p.edit().putString("holiday_date",date.text.toString()).putString("holiday_msg",msg.text.toString()).apply();render()}}}else{val a=field(c,"Lunch starts (HH:mm)",p.getString("start","13:00")!!);val b=field(c,"Lunch ends (HH:mm)",p.getString("end","14:30")!!);val m=field(c,"Lunch SMS",p.getString("friday_msg","Dear Customer, we are on our Friday lunch break. Please call after 2:30 PM. Thank you.")!!,3);dialog("Friday lunch",c){if(!valid(a.text.toString(),b.text.toString()))error() else{p.edit().putString("start",a.text.toString()).putString("end",b.text.toString()).putString("friday_msg",m.text.toString()).apply();render()}}} }
 private fun chooseSim(){if(checkSelfPermission(Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){grant();return};try{val manager=getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager;val subs=manager.activeSubscriptionInfoList.orEmpty();val labels=mutableListOf("Default SMS SIM");subs.forEach{labels.add("SIM ${it.simSlotIndex+1} · ${it.carrierName}")};AlertDialog.Builder(this).setTitle("Send SMS using").setSingleChoiceItems(labels.toTypedArray(),-1){d,which->p.edit().putInt("sim",if(which==0)-1 else subs[which-1].subscriptionId).apply();d.dismiss();Toast.makeText(this,"SMS SIM saved",Toast.LENGTH_SHORT).show()}.setNegativeButton("Cancel",null).show()}catch(e:Exception){Toast.makeText(this,"SIM selection unavailable: ${e.message}",Toast.LENGTH_LONG).show()}}
 private fun grant(){val needed=arrayOf(Manifest.permission.READ_PHONE_STATE,Manifest.permission.READ_CALL_LOG,Manifest.permission.SEND_SMS,Manifest.permission.READ_PHONE_NUMBERS).filter{checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED};if(needed.isEmpty())Toast.makeText(this,"Required permissions already granted",Toast.LENGTH_SHORT).show() else requestPermissions(needed.toTypedArray(),10)}
}

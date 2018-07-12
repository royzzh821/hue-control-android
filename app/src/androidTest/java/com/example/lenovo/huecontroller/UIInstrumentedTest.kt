package com.example.lenovo.huecontroller

import android.support.test.annotation.UiThreadTest
import android.support.test.rule.ActivityTestRule
import android.widget.Button
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UIInstrumentedTest {

  @Rule
  @JvmField
  val activityRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java)

  @Test
  @UiThreadTest
  fun testLightTurnedOnRemotely() {
    activityRule.activity.receiveMessage("1")
    assertFalse(activityRule.activity.findViewById<Button>(R.id.HueOn).isEnabled)
    assertTrue(activityRule.activity.findViewById<Button>(R.id.HueOff).isEnabled)
  }

  @Test
  @UiThreadTest
  fun testLightTurnedOffRemotely() {
    activityRule.activity.receiveMessage("0")
    assertTrue(activityRule.activity.findViewById<Button>(R.id.HueOn).isEnabled)
    assertFalse(activityRule.activity.findViewById<Button>(R.id.HueOff).isEnabled)
  }

  @Test
  @UiThreadTest
  fun testLightTurnedOnOffRemotely() {
    activityRule.activity.receiveMessage("1")
    activityRule.activity.receiveMessage("0")
    assertTrue(activityRule.activity.findViewById<Button>(R.id.HueOn).isEnabled)
    assertFalse(activityRule.activity.findViewById<Button>(R.id.HueOff).isEnabled)
  }

  @Test
  @UiThreadTest
  fun testLightTurnedOffOnRemotely() {
    activityRule.activity.receiveMessage("0")
    activityRule.activity.receiveMessage("1")
    assertFalse(activityRule.activity.findViewById<Button>(R.id.HueOn).isEnabled)
    assertTrue(activityRule.activity.findViewById<Button>(R.id.HueOff).isEnabled)
  }
}
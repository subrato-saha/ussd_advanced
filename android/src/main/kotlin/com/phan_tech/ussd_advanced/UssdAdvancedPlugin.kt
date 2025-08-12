/*
 * Copyright (c) 2020. BoostTag E.I.R.L. Romell D.Z.
 * All rights reserved
 * porfile.romellfudi.com
 */
package com.phan_tech.ussd_advanced;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import io.flutter.Log;


/**
 * AccessibilityService object for ussd dialogs on Android mobile Telcoms
 *
 * @author Romell Dominguez
 * @version 1.1.c 27/09/2018
 * @since 1.0.a
 */
public class USSDServiceKT extends AccessibilityService {

    private static AccessibilityEvent event;

    /**
     * Catch widget by Accessibility, when is showing at mobile display
     *
     * @param event AccessibilityEvent
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        USSDServiceKT.event = event;
        USSDController ussd = USSDController.INSTANCE;
//        Timber.d(String.format(
//                "onAccessibilityEvent: [type] %s [class] %s [package] %s [time] %s [text] %s",
//                event.getEventType(), event.getClassName(), event.getPackageName(),
//                event.getEventTime(), event.getText()));
        if (!ussd.isRunning()) {
            return;
        }
        String response = null;
        if(!event.getText().isEmpty()) {
            List<CharSequence> res = event.getText();
            res.remove("SEND");
            res.remove("CANCEL");
            response = String.join("\n", res );
        }
        if (LoginView(event) && notInputText(event)) {
            // first view or logView, do nothing, pass / FIRST MESSAGE
            clickOnButton(event, 0);
            ussd.stopRunning();
            USSDController.callbackInvoke.over(response != null ? response : "");
        } else if (problemView(event) || LoginView(event)) {
            // deal down
            clickOnButton(event, 1);
            USSDController.callbackInvoke.over(response != null ? response : "");
        } else if (isUSSDWidget(event)) {
//            Timber.d("catch a USSD widget/Window");
            if (notInputText(event)) {
                // not more input panels / LAST MESSAGE
                // sent 'OK' button
//                Timber.d("No inputText found & closing USSD process");
                clickOnButton(event, 0);
                ussd.stopRunning();
                USSDController.callbackInvoke.over(response != null ? response : "");
            } else {
                // sent option 1
                if (ussd.getSendType() == true)
                    ussd.getCallbackMessage().invoke(event);
                else USSDController.callbackInvoke.responseInvoke(event);
            }
        }

    }

    /**
     * Send whatever you want via USSD
     *
     * @param text any string
     */
    public static void send(String text) {
        setTextIntoField(event, text);
        clickOnButton(event, 1);
    }
    public static void send2(String text, AccessibilityEvent ev) {
        setTextIntoField(ev, text);
        clickOnButton(ev, 1);
    }

    /**
     * Dismiss dialog by using first button from USSD Dialog
     */
    public static void cancel() {
        clickOnButton(event, 0);
    }
    public static void cancel2(AccessibilityEvent ev) {
        clickOnButton(ev, 0);
    }

    /**
     * set text into input text at USSD widget
     *
     * @param event AccessibilityEvent
     * @param data  Any String
     */
    private static void setTextIntoField(AccessibilityEvent event, String data) {
        Bundle arguments = new Bundle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, data);
        }
        for (AccessibilityNodeInfo leaf : getLeaves(event)) {
            if (leaf.getClassName().equals("android.widget.EditText")
                    && !leaf.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                ClipboardManager clipboardManager = ((ClipboardManager)  USSDController
                        .INSTANCE.getContext().getSystemService(Context.CLIPBOARD_SERVICE));
                if (clipboardManager != null) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("text", data));
                }
                leaf.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
        }
    }

    /**
     * Method evaluate if USSD widget has input text
     *
     * @param event AccessibilityEvent
     * @return boolean has or not input text
     */
    protected static boolean notInputText(AccessibilityEvent event) {
        for (AccessibilityNodeInfo leaf : getLeaves(event))
            if (leaf.getClassName().equals("android.widget.EditText")) return false;
        return true;
    }

    /**
     * The AccessibilityEvent is instance of USSD Widget class
     *
     * @param event AccessibilityEvent
     * @return boolean AccessibilityEvent is USSD
     */
    private boolean isUSSDWidget(AccessibilityEvent event) {
        return (event.getClassName().equals("amigo.app.AmigoAlertDialog")
                || event.getClassName().equals("android.app.AlertDialog")
                || event.getClassName().equals("com.android.phone.oppo.settings.LocalAlertDialog")
                || event.getClassName().equals("com.zte.mifavor.widget.AlertDialog")
                || event.getClassName().equals("color.support.v7.app.AlertDialog"));
    }

    /**
     * The View has a login message into USSD Widget
     *
     * @param event AccessibilityEvent
     * @return boolean USSD Widget has login message
     */
    private boolean LoginView(AccessibilityEvent event) {
        return isUSSDWidget(event)
                && USSDController.INSTANCE.getMap().get(USSDController.KEY_LOGIN)
                .contains(event.getText().get(0).toString());
    }

    /**
     * The View has a problem message into USSD Widget
     *
     * @param event AccessibilityEvent
     * @return boolean USSD Widget has problem message
     */
    protected boolean problemView(AccessibilityEvent event) {
        return isUSSDWidget(event)
                && USSDController.INSTANCE.getMap().get(USSDController.KEY_ERROR)
                .contains(event.getText().get(0).toString());
    }

    /**
     * click a button using the index
     *
     * @param event AccessibilityEvent
     * @param index button's index
     */
    protected static void clickOnButton(AccessibilityEvent event, int index) {
    List<AccessibilityNodeInfo> leaves = getLeaves(event);

    for (AccessibilityNodeInfo leaf : leaves) {
        CharSequence text = leaf.getText();

        if (text != null && text.toString().trim().equalsIgnoreCase("send")) {
            // Try clicking the node itself
            if (leaf.isClickable()) {
                leaf.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            // Fallback: try its parent
            else if (leaf.getParent() != null && leaf.getParent().isClickable()) {
                leaf.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            break; // stop after first match
                }
            }
        }


    private static List<AccessibilityNodeInfo> getLeaves(AccessibilityEvent event) {
        List<AccessibilityNodeInfo> leaves = new ArrayList<>();
        if (event != null && event.getSource() != null) {
            getLeaves(leaves, event.getSource());
        }
        return leaves;
    }

    private static void getLeaves(List<AccessibilityNodeInfo> leaves, AccessibilityNodeInfo node) {
        if (node.getChildCount() == 0) {
            leaves.add(node);
            return;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            getLeaves(leaves, node.getChild(i));
        }
    }

    /**
     * Active when SO interrupt the application
     */
    @Override
    public void onInterrupt() {
//        Timber.d( "onInterrupt");
    }

    /**
     * Configure accessibility server from Android Operative System
     */
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
//        Timber.d("onServiceConnected");
    }
}

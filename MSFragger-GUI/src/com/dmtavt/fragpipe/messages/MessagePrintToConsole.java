package com.dmtavt.fragpipe.messages;

import com.dmtavt.fragpipe.api.Bus;
import java.awt.Color;

public class MessagePrintToConsole {

  public final Color color;
  public final String text;
  public final boolean addNewline;

  public MessagePrintToConsole(String text) {
    this(Color.BLACK, text, true);
  }

  public MessagePrintToConsole(String text, boolean addNewline) {
    this(Color.BLACK, text, addNewline);
  }

  public MessagePrintToConsole(Color color, String text, boolean addNewline) {
    this.color = color;
    this.text = text;
    this.addNewline = addNewline;
  }

  public static void toConsole(String s) {
    Bus.post(new MessagePrintToConsole(s));
  }

  public static void toConsole(String text, boolean addNewline) {
    Bus.post(new MessagePrintToConsole(text, addNewline));
  }

  public static void toConsole(Color color, String text, boolean addNewline) {
    Bus.post(new MessagePrintToConsole(color,text, addNewline));
  }
}

package com.dmtavt.fragpipe.messages;

import com.dmtavt.fragpipe.api.PyInfo;

public class NotePythonConfig implements INoteConfig {
  public final PyInfo pi;
  public final Throwable ex;
  public final String command;
  public final String version;


  public NotePythonConfig(PyInfo pi) {
    this(pi, null, pi.getCommand(), pi.getVersion());
  }

  public NotePythonConfig(PyInfo pi, Throwable ex, String command, String version) {
    this.pi = pi;
    this.ex = ex;
    this.command = command;
    this.version = version;
  }


  @Override
  public boolean isValid() {
    return pi != null && ex == null;
  }
}

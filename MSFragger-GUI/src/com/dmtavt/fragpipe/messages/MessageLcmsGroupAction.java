package com.dmtavt.fragpipe.messages;

public class MessageLcmsGroupAction {

  public MessageLcmsGroupAction(Type type) {
    this.type = type;
  }

  public enum Type {
    CONSECUTIVE, BY_PARENT_DIR, BY_FILE_NAME, SET_EXP, SET_REP, SET_DDA, SET_DIA, SET_GPF_DIA, SET_DIA_QUANT, CLEAR_GROUPS
  }
  public final Type type;
}

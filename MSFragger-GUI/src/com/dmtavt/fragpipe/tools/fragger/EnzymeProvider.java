package com.dmtavt.fragpipe.tools.fragger;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.github.chhh.utils.PropertiesUtils;

public class EnzymeProvider implements Supplier<List<MsfraggerEnzyme>> {
  private static final String PROPERTIES_FN = "enzyme-defs-supported.txt";

  @Override
  public List<MsfraggerEnzyme> get() {
    final Properties props = PropertiesUtils.initProperties(PROPERTIES_FN, EnzymeProvider.class);
    if (props == null || props.isEmpty()) {
      throw new IllegalStateException("Could not load " + PROPERTIES_FN + " for MSFragger supported enzymes");
    }
    final Pattern reCut = Pattern.compile("cut\\(([a-zA-Z-]+)\\)");
    final Pattern reNocuts = Pattern.compile("nocuts\\(([a-zA-Z-]+)\\)");
    final Pattern reSense = Pattern.compile("sense\\(([a-zA-Z-]+)\\)");
    List<MsfraggerEnzyme> list = props.stringPropertyNames().stream().sorted().map(name -> {
      String val = props.getProperty(name);
      String cut = "";
      Matcher mCut = reCut.matcher(val);
      if (mCut.find()) {
        cut = mCut.group(1).trim().toUpperCase();
      }
      String nocuts = "";
      Matcher mNocuts = reNocuts.matcher(val);
      if (mNocuts.find()) {
        nocuts = mNocuts.group(1).trim().toUpperCase();
      }
      Matcher mSense = reSense.matcher(val);
      final String sense = mSense.find() ? mSense.group(1).trim().toUpperCase() : null;
      return new MsfraggerEnzyme(name.trim(), cut, nocuts, sense);

    }).collect(Collectors.toList());

    return list;
  }
}

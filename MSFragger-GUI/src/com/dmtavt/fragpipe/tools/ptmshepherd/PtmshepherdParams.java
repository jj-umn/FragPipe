package com.dmtavt.fragpipe.tools.ptmshepherd;

import com.github.chhh.utils.StringUtils;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.dmtavt.fragpipe.api.LcmsFileGroup;
import com.github.chhh.utils.PropertiesUtils;

public class PtmshepherdParams {
  private static final Logger log = LoggerFactory.getLogger(PtmshepherdParams.class);
  public static final String DEFAULT_PROPERTIES_FN = "shepherd_default_open.config";
  public static final String DEFAULT_PROPERTIES_FN_BASE = "shepherd_default_";
  public static final String DEFAULT_PROPERTIES_FN_EXT = ".config";
  public static final String PROP_DATABASE = "database";
  /**
   * Dataset string format is `dataset = (set-name) (path-to-psm.tsv) (path-to-mzml-folder)`
   */
  public static final String PROP_DATASET = "dataset";
  public static final String DEFAULT_DATASET_NAME = "dataset01";

  private Path workDir;
  private Path db;
  private Map<LcmsFileGroup, Path> groups;
  private Map<String, String> props;

  public PtmshepherdParams(Path workDir, Path db, Map<LcmsFileGroup, Path> groups) {
    this.workDir = workDir;
    this.db = db;
    this.groups = groups;
  }

  public PtmshepherdParams(Path workDir, Path db, Map<LcmsFileGroup, Path> groups, Map<String, String> additionalProperties) {
    this(workDir, db, groups);
    Properties defaults = PropertiesUtils
        .loadPropertiesLocal(PtmshepherdParams.class, DEFAULT_PROPERTIES_FN);
    props = new HashMap<>(PropertiesUtils.toMap(defaults));

    props.putAll(additionalProperties);
  }

  public static String configFn(String searchTypeSuffix) {
    return DEFAULT_PROPERTIES_FN_BASE + searchTypeSuffix + DEFAULT_PROPERTIES_FN_EXT;
  }

  public String createConfig() {
    StringBuilder sb = new StringBuilder();
    sb.append(PROP_DATABASE).append(" = ").append(db.toAbsolutePath().normalize().toString()).append("\n");
    for (Entry<LcmsFileGroup, Path> e : groups.entrySet()) {
      LcmsFileGroup g = e.getKey();
      List<Path> lcmsPathsForGroup = g.lcmsFiles.stream().map(inputLcmsFile -> inputLcmsFile
          .getPath().getParent())
          .distinct().collect(Collectors.toList());
      if (lcmsPathsForGroup.size() != 1) {
        String msg = "PTM Shepherd config only works when LCMS files in a group are in one directory.";
        log.error(msg);
        throw new IllegalArgumentException(msg);
      }
      Path lcmsFilesDir = lcmsPathsForGroup.get(0);
      Path p = e.getValue();
      Path psmTsv = g.outputDir(workDir).resolve("psm.tsv");
      final String datasetName = StringUtils.isNullOrWhitespace(g.name) ? DEFAULT_DATASET_NAME : g.name;
      sb.append(PROP_DATASET).append(" = ")
          .append(datasetName).append(" ")
          .append(psmTsv.toString()).append(" ")
          .append(lcmsFilesDir.toString()).append("\n");
    }

    if (props != null && !props.isEmpty()) {
      sb.append("\n");
      props.entrySet().stream()
          .filter(e -> !e.getKey().startsWith("ui."))
          .sorted(Comparator.comparing(Entry::getKey))
          .forEach(e -> sb.append(e.getKey()).append(" = ").append(e.getValue()).append("\n"));
    }

    return sb.toString();
  }
}

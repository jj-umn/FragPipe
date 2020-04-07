package com.dmtavt.fragpipe.tabs;

import com.dmtavt.fragpipe.Fragpipe;
import com.dmtavt.fragpipe.api.Bus;
import com.dmtavt.fragpipe.api.ModsTable;
import com.dmtavt.fragpipe.messages.MessageMsfraggerParamsUpdate;
import com.dmtavt.fragpipe.messages.MessagePrecursorSelectionMode;
import com.dmtavt.fragpipe.messages.MessageRun;
import com.dmtavt.fragpipe.messages.MessageSaveCache;
import com.dmtavt.fragpipe.messages.MessageSearchType;
import com.dmtavt.fragpipe.messages.MessageValidityMassCalibration;
import com.dmtavt.fragpipe.messages.MessageValidityMsadjuster;
import com.dmtavt.fragpipe.messages.NoteConfigMsfragger;
import com.github.chhh.utils.CacheUtils;
import com.github.chhh.utils.PropertiesUtils;
import com.github.chhh.utils.StringUtils;
import com.github.chhh.utils.SwingUtils;
import com.github.chhh.utils.swing.DocumentFilters;
import com.github.chhh.utils.swing.FileChooserUtils;
import com.github.chhh.utils.swing.FileChooserUtils.FcMode;
import com.github.chhh.utils.swing.FormEntry;
import com.github.chhh.utils.swing.JPanelWithEnablement;
import com.github.chhh.utils.swing.MigUtils;
import com.github.chhh.utils.swing.StringRepresentable;
import com.github.chhh.utils.swing.UiCheck;
import com.github.chhh.utils.swing.UiCombo;
import com.github.chhh.utils.swing.UiSpinnerDouble;
import com.github.chhh.utils.swing.UiSpinnerInt;
import com.github.chhh.utils.swing.UiText;
import com.github.chhh.utils.swing.UiUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableCellEditor;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.greenrobot.eventbus.Subscribe;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import umich.msfragger.gui.ModificationsTableModel;
import umich.msfragger.gui.api.SearchTypeProp;
import umich.msfragger.gui.renderers.TableCellDoubleRenderer;
import umich.msfragger.gui.renderers.TableCellIntRenderer;
import umich.msfragger.gui.renderers.TableCellIntSpinnerEditor;
import umich.msfragger.params.Props.Prop;
import umich.msfragger.params.ThisAppProps;
import umich.msfragger.params.dbslice.DbSlice;
import umich.msfragger.params.enums.CleavageType;
import umich.msfragger.params.enums.FraggerOutputType;
import umich.msfragger.params.enums.FraggerPrecursorMassMode;
import umich.msfragger.params.enums.FragmentMassTolUnits;
import umich.msfragger.params.enums.IntensityTransform;
import umich.msfragger.params.enums.PrecursorMassTolUnits;
import umich.msfragger.params.enums.RemovePrecursorPeak;
import umich.msfragger.params.fragger.EnzymeProvider;
import umich.msfragger.params.fragger.Mod;
import umich.msfragger.params.fragger.MsfraggerEnzyme;
import umich.msfragger.params.fragger.MsfraggerParams;
import umich.msfragger.params.fragger.MsfraggerProps;

public class TabMsfragger extends JPanelWithEnablement {
  private static final Logger log = LoggerFactory.getLogger(TabMsfragger.class);
  private final static MigUtils mu = MigUtils.get();
  public static final String PROP_FILECHOOSER_LAST_PATH = "msfragger.filechooser.path";
  public static final String CACHE_FORM = "msfragger-form" + ThisAppProps.TEMP_FILE_EXT;
  public static final String CACHE_PROPS = "msfragger-props" + ThisAppProps.TEMP_FILE_EXT;
  public static final String[] SEARCH_TYPE_DROPDOWN_NAMES = {"Closed Search", "Open Search", "Non-specific Search", "Mass Offset Search"};
  public static final Map<String, SearchTypeProp> SEARCH_TYPE_NAME_MAPPING;
  private static final String[] TABLE_VAR_MODS_COL_NAMES = {"Enabled", "Site (editable)",
      "Mass Delta (editable)", "Max occurrences (editable)"};
  private static final String[] TABLE_FIX_MODS_COL_NAMES = {"Enabled", "Site",
      "Mass Delta (editable)"};
  private static final String PROP_misc_adjust_precurosr_mass = "misc.adjust-precursor-mass";
  private static final String PROP_misc_slice_db = "misc.slice-db";
  private static final String PROP_misc_ram = "misc.ram";
  private static final String PROP_misc_fragger_remove_precursor_range_lo = "misc.fragger.remove-precursor-range-lo";
  private static final String PROP_misc_fragger_remove_precursor_range_hi = "misc.fragger.remove-precursor-range-hi";
  private static final String PROP_misc_fragger_digest_mass_lo = "misc.fragger.digest-mass-lo";
  private static final String PROP_misc_fragger_digest_mass_hi = "misc.fragger.digest-mass-hi";
  private static final String PROP_misc_fragger_clear_mz_lo = "misc.fragger.clear-mz-lo";
  private static final String PROP_misc_fragger_clear_mz_hi = "misc.fragger.clear-mz-hi";
  private static final String PROP_misc_fragger_precursor_charge_lo = "misc.fragger.precursor-charge-lo";
  private static final String PROP_misc_fragger_precursor_charge_hi = "misc.fragger.precursor-charge-hi";
  private static final String PROP_misc_fragger_enzyme_dropdown = "misc.fragger.enzyme-dropdown";
  private static final String TAB_PREFIX = "msfragger.";
  private static final Set<String> PROPS_MISC_NAMES;
  private static final Map<String, Function<String, String>> CONVERT_TO_FILE;
  private static final Map<String, Function<String, String>> CONVERT_TO_GUI;
  private static final String CALIBRATE_VALUE_OFF = "Off";
  private static final String[] CALIBRATE_LABELS = {CALIBRATE_VALUE_OFF, "On", "On and find optimal parameters"};
  private static final List<MsfraggerEnzyme> ENZYMES = new EnzymeProvider().get();
  //public static FileNameExtensionFilter fnExtFilter = new FileNameExtensionFilter("LCMS files (mzML/mzXML/mgf/raw/d)", "mzml", "mzxml", "mgf", "raw", "d");
  private static String[] PROPS_MISC = {
      PROP_misc_adjust_precurosr_mass,
      PROP_misc_slice_db,
      PROP_misc_ram,
      PROP_misc_fragger_digest_mass_lo,
      PROP_misc_fragger_digest_mass_hi,
      PROP_misc_fragger_remove_precursor_range_lo,
      PROP_misc_fragger_remove_precursor_range_hi,
      PROP_misc_fragger_clear_mz_lo,
      PROP_misc_fragger_clear_mz_hi,
      PROP_misc_fragger_precursor_charge_lo,
      PROP_misc_fragger_precursor_charge_hi,
  };

  static {
    PROPS_MISC_NAMES = new HashSet<>(Arrays.asList(PROPS_MISC));
    CONVERT_TO_FILE = new HashMap<>();
    CONVERT_TO_GUI = new HashMap<>();
    SEARCH_TYPE_NAME_MAPPING = new HashMap<>();

    CONVERT_TO_FILE.put(MsfraggerParams.PROP_precursor_mass_units, s -> Integer.toString(
        PrecursorMassTolUnits.valueOf(s).valueInParamsFile()));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_fragment_mass_units, s -> Integer.toString(
        FragmentMassTolUnits.valueOf(s).valueInParamsFile()));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_precursor_true_units, s -> Integer.toString(FragmentMassTolUnits.valueOf(s).valueInParamsFile()));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_calibrate_mass, s -> Integer.toString(Arrays.asList(CALIBRATE_LABELS).indexOf(s)));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_num_enzyme_termini, s -> Integer.toString(
        CleavageType.valueOf(s).valueInParamsFile()));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_remove_precursor_peak, s -> Integer.toString(
        RemovePrecursorPeak.get(s)));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_intensity_transform, s -> Integer.toString(
        IntensityTransform.get(s)));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_localize_delta_mass, s -> Integer.toString(Boolean.parseBoolean(s) ? 1 : 0));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_clip_nTerm_M, s -> Integer.toString(Boolean.parseBoolean(s) ? 1 : 0));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_allow_multiple_variable_mods_on_residue, s -> Integer.toString(Boolean.parseBoolean(s) ? 1 : 0));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_override_charge, s -> Integer.toString(Boolean.parseBoolean(s) ? 1 : 0));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_output_format, s -> FraggerOutputType.valueOf(s).valueInParamsFile());
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_report_alternative_proteins, s -> Integer.toString(Boolean.parseBoolean(s) ? 1 : 0));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_fragment_ion_series, ionStr -> ionStr.trim().replaceAll("[\\s,;]+",","));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_ion_series_definitions, defStr -> defStr.trim().replaceAll("\\s*[,;]+\\s*",", "));
    CONVERT_TO_FILE.put(MsfraggerParams.PROP_mass_offsets, defStr -> {
      String content;
      if (!defStr.contains("<html")) {
        content = defStr;
      } else {
        content = Jsoup.parse(defStr).body().text();
      }
      return content.replaceAll("[\n\r]+", " ");
    });


    CONVERT_TO_GUI.put(MsfraggerParams.PROP_precursor_mass_units, s -> PrecursorMassTolUnits.fromParamsFileRepresentation(s).name());
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_fragment_mass_units, s -> FragmentMassTolUnits.fromParamsFileRepresentation(s).name());
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_precursor_true_units, s -> FragmentMassTolUnits.fromParamsFileRepresentation(s).name());
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_calibrate_mass, s -> CALIBRATE_LABELS[Integer.parseInt(s)]);
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_num_enzyme_termini, s -> CleavageType.fromValueInParamsFile(s).name());
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_remove_precursor_peak, s -> RemovePrecursorPeak.get(Integer.parseInt(s)));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_intensity_transform, s -> IntensityTransform.get(Integer.parseInt(s)));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_localize_delta_mass, s -> Boolean.toString(Integer.parseInt(s) > 0));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_clip_nTerm_M, s -> Boolean.toString(Integer.parseInt(s) > 0));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_allow_multiple_variable_mods_on_residue, s -> Boolean.toString(Integer.parseInt(s) > 0));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_override_charge, s -> Boolean.toString(Integer.parseInt(s) > 0));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_output_format, s -> FraggerOutputType.fromValueInParamsFile(s).name());
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_report_alternative_proteins, s -> Boolean.toString(Integer.parseInt(s) > 0));
    CONVERT_TO_GUI.put(MsfraggerParams.PROP_mass_offsets, SwingUtils::wrapInStyledHtml);

    //{"Closed Search", "Open Search", "Non-specific Search", "Mass Offset Search"}
    SEARCH_TYPE_NAME_MAPPING.put("Closed Search", SearchTypeProp.closed);
    SEARCH_TYPE_NAME_MAPPING.put("Open Search", SearchTypeProp.open);
    SEARCH_TYPE_NAME_MAPPING.put("Non-specific Search", SearchTypeProp.nonspecific);
    SEARCH_TYPE_NAME_MAPPING.put("Mass Offset Search", SearchTypeProp.offset);
  }

  private ImageIcon icon;
  private UiCheck checkRun;
  private JScrollPane scroll;
  private JPanel pContent;
  private ModsTable tableVarMods;
  private ModsTable tableFixMods;
  private UiSpinnerInt uiSpinnerRam;
  private UiSpinnerInt uiSpinnerThreads;
  private UiCombo uiComboMassCalibrate;
  private UiCombo uiComboOutputType;
  private UiCombo uiComboMassMode;
  private UiSpinnerInt uiSpinnerDbslice;
  private UiCheck uiCheckShiftedIons;
  private UiText uiTextCustomIonSeries;
  private JLabel labelCustomIonSeries;
  private Map<Component, Boolean> enablementMapping = new HashMap<>();
  private UiCombo uiComboEnzymes;
  private UiText uiTextCuts;
  private UiText uiTextNocuts;
  private UiText uiTextEnzymeName;
  private UiCombo uiComboCleavage;
  private UiCombo uiComboLoadDefaultsNames;
  private UiText uiTextMassOffsets;
  private UiSpinnerDouble uiSpinnerPrecTolLo;
  private UiSpinnerDouble uiSpinnerPrecTolHi;
  private UiCombo uiComboPrecursorTolUnits;
  private final Map<String, String> cache = new HashMap<>();
  private UiText uiTextIsoErr;
  private JEditorPane epMassOffsets;


  public TabMsfragger() {
    init();
    initMore();
  }

  public UiText getUiTextIsoErr() {
    return uiTextIsoErr;
  }



  private static void actionChangeMassMode(ItemEvent e) {
    if (e.getStateChange() == ItemEvent.SELECTED) {

      final Object item = e.getItem();
      if (!(item instanceof String)) {
        return;
      }
      try {
        FraggerPrecursorMassMode mode = FraggerPrecursorMassMode.valueOf((String) item);
        Bus.post(new MessagePrecursorSelectionMode(mode));

      } catch (IllegalArgumentException ex) {
        log.debug("Value [{}] not in FraggerPrecursorMassMode enum", item);
      }
    }
  }

  private void initMore() {
    // TODO: ACHTUNG: temporary fix, disabling "Define custom ion series field"
    // Remove when custom ion series work properly in msfragger
    updateEnabledStatus(uiTextCustomIonSeries, false);
    updateEnabledStatus(labelCustomIonSeries, false);

    // register on the bus only after all the components have been created to avoid NPEs
    Bus.register(this);
  }

  private void init() {
    icon = new ImageIcon(
        getClass().getResource("/umich/msfragger/gui/icons/bolt-16.png"));

    this.setLayout(new MigLayout(new LC().fillX()));

    JPanel pTop = createPanelTop();
    JPanel pContent = createPanelContent();
    JPanel pBasic = createPanelBasicOptions();
    JPanel pMods = createPanelMods();
    JPanel pAdvanced = createPanelAdvancedOptions();

    mu.add(this, pTop).growX().wrap();
    mu.add(this, pContent).growX().wrap();
    mu.add(pContent, pBasic).growX().wrap();
    mu.add(pContent, pMods).growX().wrap();
    mu.add(pContent, pAdvanced).growX().wrap();
  }

  /** Top panel with checkbox, buttons and RAM+Threads spinners. */
  private JPanel createPanelTop() {
    JPanel pTop = new JPanel(new MigLayout(new LC()));

    checkRun = new UiCheck("Run MSFragger", null, true);
    checkRun.setName("misc.is-run");
    checkRun.addActionListener(e -> {
      final boolean isSelected = checkRun.isSelected();
      updateEnabledStatus(pContent, isSelected);
    });

    uiComboLoadDefaultsNames = UiUtils.createUiCombo(SEARCH_TYPE_DROPDOWN_NAMES);
    JButton btnLoadDefaults = new JButton("Load");
    btnLoadDefaults.addActionListener(this::onClickLoadDefaults);

    pTop.add(checkRun, new CC());
    pTop.add(new JLabel("Load default parameters for:"), new CC().gapLeft("15px"));
    pTop.add(uiComboLoadDefaultsNames, new CC().gapLeft("1px"));
    pTop.add(btnLoadDefaults, new CC().wrap());

    JButton save = new JButton("Save Parameters");
    save.addActionListener(this::onClickSave);
    JButton load = new JButton("Load Parameters");
    load.addActionListener(this::onClickLoad);

    uiSpinnerRam = new UiSpinnerInt(0, 0, 1024, 1, 3);
    FormEntry feRam = fe(PROP_misc_ram, uiSpinnerRam).label("RAM (GB)").create();
    uiSpinnerThreads = new UiSpinnerInt(Runtime.getRuntime().availableProcessors() - 1, 0, 128, 1);
    FormEntry feThreads = fe(MsfraggerParams.PROP_num_threads,
        uiSpinnerThreads).label("Threads").create();

    pTop.add(save, new CC().split(6).spanX());
    pTop.add(load, new CC());
    pTop.add(feRam.label(), new CC());
    pTop.add(feRam.comp, new CC());
    pTop.add(feThreads.label(), new CC());
    pTop.add(feThreads.comp, new CC());

    this.add(pTop, BorderLayout.NORTH);
    return pTop;
  }

  private JPanel createPanelContent() {
    pContent = new JPanel();
    pContent.setLayout(new MigLayout(new LC().fillX()));
    return pContent;
  }

  /** Panel with all the basic options. */
  private JPanel createPanelBasicOptions() {

      DecimalFormat df1 = new DecimalFormat("0.#");
      JPanel pBase = new JPanel(new MigLayout(new LC().fillX()));
      pBase.setBorder(
          new TitledBorder("Common Options (Advanced Options are at the end of the page)"));

      JPanel pPeakMatch = new JPanel(new MigLayout(new LC()));
      pPeakMatch.setBorder(new TitledBorder("Peak Matching"));

      // precursor mass tolerance
      uiComboPrecursorTolUnits = UiUtils.createUiCombo(PrecursorMassTolUnits.values());
      FormEntry fePrecTolUnits = fe(MsfraggerParams.PROP_precursor_mass_units, uiComboPrecursorTolUnits).label("Precursor mass tolerance").create();
      uiSpinnerPrecTolLo = new UiSpinnerDouble(-10, -10000, 10000, 1,
          new DecimalFormat("0.#"));
      uiSpinnerPrecTolLo.setColumns(4);
      FormEntry feSpinnerPrecTolLo = fe(MsfraggerParams.PROP_precursor_mass_lower, uiSpinnerPrecTolLo).create();
      uiSpinnerPrecTolHi = new UiSpinnerDouble(+10, -10000, 10000, 1,
          new DecimalFormat("0.#"));
      uiSpinnerPrecTolHi.setColumns(4);
      FormEntry feSpinnerPrecTolHi = fe(MsfraggerParams.PROP_precursor_mass_upper, uiSpinnerPrecTolHi).create();
      FormEntry feDeisotope = fe(MsfraggerParams.PROP_deisotope, new UiSpinnerInt(1, 0, 2, 1, 4))
          .label("Deisotope")
          .tooltip("<html>0 = deisotoping off<br/>\n1 = deisotoping on").create();

      uiComboPrecursorTolUnits.addItemListener(e -> {
        Object selected = uiComboPrecursorTolUnits.getSelectedItem();
        if (selected == null || StringUtils.isNullOrWhitespace((String)selected))
          return;
        final boolean isDisabled = PrecursorMassTolUnits.valueOf((String)selected).valueInParamsFile() > 1;
        uiSpinnerPrecTolLo.setEnabled(!isDisabled);
        uiSpinnerPrecTolHi.setEnabled(!isDisabled);

        // treat calibrate masses dropdown
        boolean wasEnabled = uiComboMassCalibrate.isEnabled();
        if (wasEnabled && isDisabled) { //  switching from enabled to disabled
          String oldVal = (String)uiComboMassCalibrate.getSelectedItem();
          if (oldVal != null) {
            cache.put(MsfraggerParams.PROP_calibrate_mass, oldVal);
          }
          uiComboMassCalibrate.setSelectedItem(CALIBRATE_VALUE_OFF);
          uiComboMassCalibrate.setEnabled(false);
        } else if (!wasEnabled && !isDisabled) { // switching from disabled to enabled
          String cachedVal = cache.get(MsfraggerParams.PROP_calibrate_mass);
          if (cachedVal != null) {
            uiComboMassCalibrate.setSelectedItem(cachedVal);
          }
          uiComboMassCalibrate.setEnabled(true);
        }
      });

      pPeakMatch.add(fePrecTolUnits.label(), new CC().alignX("right"));
      pPeakMatch.add(fePrecTolUnits.comp, new CC().split(4));
      pPeakMatch.add(feSpinnerPrecTolLo.comp);
      pPeakMatch.add(new JLabel("-"), new CC());
      pPeakMatch.add(feSpinnerPrecTolHi.comp, new CC());
      pPeakMatch.add(feDeisotope.label(), new CC().alignX("right"));
      pPeakMatch.add(feDeisotope.comp, new CC().wrap());

      // fragment mass tolerance
      FormEntry feFragTolUnits = fe(MsfraggerParams.PROP_fragment_mass_units, UiUtils.createUiCombo(FragmentMassTolUnits.values()))
          .label("Fragment mass tolerance").create();
      UiSpinnerDouble uiSpinnerFragTol = new UiSpinnerDouble(10, 0, 10000, 1,
          new DecimalFormat("0.###"));
      uiSpinnerFragTol.setColumns(4);
      FormEntry feFragTol = fe(MsfraggerParams.PROP_fragment_mass_tolerance, uiSpinnerFragTol).create();
      pPeakMatch.add(feFragTolUnits.label(), new CC().alignX("right"));
      pPeakMatch.add(feFragTolUnits.comp, new CC().split(2));
      pPeakMatch.add(feFragTol.comp, new CC().wrap());

      // mass calibrate
      uiComboMassCalibrate = UiUtils.createUiCombo(CALIBRATE_LABELS);
      String minFraggerVer = MsfraggerProps.getProperties().getProperty(MsfraggerProps.PROP_MIN_VERSION_FRAGGER_MASS_CALIBRATE, "201904");
      FormEntry feCalibrate = fe(MsfraggerParams.PROP_calibrate_mass, uiComboMassCalibrate)
          .label("<html>Calibrate masses")
          .tooltip(String.format("<html>Requires MSFragger %s+.", minFraggerVer)).create();
      pPeakMatch.add(feCalibrate.label(), new CC().alignX("right"));
      pPeakMatch.add(feCalibrate.comp, new CC());

      uiTextIsoErr = new UiText();
      uiTextIsoErr.setDocument(DocumentFilters.getFilter("[^\\d/-]+"));
      uiTextIsoErr.setText("-1/0/1/2");
      uiTextIsoErr.setColumns(10);
      FormEntry feIsotopeError = fe(MsfraggerParams.PROP_isotope_error, uiTextIsoErr)
          .label("Isotope error")
          .tooltip("<html>String of the form -1/0/1/2 indicating which isotopic\n"
              + "peak selection errors MSFragger will try to correct.")
          .create();

      pPeakMatch.add(feIsotopeError.label(), new CC().alignX("right"));
      pPeakMatch.add(feIsotopeError.comp, new CC().wrap());

      // Digest panel
      JPanel pDigest = new JPanel(new MigLayout(new LC()));
      pDigest.setBorder(new TitledBorder("Protein Digestion"));

      uiComboEnzymes = UiUtils
          .createUiCombo(ENZYMES.stream().map(msfe -> msfe.name)
              //.filter(name -> !"custom".equals(name))
              .collect(Collectors.toList()));
      Optional<MsfraggerEnzyme> trypsin = ENZYMES.stream()
          .filter(e -> e.name.toLowerCase().startsWith("trypsin"))
          .min(Comparator.comparing(o -> o.name));
      trypsin.ifPresent(msfraggerEnzyme -> uiComboEnzymes.setSelectedItem(msfraggerEnzyme.name));
      FormEntry feEnzymeList = fe(PROP_misc_fragger_enzyme_dropdown, uiComboEnzymes)
          .label("Load rules")
          .tooltip("<html>Load one of default definitions of enzyme cleavage rules.\n"
              + "You can still edit the name and rules manually after loading.").create();
      uiComboEnzymes.addItemListener(event -> {
        if (event.getStateChange() == ItemEvent.SELECTED) {
          Object item = event.getItem();
          log.debug("User selected enzyme: {}, of class {}", item, item.getClass());
          if (!(item instanceof String)) {
            throw new IllegalStateException("Ui Combo Boxes should just contain strings");
          }
          String name = (String) item;
          MsfraggerEnzyme enzyme = ENZYMES.stream()
              .filter(msfe -> msfe.name.equals(name)).findFirst()
              .orElseThrow(() -> new IllegalStateException(
                  "Enzymes list should have contained the name from dropdown"));
          uiTextEnzymeName.setText(enzyme.name);
          uiTextCuts.setText(enzyme.cut);
          uiTextNocuts.setText(enzyme.nocuts);
          uiComboCleavage.setSelectedItem("nonspecific".equals(item) ? CleavageType.NON_SPECIFIC.name() : CleavageType.ENZYMATIC.name());
        }
      });


      FocusAdapter enzymeSpecFocusListener = new FocusAdapter() {
        @Override
        public void focusLost(FocusEvent evt) {
          super.focusLost(evt);
          final String cuts = StringUtils.sortedChars(uiTextCuts.getNonGhostText());
          final String nocuts = StringUtils.sortedChars(uiTextNocuts.getNonGhostText());
          List<MsfraggerEnzyme> enzymes = ENZYMES.stream()
              .map(e -> new MsfraggerEnzyme(e.name, StringUtils.sortedChars(e.cut),
                  StringUtils.sortedChars(e.nocuts)))
              .collect(Collectors.toList());
          List<String> matching = enzymes.stream()
              .filter(e -> e.cut.equals(cuts) && e.nocuts.equals(nocuts))
              .map(e -> e.name).collect(Collectors.toList());
          log.warn("Found matching enzymes: {}", matching);
          if (matching.contains("nonspecific")) {
            trySelectEnzymeDropdown("nonspecific");
          } else if (!matching.isEmpty()) {
            trySelectEnzymeDropdown(matching.get(0));
          } else {
            trySelectEnzymeDropdown("custom");
          }
        }
      };

      uiTextEnzymeName = new UiText();
      FormEntry feEnzymeName = fe(MsfraggerParams.PROP_search_enzyme_name, uiTextEnzymeName).label("Enzyme name").create();
      uiTextCuts = UiUtils.uiTextBuilder().cols(6).filter("[^A-Z]").text("KR").create();
      uiTextCuts.addFocusListener(enzymeSpecFocusListener);
      FormEntry feCuts = fe(MsfraggerParams.PROP_search_enzyme_cutafter, uiTextCuts).label("Cut after")
          .tooltip("Capital letters for amino acids after which the enzyme cuts.").create();
      uiTextNocuts = UiUtils.uiTextBuilder().cols(6).filter("[^A-Z]").text("P").create();
      uiTextNocuts.addFocusListener(enzymeSpecFocusListener);
      FormEntry feNocuts = fe(MsfraggerParams.PROP_search_enzyme_butnotafter, uiTextNocuts).label("But not before")
          .tooltip("Amino acids before which the enzyme won't cut.").create();
      pDigest.add(feEnzymeList.label(), new CC().span(2).split(2).alignX("right"));
      pDigest.add(feEnzymeList.comp, new CC());
      pDigest.add(feEnzymeName.label(), new CC().alignX("right"));
      pDigest.add(feEnzymeName.comp, new CC().minWidth("120px").split().spanX());
      pDigest.add(feCuts.label(), new CC().gapLeft("5px"));
      pDigest.add(feCuts.comp);//, new CC().minWidth("45px"));
      pDigest.add(feNocuts.label());//, new CC().split(2).spanX().gapLeft("5px"));
      pDigest.add(feNocuts.comp, new CC().wrap());

      List<String> cleavageTypeNames = Arrays.stream(CleavageType.values()).map(Enum::name)
          .collect(Collectors.toList());
      uiComboCleavage = UiUtils.createUiCombo(cleavageTypeNames);
      FormEntry feCleavageType = fe(MsfraggerParams.PROP_num_enzyme_termini, uiComboCleavage).label("Cleavage").create();
      UiSpinnerInt uiSpinnerMissedCleavages = new UiSpinnerInt(1, 0, 1000, 1);
      uiSpinnerMissedCleavages.setColumns(6);
      FormEntry feMissedCleavages = fe(MsfraggerParams.PROP_allowed_missed_cleavage, uiSpinnerMissedCleavages).label("Missed cleavages").create();
      FormEntry feClipM = fe(MsfraggerParams.PROP_clip_nTerm_M, new UiCheck("Clip N-term M", null))
          .tooltip("Trim protein N-terminal Methionine as a variable modification").create();
      pDigest.add(feCleavageType.label(), new CC().alignX("right"));
      pDigest.add(feCleavageType.comp, new CC().minWidth("120px").growX());
      pDigest.add(feMissedCleavages.label(), new CC().alignX("right"));
      pDigest.add(feMissedCleavages.comp, new CC());
      pDigest.add(feClipM.comp, new CC().gapLeft("5px").wrap());

      FormEntry fePepLenMin = fe(MsfraggerParams.PROP_digest_min_length, new UiSpinnerInt(7, 0, 1000, 1, 3))
          .label("Peptide length").create();
      FormEntry fePepLenMax = fe(MsfraggerParams.PROP_digest_max_length, new UiSpinnerInt(50, 0, 1000, 1, 3))
          .create();
      UiSpinnerDouble uiSpinnerDigestMassLo = new UiSpinnerDouble(200, 0, 50000, 100,
          new DecimalFormat("0.#"));
      uiSpinnerDigestMassLo.setColumns(6);
      FormEntry fePepMassLo = fe(PROP_misc_fragger_digest_mass_lo, uiSpinnerDigestMassLo).label("Peptide mass range").create();
      UiSpinnerDouble uiSpinnerDigestMassHi = new UiSpinnerDouble(5000, 0, 50000, 100,
          new DecimalFormat("0.#"));
      uiSpinnerDigestMassHi.setColumns(6);
      FormEntry fePepMassHi = fe(PROP_misc_fragger_digest_mass_hi, uiSpinnerDigestMassHi).create();
      pDigest.add(fePepLenMin.label(), new CC().alignX("right"));
      pDigest.add(fePepLenMin.comp, new CC().split(3).growX());
      pDigest.add(new JLabel("-"));
      pDigest.add(fePepLenMax.comp, new CC());
      pDigest.add(fePepMassLo.label(), new CC().alignX("right"));
      pDigest.add(fePepMassLo.comp, new CC().split(3).spanX());
      pDigest.add(new JLabel("-"));
      pDigest.add(fePepMassHi.comp, new CC().wrap());

      FormEntry feMaxFragCharge = fe(MsfraggerParams.PROP_max_fragment_charge, new UiSpinnerInt(2, 0, 20, 1, 2))
          .label("Max fragment charge").create();
      uiSpinnerDbslice = new UiSpinnerInt(1, 1, 99, 1, 2);
      FormEntry feSliceDb = fe(PROP_misc_slice_db, uiSpinnerDbslice).label("<html>Split database")
          .tooltip("<html>Split database into smaller chunks.\n"
              + "Only use for very large databases (200MB+) or<br/>non-specific digestion.").create();


      pDigest.add(feMaxFragCharge.label(), new CC().split(2).span(2).alignX("right"));
      pDigest.add(feMaxFragCharge.comp);
      pDigest.add(feSliceDb.label(), new CC().alignX("right"));
      pDigest.add(feSliceDb.comp, new CC().spanX().wrap());

      pBase.add(pPeakMatch, new CC().wrap().growX());
      pBase.add(pDigest, new CC().wrap().growX());

      return pBase;
  }

  private static Object[][] convertModsToVarModsData(List<Mod> mods) {
    Object[][] data = new Object[MsfraggerParams.VAR_MOD_COUNT_MAX][TABLE_VAR_MODS_COL_NAMES.length];
    for (int i = 0; i < data.length; i++) {
      data[i][0] = false;
      data[i][1] = null;
      data[i][2] = null;
      data[i][3] = null;
    }
    if (mods.size() > data.length) {
      throw new IllegalStateException("loaded mods list length is longer than msfragger supports");
    }
    for (int i = 0; i < mods.size(); i++) {
      Mod m = mods.get(i);
      data[i][0] = m.isEnabled;
      data[i][1] = m.sites;
      data[i][2] = m.massDelta;
      data[i][3] = m.maxOccurrences;
    }
    return data;
  }

  private ModsTable createTableVarMods() {
    Object[][] data = convertModsToVarModsData(Collections.emptyList());
    ModificationsTableModel m = new ModificationsTableModel(
        TABLE_VAR_MODS_COL_NAMES,
        new Class<?>[]{Boolean.class, String.class, Double.class, Integer.class},
        new boolean[]{true, true, true, true},
        new int[]{0, 1, 2, 3},
        data);
    final ModsTable t = new ModsTable(m, TABLE_VAR_MODS_COL_NAMES, TabMsfragger::convertModsToVarModsData);
    Fragpipe.rename(t, "table.var-mods", TAB_PREFIX);

    t.setToolTipText(
        "<html>Variable Modifications.<br/>\n" +
            "Values:<br/>\n" +
            "<ul>\n" +
            "<li>A-Z amino acid codes</li>\n" +
            "<li>*​ ​is​ ​used​ ​to​ ​represent​ ​any​ ​amino​ ​acid</li>\n" +
            "<li>^​ ​is​ ​used​ ​to​ ​represent​ ​a​ ​terminus</li>\n" +
            "<li>[​ ​is​ ​a​ ​modifier​ ​for​ ​protein​ ​N-terminal</li>\n" +
            "<li>]​ ​is​ ​a​ ​modifier​ ​for​ ​protein​ ​C-terminal</li>\n" +
            "<li>n​ ​is​ ​a​ ​modifier​ ​for​ ​peptide​ ​N-terminal</li>\n" +
            "<li>c​ ​is​ ​a​ ​modifier​ ​for​ ​peptide​ ​C-terminal</li>\n" +
            "</ul>\n" +
            "Syntax​ ​Examples:\n" +
            "<ul>\n" +
            "<li>15.9949​ ​M​ ​3(for​ ​oxidation​ ​on​ ​methionine)</li>\n" +
            "<li>79.66331​ ​STY​ 1​(for​ ​phosphorylation)</li>\n" +
            "<li>-17.0265​ ​nQnC​ ​1(for​ ​pyro-Glu​ ​or​ ​loss​ ​of​ ​ammonia​ ​at peptide​ ​N-terminal)</li>\n" +
            "</ul>\n" +
            "Example​ ​(M​ ​oxidation​ ​and​ ​N-terminal​ ​acetylation):\n" +
            "<ul>\n" +
            "<li>variable_mod_01​ ​=​ ​15.9949​ ​M 3</li>\n" +
            "<li>variable_mod_02​ ​=​ ​42.0106​ ​[^ 1</li>\n" +
            "</ul>");
    t.setDefaultRenderer(Double.class, new TableCellDoubleRenderer());
    t.setDefaultRenderer(Integer.class, new TableCellIntRenderer());

    // set cell editor for max occurs for var mods
    DefaultCellEditor cellEditorMaxOccurs = new TableCellIntSpinnerEditor(1, 5, 1);
    t.setDefaultEditor(Integer.class, cellEditorMaxOccurs);
    t.setFillsViewportHeight(true);

    return t;
  }

  private static Object[][] convertModsToFixTableData(List<Mod> mods) {
    Object[][] data = new Object[MsfraggerParams.ADDONS_HUMAN_READABLE.length][TABLE_FIX_MODS_COL_NAMES.length];
    for (int i = 0; i < data.length; i++) {
      data[i][0] = false;
      data[i][1] = MsfraggerParams.ADDONS_HUMAN_READABLE[i];
      data[i][2] = 0.0;
    }
    if (mods.size() > data.length) {
      throw new IllegalStateException("mod list length larger than fix mods used by fragger");
    }
    for (int i = 0; i < mods.size(); i++) {
      Mod m = mods.get(i);
      data[i][0] = m.isEnabled;
      data[i][1] = m.sites;
      data[i][2] = m.massDelta;
    }
    return data;
  }

  private ModsTable createTableFixMods() {
    Object[][] data = convertModsToFixTableData(Collections.emptyList());

    ModificationsTableModel m = new ModificationsTableModel(
        TABLE_FIX_MODS_COL_NAMES,
        new Class<?>[]{Boolean.class, String.class, Double.class},
        new boolean[]{true, false, true},
        new int[]{0, 1, 2},
        data);

    ModsTable t = new ModsTable(m, TABLE_FIX_MODS_COL_NAMES, TabMsfragger::convertModsToFixTableData);
    Fragpipe.rename(t, "table.fix-mods", TAB_PREFIX);

    t.setToolTipText(
        "<html>Fixed Modifications.<br/>Act as if the mass of aminoacids/termini was permanently changed.");
    t.setDefaultRenderer(Double.class, new TableCellDoubleRenderer());
    t.setFillsViewportHeight(true);

    return t;
  }

  private JPanel createPanelMods() {
    // Panel with modifications
    JPanel pMods = new JPanel(new MigLayout(new LC().fillX()));
    pMods.setBorder(new TitledBorder("Modifications"));

    JPanel pVarmods = new JPanel(new MigLayout(new LC()));
    pVarmods.setBorder(new TitledBorder("Variable modifications"));

    FormEntry feMaxVarmodsPerMod = fe(MsfraggerParams.PROP_max_variable_mods_per_peptide, new UiSpinnerInt(3, 0, 5, 1, 4))
        .label("Max variable mods on a peptide")
        .tooltip("<html>The maximum number of variable modifications allowed per\n" +
            "peptide sequence. This number does not include fixed modifications.").create();
    FormEntry feMaxCombos = fe(MsfraggerParams.PROP_max_variable_mods_combinations, new UiSpinnerInt(5000, 0, 100000, 500, 4))
        .label("Max combinations").create();
    FormEntry feMultipleVarModsOnResidue = fe(MsfraggerParams.PROP_allow_multiple_variable_mods_on_residue, new UiCheck("Multiple mods on residue", null))
        .tooltip("<html>Allow a single residue to carry multiple modifications.").create();
    tableVarMods = createTableVarMods();
    SwingUtilities.invokeLater(() -> {
      setJTableColSize(tableVarMods, 0, 20, 150, 50);
    });
    JScrollPane varModsScroll = new JScrollPane(tableVarMods,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    //tableScrollVarMods.setPreferredSize(new Dimension(tableScrollVarMods.getPreferredSize().width, 140));

    pVarmods.add(feMaxVarmodsPerMod.label(), new CC().alignX("right"));
    pVarmods.add(feMaxVarmodsPerMod.comp);
    pVarmods.add(feMaxCombos.label(), new CC().alignX("right"));
    pVarmods.add(feMaxCombos.comp);
    pVarmods.add(feMultipleVarModsOnResidue.comp, new CC().wrap());
    pVarmods
        .add(varModsScroll, new CC().minHeight("100px").maxHeight("150px").spanX().wrap());

    JPanel pFixmods = new JPanel(new MigLayout(new LC()));
    pFixmods.setBorder(new TitledBorder("Fixed modifications"));

    tableFixMods = createTableFixMods();
    SwingUtilities.invokeLater(() -> {
      setJTableColSize(tableFixMods, 0, 20, 150, 50);
    });
    JScrollPane tableScrollFixMods = new JScrollPane(tableFixMods,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    pFixmods.add(tableScrollFixMods,
        new CC().minHeight("100px").maxHeight("200px").growX().spanX().wrap());

    pMods.add(pVarmods, new CC().wrap().growX());
    pMods.add(pFixmods, new CC().wrap().growX());

    // mass offsets text field separately
    String tooltipMassOffsets = "<html>Creates multiple precursor tolerance windows with<br>\n"
        + "specified mass offsets. These values are multiplexed<br>\n"
        + "with the isotope error option.<br><br>\n\n"
        + "For example, value \"0/79.966\" can be used<br>\n"
        + "as a restricted open search that looks for unmodified<br>\n"
        + "and phosphorylated peptides (on any residue).<br><br>\n\n"
        + "Setting isotope_error to 0/1/2 in combination<br>\n"
        + "with this example will create search windows around<br>\n"
        + "(0,1,2,79.966, 80.966, 81.966).";

    epMassOffsets = SwingUtils.createClickableHtml(SwingUtils.wrapInStyledHtml(""), false,
        false, null, true);
    epMassOffsets.setPreferredSize(new Dimension(100, 25));
    //epMassOffsets.setMaximumSize(new Dimension(200, 25));
    epMassOffsets.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
    epMassOffsets.setFont(new JLabel().getFont());

    uiTextMassOffsets = UiUtils.uiTextBuilder().filter("[^-\\(\\)\\./,\\d ]").text("0").create();

    FormEntry feMassOffsets = fe(MsfraggerParams.PROP_mass_offsets, epMassOffsets)
        .label("User defined variable mass shifts (on any aminoacid)")
        .tooltip(tooltipMassOffsets).create();
    MigUtils.get().add(pMods, feMassOffsets.label()).wrap();
    MigUtils.get().add(pMods, feMassOffsets.comp).growX().wrap();

    pContent.add(pMods, new CC().wrap().growX());

    return pMods;
  }

  /** Panel with all the advanced options. */
  private JPanel createPanelAdvancedOptions() {
    JPanel pAdvanced = new JPanel(new MigLayout(new LC()));
    pAdvanced.setBorder(new TitledBorder("Advanced Options"));

    CC alignRight = new CC().alignX("right");
    CC wrap = new CC().wrap();

    JPanel pOpenSearch = new JPanel(new MigLayout(new LC()));
    {

      pOpenSearch.setBorder(new TitledBorder("Open Search Options"));
      FormEntry feTrackZeroTopN = fe(MsfraggerParams.PROP_track_zero_topN,
          new UiSpinnerInt(0, 0, 1000, 5, 3)).label("Track zero top N").create();
      FormEntry feAddTopNComplementary = fe(MsfraggerParams.PROP_add_topN_complementary,
          new UiSpinnerInt(0, 0, 1000, 2, 3)).label("Add top N complementary").create();
      UiSpinnerDouble spinnerZeroBinAcceptExpect = new UiSpinnerDouble(0, 0, Double.MAX_VALUE,
          0.1, 1,
          new DecimalFormat("0.00"));
      spinnerZeroBinAcceptExpect.setColumns(3);
      FormEntry feZeroBinAcceptExpect = fe(MsfraggerParams.PROP_zero_bin_accept_expect, spinnerZeroBinAcceptExpect).label("Zero bin accept expect").create();
      UiSpinnerDouble spinnerZeroBinMultExpect = new UiSpinnerDouble(1, 0, 1, 0.05, 2,
          new DecimalFormat("0.00"));
      spinnerZeroBinMultExpect.setColumns(3);
      FormEntry feZeroBinMultExpect = fe(MsfraggerParams.PROP_zero_bin_mult_expect, spinnerZeroBinMultExpect).label("Zero bin multiply expect").create();

      pOpenSearch.add(feTrackZeroTopN.label(), alignRight);
      pOpenSearch.add(feTrackZeroTopN.comp);
      pOpenSearch.add(feAddTopNComplementary.label(), alignRight);
      pOpenSearch.add(feAddTopNComplementary.comp, wrap);
      pOpenSearch.add(feZeroBinAcceptExpect.label(), alignRight);
      pOpenSearch.add(feZeroBinAcceptExpect.comp);
      pOpenSearch.add(feZeroBinMultExpect.label(), alignRight);
      pOpenSearch.add(feZeroBinMultExpect.comp, wrap);

      uiCheckShiftedIons = new UiCheck("<html>Localize delta mass", null);
      FormEntry feShiftedIonsCheck = fe(MsfraggerParams.PROP_localize_delta_mass, uiCheckShiftedIons)
          .tooltip("<html>Shifted ion series are the same as regular b/y ions,\n"
              + "but with the addition of the mass shift of the precursor.\n"
              + "Regular ion series will still be used.\n"
              + "This option is </b>incompatible</b> with database splitting.").create();
      UiText uiTextShiftedIonsExclusion = new UiText();
      uiTextShiftedIonsExclusion.setDocument(DocumentFilters.getFilter("[A-Za-z]"));
      uiTextShiftedIonsExclusion.setText("(-1.5,3.5)");
      FormEntry feShiftedIonsExclusion = fe(
          MsfraggerParams.PROP_delta_mass_exclude_ranges, uiTextShiftedIonsExclusion).label("Delta mass exclude ranges")
          .tooltip("<html>Ranges expressed like: (-1.5,3.5)").create();
      pOpenSearch.add(feShiftedIonsCheck.comp, new CC().alignX("right"));
      pOpenSearch.add(feShiftedIonsExclusion.label(), new CC().split(2).spanX().gapLeft("25px"));
      pOpenSearch.add(feShiftedIonsExclusion.comp, new CC().growX());

      uiCheckShiftedIons.addActionListener(e -> {
        final boolean selected = uiCheckShiftedIons.isSelected();
        final int dbSlicing = uiSpinnerDbslice.getActualValue();
        if (selected && dbSlicing > 1) {
          JOptionPane.showMessageDialog(this,
              "<html>This option is incompatible with DB Splitting.<br/>"
                  + "Please either turn it off, or turn off DB Splitting by setting<br/>"
                  + "it to 1.", "Incompatible options", JOptionPane.WARNING_MESSAGE);
        }
      });

      uiSpinnerDbslice.addChangeListener(e -> {
        final boolean selected = uiCheckShiftedIons.isSelected();
        final int dbSlicing = uiSpinnerDbslice.getActualValue();
        if (selected && dbSlicing > 1) {
          JOptionPane.showMessageDialog(this,
              "<html><code>DB Slicing<code> is incompatible with <code>Localize delta mass</code> option.<br/>"
                  + "Please either set <code>DB Slicing<code> to 1, or uncheck <code>Localize delta mass</code> checkbox<br/>"
                  + "at the end of this form.",
              "Incompatible options", JOptionPane.WARNING_MESSAGE);
        }
      });

    }

    JPanel pSpectral = new JPanel(new MigLayout(new LC()));
    {
      pSpectral.setBorder(new TitledBorder("Spectral Processing"));

      FormEntry feMinPeaks = fe(MsfraggerParams.PROP_minimum_peaks, new UiSpinnerInt(15, 0, 1000, 1, 4))
          .label("Min peaks").create();
      FormEntry feUseTopN = fe(MsfraggerParams.PROP_use_topN_peaks, new UiSpinnerInt(100, 0, 1000000, 10, 4)).label("Use top N peaks").create();
      UiSpinnerDouble spinnerMinRatio = new UiSpinnerDouble(0.01, 0, Double.MAX_VALUE, 0.1, 2,
          new DecimalFormat("0.00"));
      spinnerMinRatio.setColumns(4);
      FormEntry feMinRatio = fe(MsfraggerParams.PROP_minimum_ratio, spinnerMinRatio).label("Min ratio").create();
      FormEntry feClearRangeMzLo = fe(PROP_misc_fragger_clear_mz_lo, new UiSpinnerInt(0, 0, 100000, 10, 4)).label("Clear m/z range").create();
      FormEntry feClearRangeMzHi = fe(PROP_misc_fragger_clear_mz_hi, new UiSpinnerInt(0, 0, 100000, 10, 4)).create();

      uiComboMassMode = new UiCombo(); // UiUtils.createUiCombo(FraggerPrecursorMassMode.values());
      uiComboMassMode.setModel(new DefaultComboBoxModel<>(new String[] {
          FraggerPrecursorMassMode.selected.name(),
          FraggerPrecursorMassMode.isolated.name(),
          FraggerPrecursorMassMode.recalculated.name(),
      }));
      uiComboMassMode.addItemListener(TabMsfragger::actionChangeMassMode);
      FormEntry fePrecursorMassMode = fe(MsfraggerParams.PROP_precursor_mass_mode, uiComboMassMode).label("Precursor mass mode")
          .tooltip("<html>Determines which entry from mzML files will be\n"
              + "used as the precursor's mass - 'Selected' or 'Isolated' ion.\n"
              + "'Recalculated' option runs a separate MSAdjuster tool to\n"
              + "perform mono-isotopic mass correction").create();

      FormEntry feRemovePrecPeak = fe(MsfraggerParams.PROP_remove_precursor_peak, UiUtils.createUiCombo(RemovePrecursorPeak.getNames())).label("Remove precursor peak").create();
      DecimalFormat df1 = new DecimalFormat("0.#");
      FormEntry fePrecRemoveRangeLo = fe(PROP_misc_fragger_remove_precursor_range_lo,
          UiSpinnerDouble.builder(-1.5, -1000.0, 1000.0, 0.1).setNumCols(5).setFormat(df1).create())
          .label("removal m/z range").create();
      FormEntry fePrecRemoveRangeHi = fe(PROP_misc_fragger_remove_precursor_range_hi,
          UiSpinnerDouble.builder(+1.5, -1000.0, 1000.0, 0.1).setNumCols(5).setFormat(df1).create())
          .create();
      FormEntry feIntensityTransform = fe(MsfraggerParams.PROP_intensity_transform, UiUtils.createUiCombo(IntensityTransform.getNames())).label("Intensity transform").create();

      pSpectral.add(fePrecursorMassMode.label(), new CC().alignX("right"));
      pSpectral.add(fePrecursorMassMode.comp, new CC().wrap());

      pSpectral.add(feMinPeaks.label(), alignRight);
      pSpectral.add(feMinPeaks.comp, new CC().split(5).spanX());
      pSpectral.add(feUseTopN.label(), new CC().gapBefore("20px"));
      pSpectral.add(feUseTopN.comp, new CC());
      pSpectral.add(feMinRatio.label(), new CC().gapBefore("20px"));
      pSpectral.add(feMinRatio.comp, wrap);

      pSpectral.add(feClearRangeMzLo.label(), alignRight);
      pSpectral.add(feClearRangeMzLo.comp, new CC().split(3).spanX());
      pSpectral.add(new JLabel("-"));
      pSpectral.add(feClearRangeMzHi.comp, new CC().wrap());

      pSpectral.add(feRemovePrecPeak.label(), alignRight);
      pSpectral.add(feRemovePrecPeak.comp, new CC().split(5).spanX());
      pSpectral.add(fePrecRemoveRangeLo.label(), new CC());
      pSpectral.add(fePrecRemoveRangeLo.comp, new CC());
      pSpectral.add(new JLabel("-"));
      pSpectral.add(fePrecRemoveRangeHi.comp, new CC().wrap());
      pSpectral.add(feIntensityTransform.label(), alignRight);
      pSpectral.add(feIntensityTransform.comp, new CC().wrap());
    }

    // Advanced peak matching panel
    JPanel pPeakMatch = new JPanel(new MigLayout(new LC()));
    {
      pPeakMatch.setBorder(new TitledBorder("Peak Matching and Output Advanced Options"));

      FormEntry feMinFragsModeling = fe(MsfraggerParams.PROP_min_fragments_modelling, new UiSpinnerInt(2, 0, 1000, 1, 4)).label("Min frags modeling").create();
      FormEntry feMinMatchedFrags = fe(MsfraggerParams.PROP_min_matched_fragments, new UiSpinnerInt(4, 0, 1000, 1, 4)).label("Min matched frags").create();

      FormEntry feIonSeries = fe(MsfraggerParams.PROP_fragment_ion_series, new UiText(10))
          .label("Fragment ion series").tooltip(
              "Which peptide ion series to check against.\n"
                  + "<b>Use spaces, commas or semicolons as delimiters</b>, e.g. \"b,y\"\n"
                  + "This mostly depends on fragmentation method.\n"
                  + "Typically \"b,y\" are used for CID and \"c,z\" for ECD.\n"
                  + "MSFragger can generate \"a,b,c,x,y,z\" ion series by default,\n"
                  + "but <b>you can define your own in 'Define custom ion series' field</b>.\n"
                  + "If you define custom series, you will need to include the name you\n"
                  + "gave it here.").create();
      uiTextCustomIonSeries = new UiText(10);
      String tooltipCustomIonSeriesDisabled = "This feature is currently disabled";
      String tooltipCustomIonSeriesOriginal = "Custom ion series allow specification of arbitrary mass gains/losses\n"
          + "for N- and C-terminal ions. Separate multiple definitions by commas or semicolons.\n"
          + "<b>Format:</b> name terminus mass-delta\n"
          + "Example definition string:\n"
          + "b* N -17.026548; b0 N -18.010565\n"
          + "This would define two new ion types named <i>b*</i> and <i>b0</i>,\n"
          + "you can name them whatever you fancy. <i>b*</i> is the equivalent of an\n"
          + "N terminal b-ion with ammonia loss, <i>b0</i> is the same with water loss.\n";
      FormEntry feCustomSeries = fe(MsfraggerParams.PROP_ion_series_definitions, uiTextCustomIonSeries)
          .label("Define custom ion series").tooltip(tooltipCustomIonSeriesDisabled).create();
      labelCustomIonSeries = feCustomSeries.label();

      FormEntry feTrueTolUnits = fe(MsfraggerParams.PROP_precursor_true_units, UiUtils.createUiCombo(FragmentMassTolUnits.values())).label("Precursor true tolerance").create();
      UiSpinnerDouble uiSpinnerTrueTol = new UiSpinnerDouble(10, 0, 100000, 5,
          new DecimalFormat("0.#"));
      uiSpinnerTrueTol.setColumns(4);
      FormEntry feTrueTol = fe(MsfraggerParams.PROP_precursor_true_tolerance, uiSpinnerTrueTol)
          .tooltip("True precursor mass tolerance should be set to your instrument's\n"
              + "precursor mass accuracy(window is +/- this value).  This value is used\n"
              + "for tie breaking of results and boosting of unmodified peptides in open\n"
              + "search.").create();
      FormEntry feReportTopN = fe(MsfraggerParams.PROP_output_report_topN,
          new UiSpinnerInt(1, 1, 10000, 1, 4)).label("Report top N")
          .tooltip("Report top N PSMs per input spectrum.").create();
      UiSpinnerDouble uiSpinnerOutputMaxExpect = new UiSpinnerDouble(50, 0, Double.MAX_VALUE, 1,
          new DecimalFormat("0.#"));
      uiSpinnerOutputMaxExpect.setColumns(4);
      FormEntry feOutputMaxExpect = fe(MsfraggerParams.PROP_output_max_expect, uiSpinnerOutputMaxExpect).label("Output max expect")
          .tooltip("Suppresses reporting of PSM if top hit has<br> expectation greater than this threshold").create();


      uiComboOutputType = UiUtils.createUiCombo(FraggerOutputType.values());
      FormEntry feOutputType = fe(MsfraggerParams.PROP_output_format, uiComboOutputType).label("Output format")
          .tooltip("How the search results are to be reported.\n" +
              "Downstream tools only support PepXML format.\n\n" +
              "Only use TSV (tab delimited file) if you want to process \n" +
              "search resutls yourself for easier import into other software.").create();

      String tooltipPrecursorCHarge =
          "Assume range of potential precursor charge states.\n" +
              "Only relevant when override_charge is set to 1.\n" +
              "Specified as space separated range of integers.";
      FormEntry fePrecursorChargeLo = fe(PROP_misc_fragger_precursor_charge_lo, new UiSpinnerInt(1, 0, 30, 1, 2))
          .tooltip(tooltipPrecursorCHarge).create();
      FormEntry fePrecursorChargeHi = fe(PROP_misc_fragger_precursor_charge_hi, new UiSpinnerInt(4, 0, 30, 1, 2))
          .tooltip(tooltipPrecursorCHarge).create();
      FormEntry feOverrideCharge = fe(MsfraggerParams.PROP_override_charge, new UiCheck("Override charge with precursor charge", null))
          .tooltip("Ignores precursor charge and uses charge state\n" +
              "specified in precursor_charge range.").create();
      FormEntry feReportAltProts = fe(MsfraggerParams.PROP_report_alternative_proteins, new UiCheck("Report alternative proteins", null, false)).create();

      pPeakMatch.add(feMinFragsModeling.label(), alignRight);
      pPeakMatch.add(feMinFragsModeling.comp);
      pPeakMatch.add(feMinMatchedFrags.label(), alignRight);
      pPeakMatch.add(feMinMatchedFrags.comp, new CC().wrap());

      pPeakMatch.add(feIonSeries.label(), alignRight);
      pPeakMatch.add(feIonSeries.comp, new CC().growX());
      pPeakMatch.add(labelCustomIonSeries, new CC().split(2).spanX());
      pPeakMatch.add(feCustomSeries.comp, new CC().growX().wrap());

      pPeakMatch.add(feTrueTolUnits.label(), alignRight);
      pPeakMatch.add(feTrueTolUnits.comp, new CC().split(2));
      pPeakMatch.add(feTrueTol.comp, new CC().growX());

      pPeakMatch.add(feOverrideCharge.comp, new CC().split(4).spanX());
      pPeakMatch.add(fePrecursorChargeLo.comp);
      pPeakMatch.add(new JLabel("-"));
      pPeakMatch.add(fePrecursorChargeHi.comp, wrap);
      pPeakMatch.add(feReportTopN.label(), alignRight);
      pPeakMatch.add(feReportTopN.comp, new CC().growX());
      pPeakMatch.add(feReportAltProts.comp, new CC().alignX("left").spanX().wrap());
      pPeakMatch.add(feOutputType.label(), alignRight);
      pPeakMatch.add(feOutputType.comp);
      pPeakMatch.add(feOutputMaxExpect.label(), alignRight);
      pPeakMatch.add(feOutputMaxExpect.comp, wrap);
    }

    pAdvanced.add(pSpectral, new CC().wrap().growX());
    pAdvanced.add(pPeakMatch, new CC().wrap().growX());
    pAdvanced.add(pOpenSearch, new CC().wrap().growX());

    return pAdvanced;
  }

  private FormEntry.Builder fe(JComponent comp, String name) {
    return Fragpipe.fe(comp, name, TAB_PREFIX);
  }

  private FormEntry.Builder fe(String name, JComponent comp) {
    return fe(comp, name);
  }

  private void onClickLoadDefaults(ActionEvent actionEvent) {
    String s = (String)uiComboLoadDefaultsNames.getSelectedItem();
    SearchTypeProp type = SEARCH_TYPE_NAME_MAPPING.get(s);
    if (type == null) {
      throw new IllegalStateException(String.format("No mapping for search type string '%s'", s));
    }
    if (loadDefaults(type, true)) {
      postSearchTypeUpdate(type, true);
    }
  }

  private void onClickSave(ActionEvent e) {
    cacheSave();

    // now save the actual user's choice
    JFileChooser fc = new JFileChooser();
    fc.setApproveButtonText("Save");
    fc.setApproveButtonToolTipText("Save to a file");
    fc.setDialogTitle("Choose where params file should be saved");
    fc.setMultiSelectionEnabled(false);

    final String propName = ThisAppProps.PROP_FRAGGER_PARAMS_FILE_IN;
    ThisAppProps.load(propName, fc);

    fc.setSelectedFile(new File(MsfraggerParams.CACHE_FILE));
    Component parent = SwingUtils.findParentFrameForDialog(this);
    int saveResult = fc.showSaveDialog(parent);
    if (JFileChooser.APPROVE_OPTION == saveResult) {
      File selectedFile = fc.getSelectedFile();
      Path path = Paths.get(selectedFile.getAbsolutePath());
      ThisAppProps.save(propName, path.toString());

      // if exists, overwrite
      if (Files.exists(path)) {
        int overwrite = JOptionPane.showConfirmDialog(parent, "<html>File exists, overwrtie?<br/><br/>" + path.toString(), "Overwrite", JOptionPane.OK_CANCEL_OPTION);
        if (JOptionPane.OK_OPTION != overwrite) {
          return;
        }
        try {
          Files.delete(path);
        } catch (IOException ex) {
          JOptionPane.showMessageDialog(parent, "Could not overwrite", "Overwrite", JOptionPane.ERROR_MESSAGE);
          return;
        }
      }
      try {
        ThisAppProps.save(PROP_FILECHOOSER_LAST_PATH, path.toAbsolutePath().toString());
        MsfraggerParams params = formCollect();

        params.save(new FileOutputStream(path.toFile()));
        params.save();

      } catch (IOException ex) {
        JOptionPane.showMessageDialog(parent, "<html>Could not save file: <br/>" + path.toString() +
            "<br/>" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }
    }
  }

  private void cacheSave() {
    log.warn("Old cache save fragger tab method called");
    // saving form data, except modification tables
    {
      Map<String, String> map = formTo();
      Properties mapAsProps = PropertiesUtils.from(map);
      Path tempFileForm = CacheUtils.getTempFile(CACHE_FORM);
      log.debug("Saving cache cacheSave() to: {}", tempFileForm.toString());
      try {
        mapAsProps.store(Files.newBufferedWriter(tempFileForm), ThisAppProps.cacheComments());
      } catch (IOException e) {
        log.warn("Could not store {} cache as map to: {}", this.getClass().getSimpleName(), tempFileForm.toString());
      }
      log.debug("Done saving cache cacheSave() to: {}", tempFileForm.toString());
    }

    // storing form properties that can't be just represented in the map
    {
      MsfraggerParams msfraggerParams = formCollect();
      Path tempFileProps = CacheUtils.getTempFile(CACHE_PROPS);
      try {
        msfraggerParams.save(Files.newOutputStream(tempFileProps));
      } catch (IOException e) {
        log.warn("Could not store {} cache as msfragger props to: {}", this.getClass().getSimpleName(), tempFileProps.toString());
      }
    }
  }

  private void cacheLoad() {
    log.warn("Old cache load fragger tab method called");
    // load form as map first
    {
      try {
        Path path = CacheUtils.locateTempFile(CACHE_FORM);
        Properties propsFromFile = PropertiesUtils.from(path);
        Map<String, String> map = PropertiesUtils.to(propsFromFile);
        formFrom(map);
      } catch (FileNotFoundException ignored) {
        // no form cache yet
      } catch (IOException e) {
        log.warn("Could not load properties as map from cache file: {}", e.getMessage());
      }
    }

    // then load specific msfragger non-properties-representable params
    {
      try {
        Path path = CacheUtils.locateTempFile(CACHE_PROPS);
        MsfraggerParams params = new MsfraggerParams();
        params.load(Files.newInputStream(path), false);
        formFrom(params);
      } catch (FileNotFoundException ignored) {
        // no form cache yet
      } catch (IOException e) {
        log.warn("Could not load properties as map from cache file: {}", e.getMessage());
      }

    }
  }

  private void setJTableColSize(JTable table, int colIndex, int minW, int maxW, int prefW) {
    table.getColumnModel().getColumn(colIndex).setMinWidth(minW);
    table.getColumnModel().getColumn(colIndex).setMaxWidth(maxW);
    table.getColumnModel().getColumn(colIndex).setPreferredWidth(prefW);
  }

  private void updateRowHeights(JTable table) {
    for (int row = 0; row < table.getRowCount(); row++) {
      int rowHeight = table.getRowHeight();

      for (int column = 0; column < table.getColumnCount(); column++) {
        Component comp = table.prepareRenderer(table.getCellRenderer(row, column), row, column);
        rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
      }

      table.setRowHeight(row, rowHeight);
    }
  }

  private void formFrom(MsfraggerParams params) {
    Map<String, String> map = paramsTo(params);
    formFrom(map);
    tableVarMods.setData(params.getVariableMods());
    tableFixMods.setData(params.getAdditionalMods());
    updateRowHeights(tableVarMods);
    setJTableColSize(tableVarMods, 0, 20, 150, 50);
    updateRowHeights(tableFixMods);
    setJTableColSize(tableFixMods, 0, 20, 150, 50);
  }

  private MsfraggerParams formCollect() {
    Map<String, String> map = formTo();
    MsfraggerParams params = paramsFrom(map);

    // before collecting mods, make sure that no table cell editor is open
    stopJTableEditing(tableFixMods);
    stopJTableEditing(tableVarMods);

    List<Mod> modsVar = formTo(tableVarMods.model);
    params.setVariableMods(modsVar);
    List<Mod> modsFix = formTo(tableFixMods.model);
    params.setAdditionalMods(modsFix);
    return params;
  }

  private boolean stopJTableEditing(JTable t) {
    TableCellEditor editor = t.getCellEditor();
    if (editor == null) {
      log.debug("cell editor was null");
      return true;
    }
    log.debug("cell editor existed, trying to close");
    return editor.stopCellEditing();
  }

  private List<Mod> formTo(ModificationsTableModel model) {
    return model.getModifications();
  }

  private void formFrom(Map<String, String> map) {
    SwingUtilities.invokeLater(() -> valuesFromMap(this, map));
  }

  public void valuesFromMap(Container origin, Map<String, String> map) {
    // TODO: switch to SwingUtils vesrion of this mehtod
    Map<String, Component> comps = SwingUtils.mapComponentsByName(origin, true);
    for (Entry<String, String> kv : map.entrySet()) {
      final String name = kv.getKey();
      Component component = comps.get(name);
      if (component != null) {
        if (!(component instanceof StringRepresentable)) {
          log.trace(String
              .format("SwingUtils.valuesFromMap() Found component of type [%s] by name [%s] which does not implement [%s]",
                  component.getClass().getSimpleName(), name,
                  StringRepresentable.class.getSimpleName()));
          continue;
        }
        try {
          ((StringRepresentable) component).fromString(kv.getValue());
        } catch (IllegalArgumentException ex) {
          if (component.equals(uiComboMassMode)) {
            log.error("When loading fragger-mass-mode option, the given value ({}) is no longer an option in MSfragger/FragPipe. "
                + "Not changing value, please select manually", kv.getValue());
          } else if (component instanceof JComboBox) {
            log.warn(
                "Tried to load a value in combo-box that is not in combo-box's model. Component name={}, input value={}",
                name, kv.getValue());
          } else {
            log.warn(
                "Illegal input when filling UI form. Name={}, input value={}", name, kv.getValue());
          }
        }

      }
    }
  }

  private Map<String, String> formTo() {
    Map<String, String> map = SwingUtils.valuesToMap(this);
    HashMap<String, String> m = new HashMap<>();
    map.forEach((k, v) -> m.put(StringUtils.stripLeading(k, TAB_PREFIX), v));
    return m;
  }

  public MsfraggerParams getParams() {
    return formCollect();
  }

  /**
   * Converts textual representations of all fields in the form to stadard {@link MsfraggerParams}.
   */
  private MsfraggerParams paramsFrom(Map<String, String> map) {
    MsfraggerParams p = new MsfraggerParams();
    final double[] precursorRemoveRange = new double[2];
    final double[] clearMzRange = new double[2];
    final double[] digestMassRange = new double[2];
    final int[] precursorChargeRange = new int[2];

    for (Entry<String, String> e : map.entrySet()) {
      final String k = e.getKey();
      final String v = e.getValue();
      if (MsfraggerParams.PROP_NAMES_SET.contains(k)) {
        // known property
        Function<String, String> converter = CONVERT_TO_FILE.getOrDefault(k, s -> s);
        String converted = converter.apply(v);

        if (MsfraggerParams.PROP_fragment_ion_series.equals(k) && StringUtils.isNullOrWhitespace(converted)) {
          // don't set ion series to be used in the fragger config file if the string is emtpty
          continue;
        }
        p.getProps().setProp(k, converted);

      } else {
        // unknown prop, it better should be from the "misc" category we added in this panel
        if (PROPS_MISC_NAMES.contains(k) || k.startsWith("misc.")) {
          log.trace("Found misc option: {}={}", k, v);

          switch (k) {
            case PROP_misc_fragger_remove_precursor_range_lo:
              precursorRemoveRange[0] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_remove_precursor_range_hi:
              precursorRemoveRange[1] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_clear_mz_lo:
              clearMzRange[0] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_clear_mz_hi:
              clearMzRange[1] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_digest_mass_lo:
              digestMassRange[0] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_digest_mass_hi:
              digestMassRange[1] = Double.parseDouble(v);
              break;
            case PROP_misc_fragger_precursor_charge_lo:
              precursorChargeRange[0] = Integer.parseInt(v);
              break;
            case PROP_misc_fragger_precursor_charge_hi:
              precursorChargeRange[1] = Integer.parseInt(v);
              break;
          }

        } else {
          // we don't know what this option is, someone probably forgot to add it to the list of
          // known ones
          log.debug("Unknown prop name in fragger panel: [{}] with value [{}]", k, v);
        }
      }
    }
    p.setClearMzRange(clearMzRange);
    p.setDigestMassRange(digestMassRange);
    p.setPrecursorCharge(precursorChargeRange);
    p.setRemovePrecursorRange(precursorRemoveRange);

    FraggerOutputType outType = p.getOutputFormat();
    if (outType == null) {
      throw new IllegalStateException("FraggerOutputType was not set by the point where we needed to provide the output extension.");
    }
    p.setOutputFileExtension(outType.getExtension());

    return p;
  }

  private Map<String, String> paramsTo(MsfraggerParams params) {
    HashMap<String, String> map = new HashMap<>();
    for (Entry<String, Prop> e : params.getProps().getMap().entrySet()) {
      if (e.getValue().isEnabled) {
        final Function<String, String> converter = CONVERT_TO_GUI.get(e.getKey());
        final String converted;
        if (converter != null) {
          try {
            converted = converter.apply(e.getValue().value);
            map.put(e.getKey(), converted);
          } catch (Exception ex) {
            log.error("Error converting parameter [{}={}]", e.getKey(), e.getValue().value);
          }
        } else {
          converted = e.getValue().value;
          map.put(e.getKey(), converted);
        }
      }
    }

    // special treatment of some fields
    double[] precursorRemoveRange = params.getRemovePrecursorRange();
    double[] clearMzRange = params.getClearMzRange();
    double[] digestMassRange = params.getDigestMassRange();
    int[] precursorCharge = params.getPrecursorCharge();
    DecimalFormat fmt = new DecimalFormat("0.#");
    DecimalFormat fmt2 = new DecimalFormat("0.##");
    map.put(PROP_misc_fragger_clear_mz_lo, fmt.format(clearMzRange[0]));
    map.put(PROP_misc_fragger_clear_mz_hi, fmt.format(clearMzRange[1]));
    map.put(PROP_misc_fragger_digest_mass_lo, fmt.format(digestMassRange[0]));
    map.put(PROP_misc_fragger_digest_mass_hi, fmt.format(digestMassRange[1]));
    map.put(PROP_misc_fragger_precursor_charge_lo, fmt.format(precursorCharge[0]));
    map.put(PROP_misc_fragger_precursor_charge_hi, fmt.format(precursorCharge[1]));
    map.put(PROP_misc_fragger_remove_precursor_range_lo, fmt2.format(precursorRemoveRange[0]));
    map.put(PROP_misc_fragger_remove_precursor_range_hi, fmt2.format(precursorRemoveRange[1]));

    return map;
  }

  private boolean modListContainsIllegalSites(List<Mod> mods) {
    return mods.stream().anyMatch(m -> m.sites != null && m.sites.contains("[*"));
  }

  @Subscribe
  public void on(NoteConfigMsfragger m) {
    updateEnabledStatus(this, m.isValid());
  }

  @Subscribe
  public void on(MessageValidityMsadjuster msg) {
    log.debug("'Adjust precursor masses' checkbox was removed. Not reacting to MessageValidityMsadjuster event.");
//    enablementMapping.put(uiCheckAdjustPrecursorMass, msg.isValid);
//    updateEnabledStatus(uiCheckAdjustPrecursorMass, msg.isValid);
  }

  @Subscribe
  public void on(MessageValidityMassCalibration msg) {
    log.debug("Got message 'MessageValidityMassCalibration' reading isValid = {} ", msg.isValid);
    enablementMapping.put(uiComboMassCalibrate, msg.isValid);
    updateEnabledStatus(uiComboMassCalibrate, msg.isValid);
  }

  @Subscribe
  public void on(MessagePrecursorSelectionMode m) {
    log.debug("Received MessagePrecursorSelectionMode [{}]. Doing nothing.", m.mode.name());
  }

  @Subscribe
  public void on(MessageMsfraggerParamsUpdate m) {
    formFrom(m.params);
    cacheSave();
  }

  @Subscribe
  public void on(MessageSearchType m) {
    loadDefaults(m.type);
  }

  public int getRamGb() {
    return (Integer) uiSpinnerRam.getValue();
  }

  public int getThreads() {
    return (Integer) uiSpinnerThreads.getValue();
  }

  public boolean isRun() {
    return checkRun.isSelected() && checkRun.isEnabled();
  }

  public boolean isMsadjuster() {
    FraggerPrecursorMassMode mode = FraggerPrecursorMassMode.valueOf((String) uiComboMassMode.getSelectedItem());
    if (FraggerPrecursorMassMode.recalculated.equals(mode)) {
      return true;
    }
    return false;
  }

  public int getNumDbSlices() {
    return uiSpinnerDbslice.getActualValue();
  }

  public String getMassOffsets() {
    return uiTextMassOffsets.getNonGhostText();
  }

  public String getOutputFileExt() {
    return getOutputType().getExtension();
  }

  public FraggerOutputType getOutputType() {
    String val = uiComboOutputType.getItemAt(uiComboOutputType.getSelectedIndex());
    return FraggerOutputType.valueOf(val);
  }

  private void onClickLoad(ActionEvent e) {
    FileNameExtensionFilter filter = new FileNameExtensionFilter("Properties/Params",
        "properties", "params", "para", "conf", "txt");
    JFileChooser fc = FileChooserUtils.create("Select saved file", "Load", false, FcMode.FILES_ONLY, true, filter);
    fc.setFileFilter(filter);
    FileChooserUtils.setPath(fc, Stream.of(ThisAppProps.load(ThisAppProps.PROP_FRAGGER_PARAMS_FILE_IN)));

    Component parent = SwingUtils.findParentFrameForDialog(this);
    int saveResult = fc.showOpenDialog(parent);
    if (JFileChooser.APPROVE_OPTION == saveResult) {
      File f = fc.getSelectedFile();
      Path p = Paths.get(f.getAbsolutePath());
      ThisAppProps.save(ThisAppProps.PROP_FRAGGER_PARAMS_FILE_IN, p.toString());

      if (Files.exists(p)) {
        try {
          MsfraggerParams params = formCollect();
          params.load(new FileInputStream(f), true);
          Bus.post(new MessageMsfraggerParamsUpdate(params));
          params.save();

        } catch (Exception ex) {
          JOptionPane
              .showMessageDialog(parent,
                  "<html>Could not load the saved file: <br/>" + ex.getMessage(), "Error",
                  JOptionPane.ERROR_MESSAGE);
        }
      } else {
        JOptionPane.showMessageDialog(parent, "<html>This is strange,<br/> "
                + "but the file you chose to load doesn't exist anymore.", "Strange",
            JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void loadDefaults(SearchTypeProp type) {
    MsfraggerParams params = new MsfraggerParams();
    params.loadDefaults(type);
    formFrom(params);

    // reset some fields that are not part of Fragger config file
    uiSpinnerDbslice.setValue(1);
  }

  /**
   * @return False if user's confirmation was required, but they cancelled the operation. True
   *         otherwise.
   */
  private boolean loadDefaults(final SearchTypeProp type, boolean askConfirmation) {
    if (askConfirmation) {
      int confirmation = JOptionPane.showConfirmDialog(SwingUtils.findParentFrameForDialog(this),
          "Load " + type + " search default configuration?");
      if (JOptionPane.YES_OPTION != confirmation) {
        return false;
      }
    }
    loadDefaults(type);
    return true;
  }

  private boolean trySelectEnzymeDropdown(String name) {
    try {
      String saveCuts = uiTextCuts.getNonGhostText();
      String saveNouts = uiTextNocuts.getNonGhostText();
      uiComboEnzymes.setSelectedItem(name);
      if (true || "custom".equals(name)) { // remove 'true' to reset specificities to their definitions in our file
        uiTextCuts.setText(saveCuts);
        uiTextNocuts.setText(saveNouts);
      }
      return true;
    } catch (Exception ignored) {
    }
    return false;
  }

  /**
   * @return False if user's confirmation was required, but they cancelled the operation. True
   *         otherwise.
   */
  private boolean postSearchTypeUpdate(SearchTypeProp type, boolean askConfirmation) {
    if (askConfirmation) {
      int confirmation = JOptionPane.showConfirmDialog(SwingUtils.findParentFrameForDialog(this),
          "<html>Would you like to update options for other tools as well?<br/>"
              + "<b>Highly recommended</b>, unless you're sure what you're doing)");
      if (JOptionPane.OK_OPTION != confirmation) {
        return false;
      }
    }
    Bus.post(new MessageSearchType(type));
    return true;
  }

  @Subscribe
  public void on(DbSlice.MessageInitDone m) {
    enablementMapping.put(uiSpinnerDbslice, m.isSuccess);
    updateEnabledStatus(uiSpinnerDbslice, m.isSuccess);
  }

  @Subscribe
  public void on(MessageRun msg) {
    cacheSave();
  }

  @Subscribe
  public void on(MessageSaveCache msg) {
    cacheSave();
  }

  public String getEnzymeName() {
    return uiTextEnzymeName.getNonGhostText();
  }

}

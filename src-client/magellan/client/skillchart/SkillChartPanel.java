/*
 *  Copyright (C) 2000-2004 Roger Butenuth, Andreas Gampe,
 *                          Stefan Goetz, Sebastian Pappert,
 *                          Klaas Prause, Enno Rehling,
 *                          Sebastian Tusk, Ulrich Kuester,
 *                          Ilja Pavkovic
 *
 * This file is part of the Eressea Java Code Base, see the
 * file LICENSING for the licensing information applying to
 * this file.
 *
 */

package magellan.client.skillchart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import magellan.client.event.EventDispatcher;
import magellan.client.event.SelectionEvent;
import magellan.client.event.SelectionListener;
import magellan.client.swing.InternationalizedDataPanel;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.ID;
import magellan.library.Region;
import magellan.library.Skill;
import magellan.library.Unit;
import magellan.library.rules.SkillType;
import magellan.library.utils.Resources;
import magellan.library.utils.SkillStats;
import magellan.library.utils.logging.Logger;

/**
 * A class painting barcharts out of skills of eressea units. The data with the units to be
 * considered is received solely by SelectionEvents.
 *
 * @author Ulrich Küster
 */
public class SkillChartPanel extends InternationalizedDataPanel implements SelectionListener {
  private static final Logger log = Logger.getInstance(SkillChartPanel.class);
  /** the key of the only data series of the chart */
  private static final String SERIES = "persons";
  private ChartPanel chartPanel;
  private JFreeChart chart;
  private JComboBox persons;
  private JComboBox totalSkillPoints;
  private JComboBox totalSkillLevel;
  private JComboBox skills;
  private SkillStats skillStats = new SkillStats();
  private Set<Region> regions;
  private Map<ID, Faction> factions = new Hashtable<ID, Faction>();
  private JCheckBox cumulative;

  /**
   * Creates a new SkillChartPanel. The data is received by SelectionEvents (where factions and
   * regions are considered). Despite of that an GameData-reference is necessary to set all regions as
   * data, if no region is selected
   */
  public SkillChartPanel(EventDispatcher ed, GameData data, Properties settings) {
    super(ed, data, settings);
    regions = new HashSet<Region>(data.getRegions());

    ed.addSelectionListener(this);

    // create the chart: a vertical bar chart of the number of persons per skill level
    chart =
        ChartFactory.createBarChart("", Resources
            .get("skillchart.skillchartpanel.labeltext.horizontalaxis"), Resources
                .get("skillchart.skillchartpanel.labeltext.verticalaxis"), createDataset(null),
            PlotOrientation.VERTICAL, false, true, false);
    chart.getTitle().setFont(new Font("Arial", Font.BOLD, 24));
    chart.setBackgroundPaint(new GradientPaint(0, 0, Color.white, 0, 1000, Color.blue));

    CategoryPlot plot = chart.getCategoryPlot();
    plot.setInsets(new RectangleInsets(10, 20, 20, 40));
    NumberAxis valueAxis = (NumberAxis) plot.getRangeAxis();
    valueAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    valueAxis.setAutoRangeIncludesZero(true);

    BarRenderer renderer = (BarRenderer) plot.getRenderer();
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setShadowVisible(false);
    renderer.setDrawBarOutline(true);
    renderer.setDefaultToolTipGenerator(
        (CategoryDataset dataset, int row, int column) -> getToolTip(dataset.getColumnKey(column)));

    chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new Dimension(480, 320));

    // initialize the Panel with all the stuff
    GridBagLayout grid = new GridBagLayout();
    setLayout(grid);

    GridBagConstraints c =
        new GridBagConstraints(0, 0, 2, 1, 1.0, 1.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 2, 2);
    add(chartPanel, c);

    c =
        new GridBagConstraints(0, 1, 1, 1, 0.1, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 2, 2);
    skills = new JComboBox();
    skills.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setSkillType();
      }
    });
    add(skills, c);

    persons =
        new JComboBox(new String[] { Resources
            .get("skillchart.skillchartpanel.labeltext.totalpersons") });
    c =
        new GridBagConstraints(1, 1, 1, 1, 0.1, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 2, 2);
    add(persons, c);

    totalSkillPoints =
        new JComboBox(new String[] { Resources
            .get("skillchart.skillchartpanel.labeltext.totalskilllevel") });
    c =
        new GridBagConstraints(0, 2, 1, 1, 0.1, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 2, 2);
    add(totalSkillPoints, c);

    if (data.noSkillPoints) {
      totalSkillPoints.setVisible(false);
    }

    totalSkillLevel =
        new JComboBox(new String[] { Resources
            .get("skillchart.skillchartpanel.labeltext.totalskillpoints") });
    c =
        new GridBagConstraints(1, 2, 1, 1, 0.1, 0.0, GridBagConstraints.CENTER,
            GridBagConstraints.BOTH, new Insets(6, 6, 6, 6), 2, 2);
    add(totalSkillLevel, c);

    cumulative = new JCheckBox(Resources
        .get("skillchart.skillchartpanel.labeltext.cumulative"));
    cumulative.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setSkillType();
      }
    });
    c.gridx = 0;
    c.gridy = 3;
    add(cumulative, c);
  }

  /**
   * Returns the chart (for tests).
   */
  JFreeChart getChart() {
    return chart;
  }

  protected void setSkillType() {
    SkillType skillType = (SkillType) skills.getSelectedItem();

    chart.getTitle().setText(skillType != null ? skillType.getName() : "");

    setComboBoxes(skillType);
    chart.getCategoryPlot().setDataset(createDataset(skillType));
  }

  /**
   * just to reset the combo boxes
   */
  private void setComboBoxes(SkillType skillType) {
    if (skillType != null) {
      Vector<String> vPersons = new Vector<String>();
      Vector<String> vTotalSkillLevel = new Vector<String>();
      Vector<String> vTotalSkillPoints = new Vector<String>();
      vPersons.add(Resources.get("skillchart.skillchartpanel.labeltext.totalpersons")
          + skillStats.getPersonNumber(skillType));
      vTotalSkillLevel.add(Resources.get("skillchart.skillchartpanel.labeltext.totalskilllevel")
          + skillStats.getSkillLevelNumber(skillType));
      vTotalSkillPoints.add(Resources.get("skillchart.skillchartpanel.labeltext.totalskillpoints")
          + skillStats.getSkillPointsNumber(skillType));

      boolean isCumulative = cumulative.isSelected();
      String preLabel = isCumulative ? "T>=" : "T";
      int cumulativePersons = 0, cumulativeLevel = 0, cumulativePoints = 0;
      for (Skill skill : skillStats.getKnownSkills(skillType)) {
        int pNumber = skillStats.getPersonNumber(skill);
        if (isCumulative) {
          cumulativePersons += pNumber;
          pNumber = cumulativePersons;
        }
        vPersons.add(preLabel + skill.getLevel() + ": " + pNumber);

        int level = skillStats.getSkillLevelNumber(skill);
        if (isCumulative) {
          cumulativeLevel += level;
          level = cumulativeLevel;
        }
        vTotalSkillLevel.add(preLabel + skill.getLevel() + ": " + level);

        int points = skillStats.getSkillPointsNumber(skill);
        if (isCumulative) {
          cumulativePoints += points;
          points = cumulativePoints;
        }
        vTotalSkillPoints.add(preLabel + skill.getLevel() + ": "
            + points);
      }

      persons.setModel(new DefaultComboBoxModel(vPersons));
      totalSkillLevel.setModel(new DefaultComboBoxModel(vTotalSkillLevel));
      totalSkillPoints.setModel(new DefaultComboBoxModel(vTotalSkillPoints));
    } else {
      persons.setModel(new DefaultComboBoxModel(new String[] { Resources
          .get("skillchart.skillchartpanel.labeltext.totalpersons") }));
      totalSkillPoints.setModel(new DefaultComboBoxModel(new String[] { Resources
          .get("skillchart.skillchartpanel.labeltext.totalskilllevel") }));
      totalSkillLevel.setModel(new DefaultComboBoxModel(new String[] { Resources
          .get("skillchart.skillchartpanel.labeltext.totalskillpoints") }));
    }
  }

  /**
   * Creates the data set of the chart out of the SkillStats-Object of this class and the specified
   * skillType: the number of persons for every skill level from one below the lowest to one above
   * the highest known level. If skillType is null, the data set contains the single value (0,0).
   */
  private DefaultCategoryDataset createDataset(SkillType skillType) {
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    if (skillType == null) {
      dataset.addValue(0, SERIES, "");
      return dataset;
    }

    int lowLevel = skillStats.getLowestKnownSkillLevel(skillType);
    int highLevel = skillStats.getHighestKnownSkillLevel(skillType);

    if (highLevel != 0) {
      highLevel++;
    }

    if (lowLevel != 0) {
      lowLevel--;
    }

    // cumulative values are summed up from the highest level downwards
    int[] values = new int[highLevel - lowLevel + 1];
    int cumulativeLevel = 0;
    for (int level = highLevel; level >= lowLevel; level--) {
      int persons = skillStats.getPersonNumber(new Skill(skillType, 1, level, 1, false));
      if (cumulative.isSelected()) {
        cumulativeLevel += persons;
        persons = cumulativeLevel;
      }
      values[level - lowLevel] = persons;
    }

    for (int level = lowLevel; level <= highLevel; level++) {
      dataset.addValue(values[level - lowLevel], SERIES, Integer.valueOf(level));
    }

    return dataset;
  }

  /**
   * Displays skill information only for selected objects (Factions or Regions).
   *
   * @see magellan.client.event.SelectionListener#selectionChanged(magellan.client.event.SelectionEvent)
   */
  public void selectionChanged(SelectionEvent e) {
    // we are only interested in the selectedObjects, not in activeObject
    // and there only in regions and factions
    if (e.getSelectedObjects() != null) {
      boolean modified = false;

      // empty Selection => set all region as data
      if (e.getSelectedObjects().isEmpty()) {
        regions.clear();

        if (getGameData() != null) {
          regions.addAll(getGameData().getRegions());
        }

        modified = true;
      } else {
        boolean rModified = false;
        boolean fModified = false;
        for (Object o : e.getSelectedObjects()) {
          if (o instanceof Region) {
            // some regions were selected:
            Region region = (Region) o;
            if (!rModified) {
              regions.clear();
            }
            rModified = true;

            regions.add(region);
          } else if (o instanceof Faction) {
            // a faction has been selected
            Faction faction = (Faction) o;
            if (!fModified) {
              factions.clear();
            }
            fModified = true;

            factions.put(faction.getID(), faction);
          }
        }
        modified = rModified || fModified;
      }

      if (modified) {
        // update the skillStats-Object to the new data (new factions or new regions)
        skillStats = new SkillStats();

        for (Region r : regions) {
          for (Unit u : r.units()) {
            if (factions.containsValue(u.getFaction())) {
              skillStats.addUnit(u);
            }
          }
        }

        skills.removeAllItems();

        for (SkillType skillType : skillStats.getKnownSkillTypes()) {
          skills.addItem(skillType);
        }

        if (skills.getItemCount() > 0) {
          skills.setSelectedIndex(0);
        }

        chart.getCategoryPlot().setDataset(createDataset((SkillType) skills.getSelectedItem()));
      }
    }
  }

  /**
   * @see magellan.client.swing.InternationalizedDataPanel#gameDataChanged(magellan.library.event.GameDataEvent)
   */
  @Override
  public void gameDataChanged(magellan.library.event.GameDataEvent e) {
    setGameData(e.getGameData());

    if (getGameData().noSkillPoints) {
      totalSkillPoints.setVisible(false);
    } else {
      totalSkillPoints.setVisible(true);
    }

    /**
     * Don't clear factions as the SelectionEvent of the updated List in FactionStatsDialog might be
     * processed befor the GameDataEvent
     */
    // factions.clear();
    // enforce refreshing of regions-table and redrawing of chart
    selectionChanged(SelectionEvent.create(this));
  }

  /**
   * Returns the tooltip for the bar of the given skill level (column key of the data set). The
   * values are not cumulative.
   */
  private String getToolTip(Comparable<?> levelKey) {
    SkillType type = (SkillType) skills.getSelectedItem();

    if ((type == null) || !(levelKey instanceof Integer))
      return null;

    Skill skill = new Skill(type, 1, ((Integer) levelKey).intValue(), 1, false);
    return Resources.get("skillchart.skillchartpanel.labeltext.totalpersons")
        + skillStats.getPersonNumber(skill)
        + ", " + Resources.get("skillchart.skillchartpanel.labeltext.totalskilllevel")
        + skillStats.getSkillLevelNumber(skill)
        + ", " + Resources.get("skillchart.skillchartpanel.labeltext.totalskillpoints")
        + skillStats.getSkillPointsNumber(skill);
  }
}

// class magellan.client.skillchart.SkillChartPanelTest
//
// Copyright 2003-2026 by magellan project team
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program (see doc/LICENCE.txt); if not, write to the
// Free Software Foundation, Inc.,
// 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//
package magellan.client.skillchart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBox;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.data.category.CategoryDataset;
import org.junit.Before;
import org.junit.Test;

import magellan.client.event.SelectionEvent;
import magellan.library.Faction;
import magellan.library.GameData;
import magellan.library.Region;
import magellan.library.Unit;
import magellan.test.GameDataBuilder;
import magellan.test.MagellanTestWithResources;

/**
 * Tests the skill chart: data set, tool tips and rendering (headless).
 */
public class SkillChartPanelTest extends MagellanTestWithResources {

  private SkillChartPanel panel;

  /**
   * Creates a faction with units of skill levels 2 (3 persons), 2 (1 person) and 4 (2 persons) and
   * selects the faction.
   */
  @Before
  public void setUp() throws Exception {
    GameDataBuilder builder = new GameDataBuilder();
    // without the default unit of the builder
    GameData data = builder.createSimpleGameData(350, false);
    Faction faction = data.getFactions().iterator().next();
    Region region = data.getRegions().iterator().next();

    addUnit(builder, data, faction, region, "u1", 3, 2);
    addUnit(builder, data, faction, region, "u2", 1, 2);
    addUnit(builder, data, faction, region, "u3", 2, 4);

    panel = new SkillChartPanel(context.getEventDispatcher(), data, settings);
    panel.selectionChanged(SelectionEvent.create(this, faction, SelectionEvent.ST_DEFAULT));
  }

  private static void addUnit(GameDataBuilder builder, GameData data, Faction faction,
      Region region, String id, int persons, int level) {
    Unit unit = builder.addUnit(data, id, "Unit " + id, faction, region);
    unit.setPersons(persons);
    builder.addSkill(unit, "Hiebwaffen", level);
  }

  private CategoryDataset dataset() {
    return panel.getChart().getCategoryPlot().getDataset();
  }

  private List<Integer> values() {
    CategoryDataset dataset = dataset();
    Integer[] values = new Integer[dataset.getColumnCount()];
    for (int i = 0; i < values.length; i++) {
      values[i] = dataset.getValue(0, i).intValue();
    }
    return Arrays.asList(values);
  }

  @Test
  public void showsPersonsPerLevel() {
    CategoryDataset dataset = dataset();
    // levels 1 to 5: one below the lowest and one above the highest known level
    assertEquals(Arrays.asList(1, 2, 3, 4, 5), dataset.getColumnKeys());
    assertEquals(Arrays.asList(0, 4, 0, 2, 0), values());
    assertTrue(panel.getChart().getTitle().getText().length() > 0);
  }

  @Test
  public void cumulativeSumsFromTheTop() throws Exception {
    JCheckBox cumulative = findCumulativeCheckBox();
    cumulative.doClick();
    assertEquals(Arrays.asList(6, 6, 2, 2, 0), values());
  }

  @Test
  public void toolTipShowsValuesOfTheLevel() {
    BarRenderer renderer = (BarRenderer) panel.getChart().getCategoryPlot().getRenderer();
    String toolTip = renderer.getDefaultToolTipGenerator().generateToolTip(dataset(), 0, 1);
    assertNotNull(toolTip);
    // level 2: 4 persons, 4 * 2 = 8 levels
    assertTrue(toolTip, toolTip.contains("4"));
    assertTrue(toolTip, toolTip.contains("8"));
  }

  @Test
  public void rendersHeadless() {
    JFreeChart chart = panel.getChart();
    BufferedImage image = chart.createBufferedImage(480, 320);
    assertEquals(480, image.getWidth());
  }

  private JCheckBox findCumulativeCheckBox() {
    for (java.awt.Component component : panel.getComponents()) {
      if (component instanceof JCheckBox)
        return (JCheckBox) component;
    }
    throw new AssertionError("no check box");
  }
}

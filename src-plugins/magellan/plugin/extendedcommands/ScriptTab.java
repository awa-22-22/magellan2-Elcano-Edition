// class magellan.plugin.extendedcommands.ScriptTab
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
package magellan.plugin.extendedcommands;

import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Tab header of one script in the {@link ExtendedCommandsDock}: the title and a close button.
 */
public class ScriptTab extends JPanel {
  private final JLabel label;

  /**
   * Creates a tab header.
   *
   * @param title the tab title
   * @param closeTooltip tooltip of the close button
   * @param closeListener called when the close button is pressed
   */
  public ScriptTab(String title, String closeTooltip, ActionListener closeListener) {
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));
    setOpaque(false);

    label = new JLabel(title);
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
    add(label);

    JButton close = new JButton("\u00d7"); // multiplication sign as close symbol
    close.setToolTipText(closeTooltip);
    close.setMargin(new Insets(0, 2, 0, 2));
    close.setBorderPainted(false);
    close.setContentAreaFilled(false);
    close.setFocusable(false);
    close.addActionListener(closeListener);
    add(close);
  }

  /**
   * Returns the tab title.
   */
  public String getText() {
    return label.getText();
  }

  /**
   * Changes the tab title.
   */
  public void setText(String title) {
    label.setText(title);
  }
}

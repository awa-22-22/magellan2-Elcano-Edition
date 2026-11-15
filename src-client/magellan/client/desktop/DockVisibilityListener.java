// class magellan.client.desktop.DockVisibilityListener
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
package magellan.client.desktop;

/**
 * A dock component (see {@link magellan.client.extern.MagellanPlugIn#getDocks()}) that implements
 * this interface is told when its dock becomes visible or hidden. This keeps dock components
 * independent of the docking library.
 */
public interface DockVisibilityListener {

  /**
   * Called when the dock containing this component has become visible.
   */
  void dockShown();

  /**
   * Called when the dock containing this component has been hidden or closed.
   */
  void dockHidden();
}

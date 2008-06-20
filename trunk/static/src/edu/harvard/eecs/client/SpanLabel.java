package edu.harvard.eecs.client;
/*

This file is part of Latiki, a web-based editing and typesetting program
Copyright (C) 2008 Joseph Barillari & Tim Credo

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;

public class SpanLabel extends Label {
    /*   it's like a label, but uses a span instead of a div
     */

	public SpanLabel() {
		setElement(DOM.createSpan());
		sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONMOUSEWHEEL);
		setStyleName("gwt-Label");
	}

  public SpanLabel(String text) {
    this();
    setText(text);
  }

}

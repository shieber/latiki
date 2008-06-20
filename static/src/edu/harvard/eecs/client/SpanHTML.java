
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


/* Modified by Joseph Barillari & Tim Credo for Latiki, Spring 2008 */

/*
 * Copyright 2006 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package edu.harvard.eecs.client;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.Label;

/**
 * A widget that can contain arbitrary HTML.
 * 
 * <p>
 * If you only need a simple label (text, but not HTML), then the
 * {@link com.google.gwt.user.client.ui.Label} widget is more appropriate, as it
 * disallows the use of HTML, which can lead to potential security issues if not
 * used properly.
 * </p>
 * 
 * <h3>CSS Style Rules</h3>
 * <ul class='css'>
 * <li>.gwt-HTML { }</li>
 * </ul>
 * 
 * <p>
 * <h3>Example</h3>
 * {@example com.google.gwt.examples.HTMLExample}
 * </p>
 * extended by jdb to use a span instead of a div
 */
public class SpanHTML extends Label implements HasHTML {

  /**
   * Creates an empty HTML widget.
   */
  public SpanHTML() {
    setElement(DOM.createSpan());
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONMOUSEWHEEL);
    setStyleName("gwt-HTML");
  }

  /**
   * Creates an HTML widget with the specified HTML contents.
   * 
   * @param html the new widget's HTML contents
   */
  public SpanHTML(String html) {
    this();
    setHTML(html);
  }

  /**
   * Creates an HTML widget with the specified contents, optionally treating it
   * as HTML, and optionally disabling word wrapping.
   * 
   * @param html the widget's contents
   * @param wordWrap <code>false</code> to disable word wrapping
   */
  public SpanHTML(String html, boolean wordWrap) {
    this(html);
    setWordWrap(wordWrap);
  }

  public String getHTML() {
    return DOM.getInnerHTML(getElement());
  }

  public void setHTML(String html) {
    DOM.setInnerHTML(getElement(), html);
  }
}

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
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.ui.ClickListener;

public class DiffPanel extends FlowPanel {

	private class AcceptRejectPopup extends PopupPanel {
		UIObject uio_ = null;

		public AcceptRejectPopup(UIObject uio) {
			super(true);
			uio_ = uio;
			HorizontalPanel hp = new HorizontalPanel();
			Button accept = new Button("Accept", new ClickListener() {
				public void onClick(Widget sender) {
					String style = uio_.getStyleName();
					// accept newmaterial: remove the style name
					// accept removedmaterial: mark it hidden
					if ((style.indexOf("newmaterial") > -1)) {
						uio_.removeStyleName("newmaterial");
						uio_.addStyleName("showchange");
					}
					if ((style.indexOf("removedmaterial") > -1)) {
						uio_.addStyleName("hidechange");
					}
					hide();
				}
			});
			Button reject = new Button("Reject", new ClickListener() {
				public void onClick(Widget sender) {
					String style = uio_.getStyleName();
					// reject newmaterial: mark it hidden
					// reject removedmaterial: remove the style
					if ((style.indexOf("newmaterial") > -1)) {
						uio_.addStyleName("hidechange");
					}
					if ((style.indexOf("removedmaterial") > -1)) {
						uio_.removeStyleName("removedmaterial");
						uio_.addStyleName("showchange");
					}
					hide();
				}
			});
			// contents.setWidth("128px");
			accept.setWidth("60px");
			reject.setWidth("60px");
			hp.add(accept);
			hp.add(reject);
			setWidget(hp);
			setStyleName("ks-popups-Popup");
		}
	}

	public static final int INVARIANT = 0;

	public static final int ADDED = 2;

	public static final int DELETED = 1;

	/*
	 * Takes the json representation of the diff, turns it into a diffpanel
	 */
	public DiffPanel(String jsonRep) {
		// the string rep is a list of lists
		// each list is of the form [type, string]
		// where type is one of the constants above

		JSONArray blocks = JSONParser.parse(jsonRep).isArray();
		for (int i = 0; i < blocks.size(); i++) {
			JSONArray pair = blocks.get(i).isArray();
			int type = (int) pair.get(0).isNumber().getValue();
			String text = pair.get(1).isString().stringValue();

			//DOM.setInnerText(span, text);
			String style = "FAIL";
			switch (type) {
			case ADDED:
				style = "newmaterial";
				break;
			case DELETED:
				style = "removedmaterial";
				break;
			case INVARIANT:
				style = "invariant";
			}
			//DOM.setElementProperty(span, "class", style);

			//Label lbl = new Label(text);
			// FIXME! sanitize HTML!
			text = text.replaceAll(">", "&gt;");
			text = text.replaceAll("<", "&lt;");
			text = text.replaceAll("&", "&amp;");
			text = text.replaceAll("\n", "<br/>");
			SpanHTML lbl = new SpanHTML(text);
			lbl.addStyleName(style);
			lbl.addClickListener(new ClickListener() {
				public void onClick(Widget sender) {
					AcceptRejectPopup p = new AcceptRejectPopup(sender);
					int left = sender.getAbsoluteLeft() + 10;
					int top = sender.getAbsoluteTop() + 10;
					p.setPopupPosition(left, top);
					p.show();
				}
			});
			add(lbl);
			//DOM.setEventListener(span, listener);
			//			add(span);
		}
	}

	/* returns true if all changes have been accepted or rejected,
	 * false otherwise.
	 */
	//fixme :unused
	// 	public boolean checkRemainingChoices() {
	// 		for (int i = 0; i < getWidgetCount(); i++) {
	// 			Widget widget = getWidget(i);
	// 			if (!(widget instanceof Label)) {
	// 				continue;
	// 			}
	// 			String style = widget.getStyleName();
	// 			if ((style.indexOf("invariant") > -1)
	// 					|| (style.indexOf("deleted") > -1)) {
	// 				if (!((style.indexOf("accepted") > -1) || (style
	// 						.indexOf("rejected") > -1))) {
	// 					return false;
	// 				}
	// 			}
	// 		}
	// 		return true;
	// 	}
	/*
	 * Get the final result as a string. if acceptAdded is true,
	 * accept all adds that haven't already been accepted; if false,
	 * reject them. same goes for acceptRemoved
	 * boolean acceptRemoved, boolean acceptAdded
	 * actually, never mind. we just accept all unconformed changes.
	 * 
	 */
	public String getOutput() {
		String output = ""; // fixme: this is undoubtedly o(n^2)
		for (int i = 0; i < getWidgetCount(); i++) {
			Widget widget = getWidget(i);
			if (!(widget instanceof SpanHTML)) {
				continue;
			}
			String text = ((SpanHTML) widget).getHTML();
			text = text.replaceAll("<br/>", "\n");
			text = text.replaceAll("<br>", "\n"); // somehow gwt is normalizing these to <br>; argh..
			text = text.replaceAll("&gt;",  ">");
			text = text.replaceAll("&lt;" , "<");
			text = text.replaceAll("&amp;", "&");

			String style = widget.getStyleName();
			if (style.indexOf("invariant") > -1) {
				// keep invariants, accepted changes of any sort, and unrejected new material
				output += text;
			} else if ((style.indexOf("showchange") > -1)) {
				output += text;
			} else if ((style.indexOf("newmaterial") > -1) && (style.indexOf("hidechange") == -1)) {
				output += text;
			}
			
		}
		return output;
	}
}
